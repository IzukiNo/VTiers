package com.vtiers.screens;

import com.vtiers.VTiersClient;
import com.vtiers.profile.GameMode;
import com.vtiers.profile.PlayerProfile;
import com.vtiers.profile.Status;
import com.vtiers.profile.types.SuperProfile;
import com.vtiers.profile.types.VNListProfile;
import com.vtiers.textures.ColorControl;
import com.vtiers.textures.Icons;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.gui.components.toasts.SystemToast;
import net.minecraft.client.gui.screens.ConfirmLinkScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.HoverEvent;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;
import net.minecraft.util.CommonColors;

import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;

import static com.vtiers.VTiersClient.LOGGER;

import com.mojang.blaze3d.platform.NativeImage;

public class PlayerSearchResultScreen extends Screen {
    private final PlayerProfile playerProfile;
    private final Identifier playerAvatarTexture = Identifier.parse("");

    Button dimensionsWarning;

    private int separator;
    private boolean small;
    private boolean tooSmall;
    private boolean imageReady;
    private boolean toastShown;

    public PlayerSearchResultScreen(PlayerProfile playerProfile) {
        super(Component.nullToEmpty(playerProfile.name));
        this.playerProfile = playerProfile;
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor context, int mouseX, int mouseY, float delta) {
        if (!playerProfile.isPlayerValid()) {
            onClose();
            return;
        }

        if (playerProfile.nameChanged && !toastShown) {
            SystemToast.add(Minecraft.getInstance().gui.toastManager(), SystemToast.SystemToastId.NARRATOR_TOGGLE, Component.nullToEmpty("Recent name change"), Component.nullToEmpty("(" + playerProfile.name + " to " + playerProfile.inGameName + ") Data should be accurate"));
            toastShown = true;
        }

        int centerX = width / 2;
        int listY = (int) (height / 2.65);
        separator = height / 23;
        small = width < 575 || height < 420;
        tooSmall = width < 450 || height < 262;
        int avatarY = height / 55 + 12;

        super.extractRenderState(context, mouseX, mouseY, delta);

        dimensionsWarning.visible = small;
        if (tooSmall) {
            dimensionsWarning.setMessage(Component.nullToEmpty("⚠"));
            dimensionsWarning.setTooltip(Tooltip.create(Component.nullToEmpty("Your window dimensions (" + width + "x" + height + ") are too small\nLower the GUI scale or make the window bigger! (min: 450x262)")));
        }

        if (playerProfile.status == Status.SEARCHING) {
            context.centeredText(font, Component.nullToEmpty("Searching for " + playerProfile.name + "..."), centerX, listY, ColorControl.getColorMinecraftStandard("green"));
            return;
        }

        if (playerProfile.numberOfImageRequests == 0)
            playerProfile.savePlayerImage();

        drawPlayerAvatar(context, centerX, avatarY);
        if (!imageReady)
            context.centeredText(font, Component.nullToEmpty("Loading " + playerProfile.name + "'s skin"), centerX, avatarY + 50, ColorControl.getColorMinecraftStandard("green"));

        context.centeredText(font, playerProfile.getFullName(), centerX, height / 55, CommonColors.WHITE);

        drawCategoryList(context, VNListProfile.VNLIST_IMAGE, playerProfile.profileVNList != null ? playerProfile.profileVNList : playerProfile.profilePvPTiers, centerX, listY);
    }

