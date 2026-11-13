package com.vtiers.textures;

import com.google.gson.JsonObject;
import java.util.HashMap;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.Style;
import net.minecraft.resources.Identifier;

public class ColorControl {
    private static final HashMap<String, Integer> colors = new HashMap<>();

    public static void updateColors(JsonObject jsonObject) {
        colors.clear();
        if (jsonObject != null) {
            jsonObject.keySet().forEach(key -> {
                try {
                    String hex = jsonObject.get(key).getAsString().replace("#", "");
                    colors.put(key, Integer.parseUnsignedInt(hex, 16));
                } catch (Exception ignored) {
                }
            });
        }

        Icons.GLOBE = Component.literal("\uF000").setStyle(Style.EMPTY.withColor(getColorMinecraftStandard("region")).withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "misc"))));
        Icons.OVERALL = Component.literal("\uF001").setStyle(Style.EMPTY.withColor(getColorMinecraftStandard("overall")).withFont(new FontDescription.Resource(Identifier.fromNamespaceAndPath("minecraft", "misc"))));
    }

    public static int getColor(String colorName) {
        return colors.getOrDefault(colorName, 0xaaaaaa);
    }

    public static int getColorMinecraftStandard(String colorName) {
        return 0xff000000 | (colors.getOrDefault(colorName, 0xaaaaaa) & 0x00ffffff);
    }
}