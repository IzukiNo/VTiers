package com.vtiers.textures;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.vtiers.PlayerProfileQueue;
import com.vtiers.VTiersClient;
import com.vtiers.profile.PlayerProfile;
import com.vtiers.screens.ConfigScreen;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.util.GsonHelper;

import static com.vtiers.VTiersClient.LOGGER;

public class ColorLoader implements PreparableReloadListener {
    public static Identifier identifier = Identifier.fromNamespaceAndPath("minecraft", "colors/pvptiers.json");

    @Override
    public CompletableFuture<Void> reload(SharedState store, Executor prepareExecutor, PreparationBarrier reloadSynchronizer, Executor applyExecutor) {
        return CompletableFuture.supplyAsync(() -> {
            if (store.resourceManager().getResource(identifier).isPresent()) {
                try {
                    return GsonHelper.fromJson(new Gson(), new InputStreamReader(store.resourceManager().getResource(identifier).get().open(), StandardCharsets.UTF_8), JsonObject.class);
                } catch (IOException ignored) {
                    LOGGER.warn("Error loading colors info");
                }
            }
            return null;
        }, prepareExecutor).thenCompose(reloadSynchronizer::wait).thenAcceptAsync(jsonObject -> {
            try {
                if (jsonObject != null) {
                    ColorControl.updateColors(jsonObject);
                    VTiersClient.restyleAllTexts(VTiersClient.playerProfiles);
                    VTiersClient.updateAllTags();
                }

                if (ConfigScreen.ownProfile == null) {
                    String ownName = null;
                    if (Minecraft.getInstance() != null && Minecraft.getInstance().getGameProfile() != null) {
                        ownName = Minecraft.getInstance().getGameProfile().name();
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