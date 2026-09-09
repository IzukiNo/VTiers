package com.tiers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.mojang.authlib.GameProfile;
import com.tiers.TiersClient;
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
    private int tiers_cacheVersion;

    @Unique
    private Text tiers_lastOriginal;

    @Unique
    private Text tiers_cached;

    @Inject(at = @At(value = "TAIL"), method = "<init>")
    private void onConstruct(GameProfile profile, boolean secureChatEnforced, CallbackInfo ci) {
        if (TiersClient.toggleMod)
            TiersClient.addGetPlayer(profile.name(), false);
    }

    @ModifyReturnValue(at = @At("RETURN"), method = "getDisplayName")
    private Text modifyPlayerName(Text original) {
        if (!TiersClient.toggleMod || !TiersClient.toggleTab)
            return original;

        Text baseText = original != null ? original : Text.literal(getProfile().name());

        if (baseText == tiers_lastOriginal && tiers_cacheVersion == TiersClient.cacheVersion)
            return tiers_cached;
        tiers_cacheVersion = TiersClient.cacheVersion;
        tiers_lastOriginal = baseText;

        Text replaced = TiersClient.addGetPlayer(getProfile().name(), false).deepReplace(baseText);
        if (original == null && replaced == baseText)
            return null;

        return tiers_cached = replaced;
    }
}