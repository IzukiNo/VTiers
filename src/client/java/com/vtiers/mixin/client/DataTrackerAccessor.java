package com.vtiers.mixin.client;

import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.SynchedEntityData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(SynchedEntityData.class)
public interface DataTrackerAccessor {
    @Invoker("set")
    <T> void invokeSet(EntityDataAccessor<T> key, T value, boolean force);
}