package com.vtiers.mixin.client;

import net.minecraft.entity.data.DataTracker;
import net.minecraft.entity.data.TrackedData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(DataTracker.class)
public interface DataTrackerAccessor {
    @Invoker("set")
    <T> void invokeSet(TrackedData<T> key, T value, boolean force);
}