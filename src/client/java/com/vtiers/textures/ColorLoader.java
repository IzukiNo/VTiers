package com.vtiers.textures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vtiers.PlayerProfileQueue;
import com.vtiers.VTiersClient;
import com.vtiers.profile.PlayerProfile;
import com.vtiers.screens.ConfigScreen;
import net.fabricmc.fabric.api.resource.IdentifiableResourceReloadListener;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;
import net.minecraft.util.JsonHelper;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

import static com.vtiers.VTiersClient.LOGGER;

public class ColorLoader implements IdentifiableResourceReloadListener {
    public static Identifier identifier = Identifier.of("minecraft", "colors/pvptiers.json");

    @Override
    public Identifier getFabricId() {
        return Identifier.of("vtiers", "color_loader");
    }

    @Override
    public CompletableFuture<Void> reload(Synchronizer reloadSynchronizer, ResourceManager resourceManager, Executor prepareExecutor, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            if (resourceManager.getResource(identifier).isPresent()) {
                try {
                    return JsonHelper.deserialize(new Gson(), new InputStreamReader(resourceManager.getResource(identifier).get().getInputStream(), StandardCharsets.UTF_8), JsonObject.class);
                } catch (IOException ignored) {
                    LOGGER.warn("Error loading colors info");
                }
            }
            return null;
        }, prepareExecutor).thenCompose(reloadSynchronizer::whenPrepared).thenAcceptAsync(jsonObject -> {
            try {
                if (jsonObject != null) {
                    ColorControl.updateColors(jsonObject);
                    VTiersClient.restyleAllTexts(VTiersClient.playerProfiles);
                    VTiersClient.updateAllTags();
                }

                if (ConfigScreen.ownProfile == null) {
                    String ownName = null;
                    if (MinecraftClient.getInstance() != null && MinecraftClient.getInstance().getGameProfile() != null) {
                        ownName = MinecraftClient.getInstance().getGameProfile().getName();
                    }
                    if (ownName != null && !ownName.isEmpty()) {
                        ConfigScreen.ownProfile = new PlayerProfile(ownName, false);
                        PlayerProfileQueue.putFirstInQueue(ConfigScreen.ownProfile);
                    }

                    String defaultProfileMojang = loadStringFromResources("json/defaultProfileMojang.json");
                    String defaultProfileVNList = loadStringFromResources("json/defaultProfileVNList.json");

                    ConfigScreen.defaultProfile = new PlayerProfile(defaultProfileMojang, defaultProfileVNList);

                } else {
                    ArrayList<PlayerProfile> configProfiles = new ArrayList<>();
                    if (ConfigScreen.defaultProfile != null) configProfiles.add(ConfigScreen.defaultProfile);
                    if (ConfigScreen.ownProfile != null) configProfiles.add(ConfigScreen.ownProfile);
                    VTiersClient.restyleAllTexts(configProfiles);
                }
            } catch (Exception e) {
                LOGGER.error("Error applying color resource reload", e);
            }
        }, applyExecutor);
    }

    private static String loadStringFromResources(String path) {
        try (InputStream inputStream = ColorLoader.class.getClassLoader().getResourceAsStream(path)) {
            if (inputStream != null) {
                try (BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream))) {
                    String line;
                    StringBuilder stringBuilder = new StringBuilder();
                    while ((line = bufferedReader.readLine()) != null) {
                        stringBuilder.append(line);
                        stringBuilder.append(System.lineSeparator());
                    }

                    return stringBuilder.toString();
                }
            }
        } catch (IOException ignored) {
            LOGGER.warn("Error loading default jsons");
        }

        return "";
    }
}