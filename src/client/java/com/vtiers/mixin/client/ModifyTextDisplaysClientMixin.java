package com.vtiers.mixin.client;

import com.vtiers.VTiersClient;
import com.vtiers.profile.PlayerProfile;
import net.minecraft.client.renderer.entity.DisplayRenderer;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.FormattedText;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

@Mixin(DisplayRenderer.TextDisplayRenderer.class)
public abstract class ModifyTextDisplaysClientMixin {
    @ModifyArg(at = @At(value = "INVOKE", target = "Lnet/minecraft/client/gui/Font;split(Lnet/minecraft/network/chat/FormattedText;I)Ljava/util/List;"), method = "splitLines")
    public FormattedText modifyLines(FormattedText original) {
        if (!VTiersClient.toggleMod || !(original instanceof Component originalText))
            return original;

        return PlayerProfile.getFullyReplaced(originalText);
    }
}