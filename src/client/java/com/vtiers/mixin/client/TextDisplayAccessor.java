package com.vtiers.mixin.client;

import net.minecraft.entity.data.TrackedData;
import net.minecraft.entity.decoration.DisplayEntity;
import net.minecraft.text.Text;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(DisplayEntity.TextDisplayEntity.class)
public interface TextDisplayAccessor {
    @Accessor("TEXT")
    static TrackedData<Text> getTEXT() {
        throw new AssertionError();
    }
}