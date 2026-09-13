package com.vtiers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.vtiers.VTiersClient;
import net.minecraft.client.network.PlayerListEntry;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(PlayerListEntry.class)
public abstract class ModifyTabClientMixin {
    @Shadow
    public abstract GameProfile getProfile();

    @Unique
    private int vtiers_cacheVersion;

    @Unique
    private Text vtiers_lastOriginal;

    @Unique
    private Text vtiers_cached;

    @Inject(at = @At(value = "TAIL"), method = "<init>")
    private void onConstruct(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
        if (VTiersClient.toggleMod)
            VTiersClient.addGetPlayer(profile.name(), false);
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "getDisplayName")
    private Text modifyPlayerName(Text original) {
        if (!VTiersClient.toggleMod || !VTiersClient.toggleTab)
            return original;

        Text baseText = original != null ? original : Text.literal(getProfile().name());

        if (baseText == vtiers_lastOriginal && vtiers_cacheVersion == VTiersClient.cacheVersion)
            return vtiers_cached;
        vtiers_cacheVersion = VTiersClient.cacheVersion;
        vtiers_lastOriginal = baseText;

        Text replaced = VTiersClient.addGetPlayer(getProfile().name(), false).deepReplace(baseText);
        if (original == null && replaced == baseText)
            return null;

        return vtiers_cached = replaced;
    }
}