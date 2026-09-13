package com.vtiers.mixin.client;

import com.vtiers.VTiersClient;
import com.vtiers.profile.PlayerProfile;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatHud.class)
public class ModifyChatClientMixin {
    @ModifyVariable(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/text/Text;Lnet/minecraft/network/message/MessageSignatureData;Lnet/minecraft/client/gui/hud/MessageIndicator;)V", argsOnly = true)
    private Text addMessage(Text original) {
        if (!VTiersClient.toggleMod)
            return original;

        if (!VTiersClient.toggleChat)
            return PlayerProfile.stripTierTags(original);

        return PlayerProfile.getFullyReplaced(original);
    }
}