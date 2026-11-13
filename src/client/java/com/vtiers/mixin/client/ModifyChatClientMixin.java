package com.vtiers.mixin.client;

import com.vtiers.VTiersClient;
import com.vtiers.profile.PlayerProfile;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.Component;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

@Mixin(ChatComponent.class)
public class ModifyChatClientMixin {
    @ModifyVariable(at = @At("HEAD"), method = "addMessage(Lnet/minecraft/network/chat/Component;Lnet/minecraft/network/chat/MessageSignature;Lnet/minecraft/client/multiplayer/chat/GuiMessageSource;Lnet/minecraft/client/multiplayer/chat/GuiMessageTag;)V", argsOnly = true)
    private Component addMessage(Component original) {
        if (!VTiersClient.toggleMod)
            return original;

        if (!VTiersClient.toggleChat)
            return PlayerProfile.stripTierTags(original);

        return PlayerProfile.getFullyReplaced(original);
    }
}