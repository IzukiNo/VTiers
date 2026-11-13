package com.vtiers;

import com.mojang.blaze3d.platform.GLX;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.brigadier.context.CommandContext;
import com.vtiers.misc.CommandRegister;
import com.vtiers.misc.ConfigManager;
import com.vtiers.misc.Mode;
import com.vtiers.mixin.client.DataTrackerAccessor;
import com.vtiers.mixin.client.TextDisplayAccessor;
import com.vtiers.profile.PlayerProfile;
import com.vtiers.profile.Status;
import com.vtiers.profile.types.SuperProfile;
import com.vtiers.screens.ConfigScreen;
import com.vtiers.screens.PlayerSearchResultScreen;
import com.vtiers.textures.ColorLoader;
import com.vtiers.textures.Icons;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.command.v2.FabricClientCommandSource;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.resource.v1.ResourceLoader;
import net.fabricmc.fabric.api.resource.v1.pack.PackActivationType;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.ClickEvent;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.util.CommonColors;
import net.minecraft.world.entity.Display;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import org.apache.commons.io.FileUtils;
import org.lwjgl.glfw.GLFW;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.http.HttpClient;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CopyOnWriteArrayList;

public class VTiersClient implements ClientModInitializer {
    public static final Logger LOGGER = LoggerFactory.getLogger(VTiersClient.class);
    public static String userAgent = "VTiers (modrinth.com/mod/vtiers)";
    public static final CopyOnWriteArrayList<PlayerProfile> playerProfiles = new CopyOnWriteArrayList<>();
    public static final HashMap<String, PlayerProfile> readyPlayerProfiles = new HashMap<>();
    public static final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(java.time.Duration.ofSeconds(5))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();
    public static int cacheVersion;
    public static volatile boolean cachesDirty = false;

    public static boolean toggleMod = true;
    public static boolean toggleIcons = true;
    public static boolean toggleTab = true;
    public static boolean toggleChat = true;
    public static boolean toggleAdaptiveSeparator = true;
    public static boolean toggleAutoKitDetect = false;
    public static ModesTierDisplay displayMode = ModesTierDisplay.ADAPTIVE_HIGHEST;
    public static Icons.Type activeIcons = Icons.Type.PVPTIERS;

    public static DisplayStatus positionPvPTiers = DisplayStatus.LEFT;
    public static Mode activePvPTiersMode = Mode.VNLIST_VANILLA;

    public static KeyMapping autoDetectKey;
    public static KeyMapping openClosestPlayerProfile;
    public static KeyMapping cycleRightKey;
    public static KeyMapping cycleLeftKey;