    private void drawCategoryList(GuiGraphicsExtractor context, Identifier image, SuperProfile superProfile, int x, int y) {
        if (superProfile == null) {
            context.centeredText(font, "Loading from API...", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("green"));
            return;
        }

        context.blit(RenderPipelines.GUI_TEXTURED, image, x - 12, (int) (y + 2.4 * separator) + 4 - 38, 0, 0, 24, 24, 24, 24);

        if (superProfile.status == Status.SEARCHING) {
            context.centeredText(font, "Searching...", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("green"));
            return;
        } else if (superProfile.status == Status.NOT_EXISTING) {
            context.centeredText(font, "Unranked", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("red"));
            return;
        } else if (superProfile.status == Status.TIMEOUTED) {
            context.centeredText(font, "Search timeouted", x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("red"));
            renderFailedRequestMessage(context, superProfile, x, y);
            return;
        } else if (superProfile.status == Status.API_ISSUE) {
            context.centeredText(font, Component.literal("Search failed: API issue").setStyle(Style.EMPTY.withHoverEvent(new HoverEvent.ShowText(Component.nullToEmpty("hello")))), x, (int) (y + 2.8 * separator), ColorControl.getColorMinecraftStandard("red"));
            renderFailedRequestMessage(context, superProfile, x, y);

            context.centeredText(font, "Update Tiers or retry in a while", x, (int) (y + 2.8 * separator + 50), CommonColors.YELLOW);
            if (!superProfile.apiErrorShown) {
                addRenderableWidget(Button.builder(Component.literal("Report issue"), (buttonWidget) ->
                        ConfirmLinkScreen.confirmLinkNow(this, URI.create("https://github.com/IzukiNo/VTiers/issues"), true)).bounds(x - 40, (int) (y + 2.8 * separator + 50 + 12), 80, 20).tooltip(Tooltip.create(Component.nullToEmpty("Report this issue on GitHub. Make sure to report only if the same search on vnlist.asia doesn't fail"))).build());
                superProfile.apiErrorShown = true;
            }
            return;
        }

        if (!superProfile.drawn) {
            StringWidget regionLabel = new StringWidget(Icons.colorText("Region", "region"), font);
            regionLabel.setPosition(x - 44, (int) (y + 2.4 * separator));
            addRenderableWidget(regionLabel);

            StringWidget overallLabel = new StringWidget(Icons.colorText("Overall", "overall"), font);
            overallLabel.setPosition(x - 44, (int) (y + 2.4 * separator) + 16);
            addRenderableWidget(overallLabel);

            StringWidget regionIcon = new StringWidget(Icons.GLOBE, font);
            regionIcon.setPosition(x - 64, (int) (y + 2.4 * separator + 2));
            regionIcon.setTooltip(Tooltip.create(regionLabel.getMessage()));
            addRenderableWidget(regionIcon);

            StringWidget overallIcon = new StringWidget(Icons.OVERALL, font);
            overallIcon.setPosition(x - 64, (int) (y + 2.4 * separator + 2) + 16);
            overallIcon.setTooltip(Tooltip.create(overallLabel.getMessage()));
            addRenderableWidget(overallIcon);

            StringWidget region = new StringWidget(superProfile.displayedRegion, font);
            region.setPosition(x + 52 - (superProfile.displayedRegion.getString().length() - 2) * 3, (int) (y + 2.4 * separator));
            region.setTooltip(Tooltip.create(superProfile.regionTooltip));
            addRenderableWidget(region);

            StringWidget overall = new StringWidget(superProfile.displayedOverall, font);
            overall.setPosition(x + 52 - (superProfile.displayedOverall.getString().length() - 2) * 3, (int) (y + 2.4 * separator) + 16);
            overall.setTooltip(Tooltip.create(superProfile.overallTooltip));
            addRenderableWidget(overall);

            drawTierList(superProfile, x - 64, (int) (y + 2.4 * separator) + 40);

            superProfile.drawn = true;
        }
    }

    private void renderFailedRequestMessage(GuiGraphicsExtractor context, SuperProfile superProfile, int x, int y) {
        String[] messages = superProfile.checkOutages();
        context.centeredText(font, messages[0], x, (int) (y + 2.8 * separator) + 15, CommonColors.YELLOW);
        context.centeredText(font, messages[1], x, (int) (y + 2.8 * separator) + 25, CommonColors.YELLOW);
        context.centeredText(font, messages[2], x, (int) (y + 2.8 * separator) + 35, CommonColors.YELLOW);
    }

    private void drawTierList(SuperProfile superProfile, int x, int y) {
        int originalX = x;
        if (small) {
            y -= 7;
            int count = 1;
            int stage = 0;
            for (GameMode gameMode : superProfile.gameModes) {
                if (drawGameModeTiers(gameMode, x + 5, y + stage * 36)) {
                    x += 35;
                    if (count % 4 == 0) {
                        stage++;
                        x = originalX;
                    }
                    count++;
                }
            }
        } else {
            for (GameMode gameMode : superProfile.gameModes)
                if (drawGameModeTiers(gameMode, x, y)) y += 15;
        }
    }

