package com.vtiers.screens;

import com.vtiers.PlayerProfileQueue;
import com.vtiers.VTiersClient;
import com.vtiers.misc.ConfigManager;
import com.vtiers.profile.PlayerProfile;
import com.vtiers.profile.Status;
import com.vtiers.profile.types.VNListProfile;
import com.vtiers.textures.ColorControl;
import com.vtiers.textures.Icons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.stream.Stream;

import static com.vtiers.VTiersClient.LOGGER;

import com.mojang.blaze3d.platform.NativeImage;

public class ConfigScreen extends Screen {
    public static PlayerProfile ownProfile;
    public static PlayerProfile defaultProfile;

    private boolean useOwnProfile;
    private String autoDetectKitBoundKey;
    private String cycleRightBoundKey;
    private String cycleLeftBoundKey;
    private final Identifier playerAvatarTexture = Identifier.parse("");
    private boolean imageReady;

    private Button toggleMod;
    private Button toggleIcons;
    private Button toggleTab;
    private Button toggleChat;
    private Button toggleSeparatorMode;
    private Button cycleDisplayMode;
    private Button clearPlayerCache;
    private Button autoKitDetect;
    private Button leftPvPTiers;
//    private ButtonWidget centerPvPTiers;
    private Button rightPvPTiers;
    private Button activeRightMode;
    private Button activeLeftMode;
    private Button enableOwnProfile;

    private int centerX;
    private int distance;

    private ConfigScreen() {
        super(Component.literal("Tiers config"));

        autoDetectKitBoundKey = String.valueOf(VTiersClient.autoDetectKey.getTranslatedKeyMessage()).replace("literal{", "\"").replace("}", "\"");
        if (autoDetectKitBoundKey.length() != 3)
            autoDetectKitBoundKey = "the assigned keybind";

        cycleRightBoundKey = String.valueOf(VTiersClient.cycleRightKey.getTranslatedKeyMessage()).replace("literal{", "\"").replace("}", "\"");
        if (cycleRightBoundKey.length() != 3)
            cycleRightBoundKey = "the assigned keybind";

        cycleLeftBoundKey = String.valueOf(VTiersClient.cycleLeftKey.getTranslatedKeyMessage()).replace("literal{", "\"").replace("}", "\"");
        if (cycleLeftBoundKey.length() != 3)
            cycleLeftBoundKey = "the assigned keybind";

        loadPlayerAvatar();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        centerX = width / 2;
        distance = height / 14;

        super.extractRenderState(context, mouseX, mouseY, delta);

        context.centeredText(font, Component.nullToEmpty("Tiers config"), centerX, height / 50, CommonColors.WHITE);

        drawIconShowcase(context);

        if (!useOwnProfile)
            context.blit(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, centerX - height / 10 / 2, height - (int) (height / 4.166) - height / 54, 0, 0, height / 10, (int) (height / 4.166), height / 10, (int) (height / 4.166));
        else
            drawPlayerAvatar(context, centerX, height - (int) (height / 4.166) - height / 54);

        context.centeredText(font, useOwnProfile ? ownProfile.getFullName() : defaultProfile.getFullName(), centerX, height - (int) (height / 4.166) - height / 54 - 12, CommonColors.WHITE);

        context.blit(RenderPipelines.GUI_TEXTURED, VNListProfile.VNLIST_IMAGE, centerX - 12, distance + 110 + 4, 0, 0, 24, 24, 24, 24);

        context.text(font, VTiersClient.getRightIcon(), centerX + 90 + 32, distance + 75 + 9, CommonColors.WHITE);
        context.text(font, VTiersClient.getLeftIcon(), centerX - 90 - 32 - 12, distance + 75 + 9, CommonColors.WHITE);

        checkUpdates();
    }

