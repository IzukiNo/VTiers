package com.vtiers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.vtiers.VTiersClient;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(Player.class)
public abstract class ModifyNametagsClientMixin {
    @Shadow
    public abstract String getScoreboardName();

    @Unique
    private int vtiers_cacheVersion;

    @Unique
    private Component vtiers_lastOriginal;

    @Unique
    private Component vtiers_cached;

    @ModifyReturnValue(at = @At("RETURN"), method = "getDisplayName")
    private Component modifyDisplayName(Component original) {
        if (!VTiersClient.toggleMod)
            return original;

        if (original == vtiers_lastOriginal && vtiers_cacheVersion == VTiersClient.cacheVersion)
            return vtiers_cached;
        vtiers_cacheVersion = VTiersClient.cacheVersion;
        vtiers_lastOriginal = original;

        return vtiers_cached = VTiersClient.addGetPlayer(getScoreboardName(), false).getFullName(original);
    }
}