    private boolean drawGameModeTiers(GameMode mode, int x, int y) {
        if (mode.drawn || mode.status != Status.READY)
            return false;

        StringWidget icon = new StringWidget(mode.gamemode.getIcon(), font);
        icon.setPosition(x, y + 3);
        if (small)
            icon.setPosition(x, y + 3);
        icon.setTooltip(Tooltip.create(mode.gamemode.getTextLabel()));
        addRenderableWidget(icon);

        StringWidget label = new StringWidget(mode.gamemode.getTextLabel(), font);
        label.setPosition(x + 20, y);
        if (!small)
            addRenderableWidget(label);

        StringWidget tier = new StringWidget(mode.displayedTier, font);
        tier.setPosition(x + 114 - (mode.displayedTier.getString().length() - 3) * 3, y);
        if (small)
            tier.setPosition(x - 2 - (mode.displayedTier.getString().length() - 3) * 2, y + 14);
        tier.setTooltip(Tooltip.create(mode.tierTooltip));
        addRenderableWidget(tier);

        if (mode.hasPeak && mode.peakTierTooltip.getStyle().getColor() != null) {
            StringWidget peakTier = new StringWidget(mode.displayedPeakTier, font);
            peakTier.setPosition(x + 136, y);
            if (small)
                peakTier.setPosition(x - 6, y + 24);
            peakTier.setTooltip(Tooltip.create(mode.peakTierTooltip));
            addRenderableWidget(peakTier);
        }

        mode.drawn = true;

        return true;
    }

    private void drawPlayerAvatar(GuiGraphicsExtractor context, int x, int y) {
        if (imageReady) {
            if (playerProfile.imageSaved == 1 || playerProfile.imageSaved == 2)
                context.blit(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, x - width / 32, y, 0, 0, width / 16, (int) (width / 6.666), width / 16, (int) (width / 6.666));
            else if (playerProfile.imageSaved < 6 && playerProfile.imageSaved > 2)
                context.blit(RenderPipelines.GUI_TEXTURED, playerAvatarTexture, (int) (x - width / 22.5), y, 0, 0, (int) (width / 11.25), (int) (width / 6.666), (int) (width / 11.25), (int) (width / 6.666));
        } else if (playerProfile.imageSaved != 0) {
            loadPlayerAvatar();
        } else if (playerProfile.numberOfImageRequests >= 6)
            context.centeredText(font, Component.nullToEmpty(playerProfile.name + "'s skin failed to load. Clear cache and retry"), x, y + 50, ColorControl.getColorMinecraftStandard("red"));
    }

    private void loadPlayerAvatar() {
        if (imageReady)
            return;

        try (FileInputStream fileInputStream = new FileInputStream(FabricLoader.getInstance().getGameDir().resolve("cache/vtiers/players/" + playerProfile.uuid + ".png").toFile())) {
            Minecraft.getInstance().getTextureManager().register(playerAvatarTexture, new DynamicTexture(null, NativeImage.read(fileInputStream)));
            imageReady = true;
        } catch (IOException ignored) {
            LOGGER.warn("Error loading player skin");
        }
    }

    @Override
    public void init() {
        playerProfile.resetDrawnStatus();

        dimensionsWarning = Button.builder(Component.nullToEmpty("ℹ"), (buttonWidget) -> {
        }).bounds(width - 20 - 5, 5, 20, 20).tooltip(Tooltip.create(Component.nullToEmpty("Your window dimensions (" + width + "x" + height + ") are small\nLower the GUI scale or make the window bigger to have a better experience (ideal: 575x420)"))).build();
        dimensionsWarning.active = false;
        dimensionsWarning.visible = small;
        if (tooSmall) {
            dimensionsWarning.setMessage(Component.nullToEmpty("⚠"));
            dimensionsWarning.setTooltip(Tooltip.create(Component.nullToEmpty("Your window dimensions (" + width + "x" + height + ") are too small\nLower the GUI scale or make the window bigger! (min: 430x262)")));
        }

        addRenderableWidget(dimensionsWarning);

        addRenderableWidget(Button.builder(Icons.NAMEMC, (ignored) ->
                ConfirmLinkScreen.confirmLinkNow(this, URI.create("https://namemc.com/profile/" + playerProfile.uuid), true)).bounds(width - 20 - 5, height - 20 - 5, 20, 20).tooltip(Tooltip.create(Component.literal("Open " + playerProfile.targetName + "'s NameMC page"))).build());

        addRenderableWidget(Button.builder(Component.nullToEmpty("Update"), (buttonWidget) -> VTiersClient.showUpdatedPlayerProfile(playerProfile, true)).bounds(5, height - 20 - 5, 68, 20).tooltip(Tooltip.create(Component.nullToEmpty("Reload the player profile"))).build());
        addRenderableWidget(Button.builder(Icons.CYCLE, (buttonWidget) -> playerProfile.updateTierlistProfiles(2)).bounds(5, height - 20 - 5 - 22, 20, 20).tooltip(Tooltip.create(Component.literal(("Update VNList results")))).build());
    }
}