    @Override
    public void onInitializeClient() {
        ConfigManager.loadConfig();
        changeIcons(activeIcons, false);
        clearCache(true);
        CommandRegister.registerCommands();

        Optional<ModContainer> modContainer = FabricLoader.getInstance().getModContainer("vtiers");

        modContainer.ifPresent(tiers -> {
            ResourceLoader.registerBuiltinPack(Identifier.fromNamespaceAndPath("vtiers", "vtiers-resources"), tiers, Component.nullToEmpty("Resources for VTiers"), PackActivationType.ALWAYS_ENABLED);
            userAgent += " v" + tiers.getMetadata().getVersion().getFriendlyString();
        });

        KeyMapping.Category category = KeyMapping.Category.register(Identifier.fromNamespaceAndPath("vtiers", "category"));
        autoDetectKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.vtiers.auto_detect_kit", GLFW.GLFW_KEY_Y, category));
        openClosestPlayerProfile = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.vtiers.open_closest_player_profile", GLFW.GLFW_KEY_H, category));
        cycleRightKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.vtiers.cycle_right_gamemodes", GLFW.GLFW_KEY_I, category));
        cycleLeftKey = KeyMappingHelper.registerKeyMapping(new KeyMapping("key.vtiers.cycle_left_gamemodes", GLFW.GLFW_KEY_U, category));

        ResourceLoader.get(PackType.CLIENT_RESOURCES).registerReloadListener(Identifier.fromNamespaceAndPath("vtiers", "color_loader"), new ColorLoader());
        ClientTickEvents.END_CLIENT_TICK.register(VTiersClient::checkKeys);
        ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
            if (toggleAutoKitDetect)
                InventoryChecker.checkInventory(minecraftClient, false);
            if (cachesDirty) {
                cachesDirty = false;
                updateCaches();
            }
        });

        LOGGER.info("VTiers initialized | User agent: {}", userAgent);
    }

    public static PlayerProfile addGetPlayer(String playerName, boolean priority) {
        if (playerName == null)
            return null;

        PlayerProfile gotFromReady = readyPlayerProfiles.get(playerName);
        if (gotFromReady == null)
            gotFromReady = readyPlayerProfiles.get(playerName.toLowerCase(Locale.ROOT));

        if (gotFromReady != null)
            return gotFromReady;

        for (PlayerProfile playerProfile : playerProfiles) {
            if (playerProfile.targetName.equalsIgnoreCase(playerName) || (playerProfile.inGameName != null && playerProfile.inGameName.equalsIgnoreCase(playerName))) {
                if (priority)
                    PlayerProfileQueue.changeToFirstInQueue(playerProfile);
                return playerProfile;
            }
        }
        PlayerProfile newProfile = new PlayerProfile(playerName, true);

        if (priority)
            PlayerProfileQueue.putFirstInQueue(newProfile);
        else
            PlayerProfileQueue.enqueue(newProfile);

        playerProfiles.add(newProfile);
        return newProfile;
    }

    public static void updateAllTags() {
        Minecraft.getInstance().execute(() -> {
            readyPlayerProfiles.values().forEach(PlayerProfile::updateAppendingText);

            if (ConfigScreen.ownProfile != null && ConfigScreen.defaultProfile != null) {
                ConfigScreen.ownProfile.updateAppendingText();
                ConfigScreen.defaultProfile.updateAppendingText();
            }
            updateCaches();
        });
    }

    public static void restyleAllTexts(List<PlayerProfile> playerProfiles) {
        Minecraft.getInstance().execute(() -> {
            playerProfiles.forEach(playerProfile -> {
                if (playerProfile.status == Status.READY) {
                    if (playerProfile.profileVNList != null && playerProfile.profileVNList.status == Status.READY)
                        playerProfile.profileVNList.parseJson(playerProfile.profileVNList.originalJson);
                    if (playerProfile.profilePvPTiers != null && playerProfile.profilePvPTiers.status == Status.READY)
                        playerProfile.profilePvPTiers.parseJson(playerProfile.profilePvPTiers.originalJson);
                }
                playerProfile.updateAppendingText();
            });
            updateCaches();
        });
    }

    public static String getNearestPlayerName() {
        Minecraft minecraftClient = Minecraft.getInstance();
        Player self = minecraftClient.player;
        if (self == null || self.level() == null)
            return null;

        Player playerEntity = self.level().players().stream()
                .filter(player -> player != self)
                .filter(player -> self.distanceTo(player) < Minecraft.getInstance().options.getEffectiveRenderDistance() * 16)
                .min(Comparator.comparingDouble(self::distanceTo))
                .orElse(null);

        if (playerEntity != null)
            return playerEntity.getScoreboardName();
        return null;
    }

    private static void checkKeys(Minecraft minecraftClient) {
        if (autoDetectKey.consumeClick())
            InventoryChecker.checkInventory(minecraftClient, true);

        if (openClosestPlayerProfile.consumeClick()) {
            String nearestPlayerName = getNearestPlayerName();
            if (nearestPlayerName != null)
                tiersCommand(nearestPlayerName);
            else
                sendMessageToPlayer(Icons.colorText("No players in render distance", "red"), true);
        }

        if (cycleRightKey.consumeClick()) {
            Component message = cycleRightMode();

            sendMessageToPlayer(message != null ? message : Icons.colorText("There's nothing on the right display", "red"), true);
        }

        if (cycleLeftKey.consumeClick()) {
            Component message = cycleLeftMode();

            sendMessageToPlayer(message != null ? message : Icons.colorText("There's nothing on the left display", "red"), true);
        }
    }

    public static Component cycleRightMode() {
        if (toggleAutoKitDetect) {
            toggleAutoKitDetect = false;
            sendMessageToPlayer(Icons.colorText("Auto kit detect has been disabled due to manual gamemode changes", "red"), false);
        }

        if (positionPvPTiers.toString().equalsIgnoreCase("RIGHT"))
            return Component.literal("Right (PvPTiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cyclePvPTiersMode());

        return null;
    }

    public static Component cycleLeftMode() {
        if (toggleAutoKitDetect) {
            toggleAutoKitDetect = false;
            sendMessageToPlayer(Icons.colorText("Auto kit detect has been disabled due to manual gamemode changes", "red"), false);
        }

        if (positionPvPTiers.toString().equalsIgnoreCase("LEFT"))
            return Component.literal("Left (PvPTiers) is now displaying ").setStyle(Style.EMPTY.withColor(CommonColors.WHITE)).append(cyclePvPTiersMode());

        return null;
    }

    public static Component getRightIcon() {
        if (positionPvPTiers.toString().equalsIgnoreCase("RIGHT"))
            return activePvPTiersMode.getIcon();

        return Component.empty();
    }

    public static Component getLeftIcon() {
        if (positionPvPTiers.toString().equalsIgnoreCase("LEFT"))
            return activePvPTiersMode.getIcon();

        return Component.empty();
    }

    public static void sendMessageToPlayer(Component message, boolean overlay) {
        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.player != null)
            minecraftClient.execute(() -> {
                if (overlay)
                    minecraftClient.player.sendOverlayMessage(message);
                else
                    minecraftClient.player.sendSystemMessage(message);
            });
    }

    public static void toggleMod(CommandContext<FabricClientCommandSource> ignoredFabricClientCommandSourceCommandContext) {
        toggleMod = !toggleMod;
        ConfigManager.saveConfig();
        sendMessageToPlayer(Icons.colorText("VTiers đang " + (toggleMod ? "bật" : "tắt"), toggleMod ? "green" : "red"), true);
    }

    public static void toggleMod() {
        toggleMod = !toggleMod;
        ConfigManager.saveConfig();
    }

    public static void toggleIcons() {
        toggleIcons = !toggleIcons;
        ConfigManager.saveConfig();
    }

    public static void toggleTab() {
        toggleTab = !toggleTab;
        ConfigManager.saveConfig();
    }

    public static void toggleChat() {
        toggleChat = !toggleChat;
        ConfigManager.saveConfig();
    }

    public static void toggleAdaptiveSeparator() {
        toggleAdaptiveSeparator = !toggleAdaptiveSeparator;
        ConfigManager.saveConfig();
    }

    public static void toggleAutoKitDetect() {
        toggleAutoKitDetect = !toggleAutoKitDetect;
        ConfigManager.saveConfig();
    }

    public static void tiersCommand(String playerName) {
        if (playerName.equalsIgnoreCase("-toggle"))
            toggleMod(null);
        else if (playerName.equalsIgnoreCase("-config"))
            setScreen(ConfigScreen.getConfigScreen(null));
        else if (playerName.equalsIgnoreCase("-help") || playerName.equalsIgnoreCase("-debug")) {
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("--- VTiers help ---", CommonColors.YELLOW), false);
            sendMessageToPlayer(Component.literal("- General contact: ").append(Component.literal("izukinoo on Discord").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://discordapp.com/users/854356889320423424"))))), false);
            sendMessageToPlayer(Component.literal("- Report a bug: ").append(Component.literal("VTiers GitHub issues").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/IzukiNo/VTiers/issues"))))), false);
            sendMessageToPlayer(Component.literal("- It's not advisable to create tickets in VNList support"), false);
            sendMessageToPlayer(Component.literal("- ").append(Component.literal("Changelogs").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://github.com/IzukiNo/VTiers/wiki/Version-changelogs"))))), false);
            sendMessageToPlayer(Component.literal("- ").append(Component.literal("Modrinth page").withStyle(style -> style.withUnderlined(true).withClickEvent(new ClickEvent.OpenUrl(URI.create("https://modrinth.com/mod/vtiers"))))), false);

            String[] debugInfo = getDebugInfo();
            sendMessageToPlayer(Icons.colorText("\n" + debugInfo[0], CommonColors.SOFT_YELLOW), false);
            Minecraft.getInstance().keyboardHandler.setClipboard(debugInfo[1]);

            try (PrintWriter printWriter = new PrintWriter(FabricLoader.getInstance().getGameDir() + "/cache/vtiers/debug.log")) {
                printWriter.println("--- VTiers Debug log ---");
                printWriter.println("--- Section 1 ---");
                printWriter.println(debugInfo[0]);
                printWriter.println("--- End of section 1 ---");
                printWriter.println("--- Section 2 ---");
                printWriter.println(debugInfo[1]);
                printWriter.println("--- End of section 2 ---");
                printWriter.println("--- Section 3 ---");
                printWriter.println(debugInfo[2]);
                printWriter.println("--- End of section 3 ---");
                printWriter.println("--- Section 4 ---");
                printWriter.println(debugInfo[3]);
                printWriter.println("--- End ---");
            } catch (IOException ignored) {
                LOGGER.warn("An error occurred while trying to write the debug log");
            }

            sendMessageToPlayer(Icons.colorText("A complete debug log has been copied to the clipboard and saved in your cache folder", "green"), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
        } else if (playerName.equalsIgnoreCase("-clear")) {
            clearCache(false);
            sendMessageToPlayer(Icons.colorText("Cleared player cache", "green"), true);
        } else if (playerName.equalsIgnoreCase("-status")) {
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("Player profiles status:", "green"), false);
            sendMessageToPlayer(Icons.colorText("Cached players: " + PlayerProfile.playerProfilesRequests.get() + " (" + PlayerProfile.failedPlayerProfilesRequests.get() + " failed)", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("VNList requests failed: " + SuperProfile.failedVNListRequests + "/" + SuperProfile.VNListRequests + " (failed / requested)", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("VNList status | is down? " + SuperProfile.isVNListDown + " | Failed request in last minute: " + SuperProfile.failedVNListRequestsLastMinute, CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("Tiers will try to recover all failed requests once the services come back up", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
        } else if (playerName.startsWith("-")) {
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
            sendMessageToPlayer(Icons.colorText("Not a valid command. Here's a list of valid commands:", "red"), false);
            sendMessageToPlayer(Icons.colorText("/vtiers -toggle", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/vtiers -config", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/vtiers -help | /vtiers -debug", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/vtiers -clear", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("/vtiers -status", CommonColors.YELLOW), false);
            sendMessageToPlayer(Icons.colorText("", CommonColors.WHITE), false);
        } else {
            PlayerProfile playerProfile = addGetPlayer(playerName, true);
            if (playerProfile.isPlayerValid())
                setScreen(new PlayerSearchResultScreen(playerProfile));
        }
    }

    public static void setScreen(Screen screen) {
        Minecraft.getInstance().scheduleWithResult(ignored -> Minecraft.getInstance().setScreen(screen));
    }

    public static String[] getDebugInfo() {
        String[] debugInfo = new String[4];

        StringBuilder fullInfo = new StringBuilder();
        playerProfiles.forEach(fullInfo::append);

        debugInfo[2] = fullInfo.toString();

        debugInfo[3] = ConfigManager.getCurrentConfig();

        final String[] version = new String[1];
        FabricLoader.getInstance().getModContainer("vtiers").ifPresent(tiers -> version[0] = "VTiers version: " + tiers.getMetadata().getVersion().getFriendlyString());
        debugInfo[0] = version[0] + "\n";
        debugInfo[1] = debugInfo[0];
        debugInfo[1] += "Launcher brand: " + Minecraft.getLauncherBrand() + "\n";
        debugInfo[1] += "Game version: " + Minecraft.getInstance().getLaunchedVersion() + " | " + FabricLoader.getInstance().getRawGameVersion() + "\n";
        debugInfo[1] += "Version type: " + Minecraft.getInstance().getVersionType() + "\n";
        debugInfo[1] += "Instance name: " + Minecraft.getInstance().name() + "\n";
        debugInfo[1] += "Game profile name: " + Minecraft.getInstance().getGameProfile().name() + "\n";
        debugInfo[1] += "OS info:\n\t" + System.getProperty("os.name") + "\n\t" + System.getProperty("os.version") + "\n\t" + System.getProperty("os.arch") + "\n";
        debugInfo[1] += "CPU info: " + GLX._getCpuInfo() + "\n";
        Runtime runtime = Runtime.getRuntime();
        debugInfo[1] += "RAM info (MB):\n\tMax: " + runtime.maxMemory() / (1024 * 1024) + "\n\tTotal: " + runtime.totalMemory() / (1024 * 1024) + "\n\tFree: " + runtime.freeMemory() / (1024 * 1024) + "\n\tIn use: " + (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024) + "\n";
        GpuDevice gpuDevice = RenderSystem.getDevice();
        debugInfo[1] += "GPU info:\n\t" + gpuDevice.getBackendName() + "\n\t" + gpuDevice.getImplementationInformation() + "\n\t" + gpuDevice.getRenderer() + "\n\t" + gpuDevice.getVersion() + "\n";
        debugInfo[1] += "Java version: " + System.getProperty("java.version") + "\n";
        debugInfo[1] += "Launch args: " + Arrays.toString(FabricLoader.getInstance().getLaunchArguments(true)) + "\n";
        debugInfo[1] += "All Fabric mods: " + FabricLoader.getInstance().getAllMods() + "\n";
        debugInfo[1] += "Resource packs: " + PackRepository.displayPackList(Minecraft.getInstance().getResourcePackRepository().getSelectedPacks()) + "\n";

        return debugInfo;
    }

    public static void changeIcons(Icons.Type iconType, boolean reload) {
        Icons.identifierMCTiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierPvPTiers = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierVNList = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT));
        Icons.identifierMCTiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        Icons.identifierPvPTiersTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        Icons.identifierVNListTags = Identifier.fromNamespaceAndPath("minecraft", "gamemodes/" + iconType.name().toLowerCase(Locale.ROOT) + "-tags");
        ColorLoader.identifier = Identifier.fromNamespaceAndPath("minecraft", "colors/" + iconType.name().toLowerCase(Locale.ROOT) + ".json");

        if (reload)
            Minecraft.getInstance().reloadResourcePacks();

        activeIcons = iconType;
        ConfigManager.saveConfig();
    }

    public static void showUpdatedPlayerProfile(PlayerProfile playerProfile, boolean removeOld) {
        if (removeOld) {
            playerProfiles.remove(playerProfile);
            PlayerProfileQueue.removeFromQueue(playerProfile);
        }

        tiersCommand(playerProfile.targetName + (removeOld ? "-force" : ""));
    }

    public static void clearCache(boolean start) {
        playerProfiles.clear();
        readyPlayerProfiles.clear();
        PlayerProfileQueue.clearQueue();
        PlayerProfile.failedPlayerProfiles.clear();
        SuperProfile.failedSuperProfiles.clear();
        PlayerProfile.resetRequestCounters();
        SuperProfile.resetRequestCounters();

        CompletableFuture.runAsync(() -> {
            try {
                FileUtils.deleteDirectory(new File(FabricLoader.getInstance().getGameDir() + (start ? "/cache/vtiers" : "/cache/vtiers/players")));
            } catch (IOException ignored) {
                LOGGER.warn("Error deleting cache folder");
            }
        });

        Minecraft minecraftClient = Minecraft.getInstance();
        minecraftClient.execute(() -> {
            if (toggleMod && minecraftClient.level != null)
                minecraftClient.level.players().forEach(playerEntity -> addGetPlayer(playerEntity.getScoreboardName(), false));
        });
    }

    public static void updateCaches() {
        cacheVersion++;

        Minecraft minecraftClient = Minecraft.getInstance();
        if (minecraftClient.level == null)
            return;

        for (Entity entity : minecraftClient.level.entitiesForRendering())
            if (entity instanceof Display.TextDisplay textDisplay)
                ((DataTrackerAccessor) textDisplay.getEntityData()).invokeSet(TextDisplayAccessor.getTEXT(), textDisplay.getText(), true);
    }

    public static Component cyclePvPTiersMode() {
        activePvPTiersMode = cycleEnum(activePvPTiersMode, Mode.getVNListValues());
        ConfigManager.saveConfig();
        return activePvPTiersMode.getTextLabel();
    }

    public static void cycleDisplayMode() {
        displayMode = cycleEnum(displayMode, ModesTierDisplay.values());
        ConfigManager.saveConfig();
    }

    private static <T extends Enum<T>> T cycleEnum(T current, T[] values) {
        return values[(current.ordinal() + 1) % values.length];
    }

    public enum ModesTierDisplay {
        HIGHEST,
        SELECTED,
        ADAPTIVE_HIGHEST;

        public String getCurrentMode() {
            if (toString().equalsIgnoreCase("HIGHEST"))
                return "Displayed Tiers: Highest";
            else if (toString().equalsIgnoreCase("SELECTED"))
                return "Displayed Tiers: Selected";
            return "Displayed Tiers: Adaptive Highest";
        }
    }

    public enum DisplayStatus {
        RIGHT,
        LEFT,
        OFF
    }
}