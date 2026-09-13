package com.vtiers.misc;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.vtiers.VTiersClient;
import com.vtiers.textures.Icons;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.screen.TitleScreen;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.text.Text;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public class ConfigManager {
    private static Config config;
    private static final Path CONFIG_PATH = FabricLoader.getInstance().getConfigDir().resolve("VTiers.json");
    private static String version;
    private static boolean upgradeAdjustmentDone;
    private static int launchTickCounter;

    static {
        FabricLoader.getInstance().getModContainer("vtiers").ifPresent(tiers -> version = tiers.getMetadata().getVersion().getFriendlyString());
    }

    private static class Config {
        boolean toggleMod;
        boolean toggleIcons;
        boolean toggleTab;
        boolean toggleChat;
        boolean toggleAdaptiveSeparator;
        boolean toggleAutoKitDetect;
        VTiersClient.ModesTierDisplay displayMode;
        Icons.Type activeIcons;

        VTiersClient.DisplayStatus positionPvPTiers;
        Mode activePvPTiersMode;

        String version;
    }

    public static void loadConfig() {
        Gson gson = new Gson();
        File file = CONFIG_PATH.toFile();
        if (file.exists()) {
            try (FileReader fileReader = new FileReader(file)) {
                config = gson.fromJson(fileReader, Config.class);
                if (config == null)
                    restoreFromClient();
            } catch (IOException | JsonSyntaxException ignored) {
                restoreFromClient();
            }
        } else
            restoreFromClient();

        VTiersClient.toggleMod = config.toggleMod;
        VTiersClient.toggleIcons = config.toggleIcons;
        VTiersClient.toggleTab = config.toggleTab;
        VTiersClient.toggleChat = config.toggleChat;
        VTiersClient.toggleAdaptiveSeparator = config.toggleAdaptiveSeparator;
        VTiersClient.toggleAutoKitDetect = config.toggleAutoKitDetect;

        if (Arrays.stream(VTiersClient.ModesTierDisplay.values()).toList().contains(config.displayMode))
            VTiersClient.displayMode = config.displayMode;

        if (Arrays.stream(Icons.Type.values()).toList().contains(config.activeIcons))
            VTiersClient.activeIcons = config.activeIcons;

        if (Arrays.stream(VTiersClient.DisplayStatus.values()).toList().contains(config.positionPvPTiers))
            VTiersClient.positionPvPTiers = config.positionPvPTiers;
        if (Arrays.stream(Mode.values()).toList().contains(config.activePvPTiersMode) && (config.activePvPTiersMode.toString().contains("PVPTIERS") || config.activePvPTiersMode.toString().contains("VNLIST")))
            VTiersClient.activePvPTiersMode = config.activePvPTiersMode;

        if (config.version == null) {
            ClientTickEvents.END_CLIENT_TICK.register(minecraftClient -> {
                if (upgradeAdjustmentDone)
                    return;

                if (minecraftClient.currentScreen instanceof TitleScreen) {
                    launchTickCounter++;

                    if (launchTickCounter >= 20) {
                        minecraftClient.getToastManager().add(SystemToast.create(minecraftClient, SystemToast.Type.NARRATOR_TOGGLE, Text.of("Thanks for updating VTiers"), Text.of("Some settings may have changed")));
                        VTiersClient.toggleMod = true;
                        VTiersClient.toggleIcons = true;
                        VTiersClient.toggleTab = true;
                        VTiersClient.toggleChat = true;
                        VTiersClient.toggleAdaptiveSeparator = true;
                        VTiersClient.toggleAutoKitDetect = false;

                        saveConfig();
                        upgradeAdjustmentDone = true;
                    }
                }
            });
        }

        saveConfig();
    }

    private static void restoreFromClient() {
        config = new Config();
        updateConfig(config);

        VTiersClient.LOGGER.info("Broken config file: VTiers has restored values from the client memory");

        saveConfig();
    }

    public static void saveConfig() {
        Gson gson = new Gson();
        File file = CONFIG_PATH.toFile();
        Config config = new Config();

        updateConfig(config);

        CompletableFuture.runAsync(() -> {
            try (FileWriter fileWriter = new FileWriter(file)) {
                gson.toJson(config, fileWriter);
            } catch (IOException ignored) {
                restoreFromClient();
            } finally {
                VTiersClient.updateAllTags();
            }
        });
    }

    private static void updateConfig(Config config) {
        config.toggleMod = VTiersClient.toggleMod;
        config.toggleIcons = VTiersClient.toggleIcons;
        config.toggleTab = VTiersClient.toggleTab;
        config.toggleChat = VTiersClient.toggleChat;
        config.toggleAdaptiveSeparator = VTiersClient.toggleAdaptiveSeparator;
        config.toggleAutoKitDetect = VTiersClient.toggleAutoKitDetect;
        config.displayMode = VTiersClient.displayMode;
        config.activeIcons = VTiersClient.activeIcons;

        config.positionPvPTiers = VTiersClient.positionPvPTiers;
        config.activePvPTiersMode = VTiersClient.activePvPTiersMode;

        config.version = version;
    }

    public static String getCurrentConfig() {
        return "\nConfig{" +
                "\ntoggleMod=" + config.toggleMod +
                "\ntoggleIcons=" + config.toggleIcons +
                "\ntoggleTab=" + config.toggleTab +
                "\ntoggleChat=" + config.toggleChat +
                "\ntoggleAdaptiveSeparator=" + config.toggleAdaptiveSeparator +
                "\ntoggleAutoKitDetect=" + config.toggleAutoKitDetect +
                "\ndisplayMode=" + config.displayMode +
                "\nactiveIcons=" + config.activeIcons +
                "\npositionPvPTiers=" + config.positionPvPTiers +
                "\nactivePvPTiersMode=" + config.activePvPTiersMode +
                "\nversion=" + config.version +
                "\n}";
    }
}