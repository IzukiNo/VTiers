package com.vtiers.mixin.client;

import com.vtiers.VTiersClient;
import com.vtiers.profile.PlayerProfile;
import net.minecraft.client.render.entity.DisplayEntityRenderer;
import net.minecraft.text.StringVisitable;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DisplayEntityRenderer.TextDisplayEntityRenderer.class)
public abstract class ModifyTextDisplaysClientMixin {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/font/TextRenderer;wrapLines(Lnet/minecraft/text/StringVisitable;I)Ljava/util/List;"), method = "getLines")
    public StringVisitable modifyLines(StringVisitable original) {
        if (!VTiersClient.toggleMod || !(original instanceof Text originalText))
            return original;

        return PlayerProfile.getFullyReplaced(originalText);
    }
}