    private void drawIconShowcase(GuiGraphicsExtractor context) {
        for (int i = 0; i < 8; i++) {
            context.centeredText(font, Component.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "gamemodes/classic-medium")))), 34 + 14 * i, 13, CommonColors.WHITE);
            context.centeredText(font, Component.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "gamemodes/pvptiers-medium")))), 34 + 14 * i, 38, CommonColors.WHITE);
            context.centeredText(font, Component.literal(String.valueOf((char) (0xF000 + i))).setStyle(Style.EMPTY.withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "gamemodes/mctiers-medium")))), 34 + 14 * i, 63, CommonColors.WHITE);
        }
    }

    private void checkUpdates() {
        toggleMod.setPosition(width / 2 - 88 - 2, distance);
        toggleIcons.setPosition(width / 2 + 2, distance);
        toggleTab.setPosition(width / 2 + 2 + 28 + 2, distance);
        toggleChat.setPosition(width / 2 + 2 + 28 + 2 + 28 + 2, distance);
        toggleSeparatorMode.setPosition(width / 2 - 90, distance + 25);
        cycleDisplayMode.setPosition(width / 2 - 90, distance + 50);
        autoKitDetect.setPosition(width / 2 - 90, distance + 75);
        clearPlayerCache.setPosition(width - 88 - 5, height - 20 - 5);
        leftPvPTiers.setPosition(centerX - 10 - 12, distance + 145);
//        centerPvPTiers.setPosition(centerX - 10, distance + 145);
        rightPvPTiers.setPosition(centerX - 10 + 12, distance + 145);
        activeRightMode.setPosition(centerX + 90 + 4, distance + 75);
        activeLeftMode.setPosition(centerX - 90 - 20 - 4, distance + 75);
        enableOwnProfile.setPosition(width - 20 - 5 - 88 - 4, height - 20 - 5);

        updateVisibilities();
    }

    @Override
    protected void init() {
        centerX = width / 2;
        distance = height / 14;

        toggleMod = Button.builder(Component.nullToEmpty(VTiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"), (buttonWidget) -> {
            VTiersClient.toggleMod();
            toggleTab.active = VTiersClient.toggleMod;
            toggleChat.active = VTiersClient.toggleMod;
            buttonWidget.setMessage(Component.nullToEmpty(VTiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"));
            buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty((VTiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers"))));
        }).bounds(width / 2 - 88 - 2, distance, 88, 20).tooltip(Tooltip.create(Component.nullToEmpty((VTiersClient.toggleMod ? "Disable Tiers" : "Enable Tiers")))).build();

        toggleIcons = Button.builder(VTiersClient.toggleIcons ? Icons.ICONS : Icons.ICONS_DISABLED, (buttonWidget) -> {
            VTiersClient.toggleIcons();
            buttonWidget.setMessage(VTiersClient.toggleIcons ? Icons.ICONS : Icons.ICONS_DISABLED);
            buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleIcons ? "Disable the gamemode icon next to the tier" : "Enable the gamemode icon next to the tier")));
        }).bounds(width / 2 + 2, distance, 28, 20).tooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleIcons ? "Disable the gamemode icon next to the tier" : "Enable the gamemode icon next to the tier"))).build();

        toggleTab = Button.builder(VTiersClient.toggleTab ? Icons.TAB : Icons.TAB_DISABLED, (buttonWidget) -> {
            VTiersClient.toggleTab();
            buttonWidget.setMessage(VTiersClient.toggleTab ? Icons.TAB : Icons.TAB_DISABLED);
            buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleTab ? "Disable Tiers on the tablist" : "Enable Tiers on the tablist")));
        }).bounds(width / 2 + 2 + 28 + 2, distance, 28, 20).tooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleTab ? "Disable Tiers on the tablist" : "Enable Tiers on the tablist"))).build();

        toggleChat = Button.builder(VTiersClient.toggleChat ? Icons.CHAT : Icons.CHAT_DISABLED, (buttonWidget) -> {
            VTiersClient.toggleChat();
            buttonWidget.setMessage(VTiersClient.toggleChat ? Icons.CHAT : Icons.CHAT_DISABLED);
            buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleChat ? "Disable Tiers in chat" : "Enable Tiers in chat")));
        }).bounds(width / 2 + 2 + 28 + 2 + 28 + 2, distance, 28, 20).tooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleChat ? "Disable Tiers in chat" : "Enable Tiers in chat"))).build();

        toggleTab.active = VTiersClient.toggleMod;
        toggleChat.active = VTiersClient.toggleMod;

        toggleSeparatorMode = Button.builder(Component.nullToEmpty(VTiersClient.toggleAdaptiveSeparator ? "Disable Dynamic Separator" : "Enable Dynamic Separator"), (buttonWidget) -> {
            VTiersClient.toggleAdaptiveSeparator();
            buttonWidget.setMessage(Component.nullToEmpty(VTiersClient.toggleAdaptiveSeparator ? "Disable Dynamic Separator" : "Enable Dynamic Separator"));
            buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleAdaptiveSeparator ? "Make the Tiers separator gray" : "Make the Tiers separator match the tier color")));
        }).bounds(width / 2 - 90, distance + 25, 180, 20).tooltip(Tooltip.create(Component.nullToEmpty(VTiersClient.toggleAdaptiveSeparator ? "Make the Tiers separator gray" : "Make the Tiers separator match the tier color"))).build();

        cycleDisplayMode = Button.builder(Component.nullToEmpty(VTiersClient.displayMode.getCurrentMode()), (buttonWidget) -> {
            VTiersClient.cycleDisplayMode();
            buttonWidget.setMessage(Component.nullToEmpty(VTiersClient.displayMode.getCurrentMode()));
        }).bounds(width / 2 - 90, distance + 50, 180, 20).tooltip(Tooltip.create(Component.nullToEmpty(("""
                Selected: only the selected tier will be displayed
                
                Highest: only the highest tier will be displayed
                
                Adaptive Highest: the highest tier will be displayed if selected does not exist""")))).build();

        autoKitDetect = Button.builder(Component.nullToEmpty(VTiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"), (buttonWidget) -> {
            VTiersClient.toggleAutoKitDetect();
            buttonWidget.setMessage(Component.nullToEmpty(VTiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty((VTiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).bounds(width / 2 - 90, distance + 75, 180, 20).tooltip(Tooltip.create(Component.nullToEmpty((VTiersClient.toggleAutoKitDetect ?
                "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")")))).build();

        if (ownProfile.status == Status.READY) {
            enableOwnProfile = Button.builder(Icons.CYCLE, (buttonWidget) -> {
                useOwnProfile = !useOwnProfile;

                imageReady = false;
                loadPlayerAvatar();

                buttonWidget.setTooltip(Tooltip.create(Component.nullToEmpty(useOwnProfile ? "Preview the default profile (" + defaultProfile.name + ")" : "Preview your player profile (" + ownProfile.name + ")")));
            }).bounds(width - 20 - 5 - 88 - 4, height - 20 - 5, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty(useOwnProfile ? "Preview the default profile (" + defaultProfile.name + ")" : "Preview your player profile (" + ownProfile.name + ")"))).build();
        } else {
            enableOwnProfile = Button.builder(Component.nullToEmpty("⚠"), (buttonWidget) -> {
                ownProfile = new PlayerProfile(Minecraft.getInstance().getGameProfile().name(), false);
                PlayerProfileQueue.putFirstInQueue(ownProfile);

                onClose();
            }).bounds(width - 20 - 5 - 88 - 4, height - 20 - 5, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Can't switch profiles: " + ownProfile.name + " is not found or fetched yet. Click to close screen and retry"))).build();
        }

        clearPlayerCache = Button.builder(Component.nullToEmpty("Clear cache"), (buttonWidget) -> VTiersClient.clearCache(false)).bounds(width - 88 - 5, height - 20 - 5, 88, 20).tooltip(Tooltip.create(Component.nullToEmpty("Clear all player cache"))).build();

        leftPvPTiers = Button.builder(Component.nullToEmpty("←"), (buttonWidget) -> {
            VTiersClient.positionPvPTiers = VTiersClient.DisplayStatus.LEFT;
//            updateLeftSwitcher(buttonWidget, centerPvPTiers, rightPvPTiers);
            updateLeftSwitcher(buttonWidget, rightPvPTiers);
        }).bounds(centerX - 10 - 12, distance + 145, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Display PvPTiers on the left"))).build();

//        centerPvPTiers = ButtonWidget.builder(Text.of("●"), (buttonWidget) -> {
//            VTiersClient.positionPvPTiers = VTiersClient.DisplayStatus.OFF;
//            leftPvPTiers.active = true;
//            buttonWidget.active = false;
//            rightPvPTiers.active = true;
//            ConfigManager.saveConfig();
//        }).dimensions(centerX - 10, distance + 145, 20, 20).tooltip(Tooltip.of(Text.of("Disable PvPTiers"))).build();

        rightPvPTiers = Button.builder(Component.nullToEmpty("→"), (buttonWidget) -> {
            VTiersClient.positionPvPTiers = VTiersClient.DisplayStatus.RIGHT;
//            updateRightSwitcher(buttonWidget, leftPvPTiers, centerPvPTiers);
            updateRightSwitcher(buttonWidget, leftPvPTiers);
        }).bounds(centerX - 10 + 12, distance + 145, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Display PvPTiers on the right"))).build();

        switch (VTiersClient.positionPvPTiers) {
            case RIGHT -> rightPvPTiers.active = false;
//            case OFF -> centerPvPTiers.active = false;
            case LEFT -> leftPvPTiers.active = false;
        }

        activeRightMode = Button.builder(Icons.CYCLE, (buttonWidget) -> {
            VTiersClient.cycleRightMode();
            autoKitDetect.setMessage(Component.nullToEmpty(VTiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            autoKitDetect.setTooltip(Tooltip.create(Component.nullToEmpty((VTiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).bounds(centerX + 90 + 4, distance + 75, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Cycle active right gamemode (press " + cycleRightBoundKey + " in game)"))).build();

        activeLeftMode = Button.builder(Icons.CYCLE, (buttonWidget) -> {
            VTiersClient.cycleLeftMode();
            autoKitDetect.setMessage(Component.nullToEmpty(VTiersClient.toggleAutoKitDetect ? "Disable auto kit detect" : "Enable auto kit detect"));
            autoKitDetect.setTooltip(Tooltip.create(Component.nullToEmpty((VTiersClient.toggleAutoKitDetect ?
                    "Disable auto kit detect: you will need to press " + autoDetectKitBoundKey + " to auto-detect the current gamemode" :
                    "Enable auto kit detect: Tiers will always scan your inventory to display the right gamemode (instead of pressing " + autoDetectKitBoundKey + ")"))));
        }).bounds(centerX - 90 - 20 - 4, distance + 75, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Cycle active left gamemode (press " + cycleLeftBoundKey + " in game)"))).build();

        Button useClassicIcons = Button.builder(VTiersClient.activeIcons == Icons.Type.CLASSIC ? Component.nullToEmpty("●") : Component.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(VTiersClient.activeIcons == Icons.Type.CLASSIC ? Component.nullToEmpty("●") : Component.empty());
            VTiersClient.changeIcons(Icons.Type.CLASSIC, true);
        }).bounds(5, 5, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Use classic styled icons and colors"))).build();

        Button usePvPTiersIcons = Button.builder(VTiersClient.activeIcons == Icons.Type.PVPTIERS ? Component.nullToEmpty("●") : Component.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(VTiersClient.activeIcons == Icons.Type.PVPTIERS ? Component.nullToEmpty("●") : Component.empty());
            VTiersClient.changeIcons(Icons.Type.PVPTIERS, true);
        }).bounds(5, 30, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Use PvPTiers styled icons and colors"))).build();

        Button useMCTiersIcons = Button.builder(VTiersClient.activeIcons == Icons.Type.MCTIERS ? Component.nullToEmpty("●") : Component.empty(), (buttonWidget) -> {
            buttonWidget.setMessage(VTiersClient.activeIcons == Icons.Type.MCTIERS ? Component.nullToEmpty("●") : Component.empty());
            VTiersClient.changeIcons(Icons.Type.MCTIERS, true);
        }).bounds(5, 55, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Use MCTiers styled icons and colors"))).build();

        switch (VTiersClient.activeIcons) {
            case CLASSIC -> useClassicIcons.active = false;
            case PVPTIERS -> usePvPTiersIcons.active = false;
            case MCTIERS -> useMCTiersIcons.active = false;
        }

        updateVisibilities();

//        Stream.of(toggleMod, toggleIcons, toggleTab, toggleChat, toggleSeparatorMode, cycleDisplayMode, autoKitDetect, clearPlayerCache, leftPvPTiers, centerPvPTiers, rightPvPTiers, activeRightMode, activeLeftMode, enableOwnProfile, useClassicIcons, usePvPTiersIcons, useMCTiersIcons)
//                .forEach(this::addDrawableChild);
        Stream.of(toggleMod, toggleIcons, toggleTab, toggleChat, toggleSeparatorMode, cycleDisplayMode, autoKitDetect, clearPlayerCache, leftPvPTiers, rightPvPTiers, activeRightMode, activeLeftMode, enableOwnProfile, useClassicIcons, usePvPTiersIcons, useMCTiersIcons)
                .forEach(this::addRenderableWidget);
    }

//    private void updateRightSwitcher(ButtonWidget buttonWidget, ButtonWidget leftMCTiers, ButtonWidget centerMCTiers) {
//        leftMCTiers.active = true;
//        centerMCTiers.active = true;
//        buttonWidget.active = false;
//        ConfigManager.saveConfig();
//    }
//
//    private void updateLeftSwitcher(ButtonWidget buttonWidget, ButtonWidget centerMCTiers, ButtonWidget rightMCTiers) {
//        buttonWidget.active = false;
//        centerMCTiers.active = true;
//        rightMCTiers.active = true;
//        ConfigManager.saveConfig();
//    }

    private void updateRightSwitcher(Button buttonWidget, Button leftMCTiers) {
        leftMCTiers.active = true;
        buttonWidget.active = false;
        ConfigManager.saveConfig();
    }

    private void updateLeftSwitcher(Button buttonWidget, Button rightMCTiers) {
        buttonWidget.active = false;
        rightMCTiers.active = true;
        ConfigManager.saveConfig();
    }

    private void updateVisibilities() {
        activeRightMode.visible = VTiersClient.positionPvPTiers == VTiersClient.DisplayStatus.RIGHT;
        activeLeftMode.visible = VTiersClient.positionPvPTiers == VTiersClient.DisplayStatus.LEFT;
    }

    private void drawPlayerAvatar(GuiGraphicsExtractor context, int x, int y) {
        if (imageReady) {
            if (ownProfile.imageSaved == 1 || ownProfile.imageSaved == 2)
                context.blit(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, x - height / 10 / 2, y, 0, 0, height / 10, (int) (height / 4.166), height / 10, (int) (height / 4.166));
            else if (ownProfile.imageSaved < 6 && ownProfile.imageSaved > 2)
                context.blit(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, x - height / 7 / 2, y, 0, 0, height / 7, (int) (height / 4.145), height / 7, (int) (height / 4.145));
        } else if (ownProfile.imageSaved != 0) {
            loadPlayerAvatar();
        } else if (ownProfile.numberOfImageRequests >= 6)
            context.centeredText(font, Component.nullToEmpty(ownProfile.name + "'s skin failed to load. Restart game to retry"), x, y + 50, ColorControl.getColorMinecraftStandard("red"));
    }

    private void loadPlayerAvatar() {
        if (imageReady)
            return;

        try (FileInputStream fileInputStream = new FileInputStream(FabricLoader.getInstance().getGameDir().resolve("cache/vtiers/" + (useOwnProfile ? ownProfile.uuid : defaultProfile.uuid) + ".png").toFile())) {
            Minecraft.getInstance().getTextureManager().register(playerAvatarTexture, new DynamicTexture(null, NativeImage.read(fileInputStream)));
            imageReady = true;
        } catch (IOException ignored) {
            LOGGER.warn("Error loading player skin");
        }
    }

    public static Screen getConfigScreen(Screen ignoredScreen) {
        return new ConfigScreen();
    }
}