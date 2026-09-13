package com.vtiers.mixin.client;

import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.vtiers.VTiersClient;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;

@Mixin(PlayerEntity.class)
public abstract class ModifyNametagsClientMixin {
    @Shadow
    public abstract String getNameForScoreboard();

    @Unique
    private int vtiers_cacheVersion;

    @Unique
    private Text vtiers_lastOriginal;

    @Unique
    private Text vtiers_cached;

    @ModifyReturnValue(at = @At("RETURN"), method = "getDisplayName")
    private Text modifyDisplayName(Text original) {
        if (!VTiersClient.toggleMod)
            return original;

        if (original == vtiers_lastOriginal && vtiers_cacheVersion == VTiersClient.cacheVersion)
            return vtiers_cached;
        vtiers_cacheVersion = VTiersClient.cacheVersion;
        vtiers_lastOriginal = original;

        return vtiers_cached = VTiersClient.addGetPlayer(getNameForScoreboard(), false).getFullName(original);
    }
}