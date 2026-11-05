package net.etylop.immersivefarming.mixin;

import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Crops on soil can't be bonemealed, even by mods that call the bonemeal methods directly
 * without firing BonemealEvent (Create: Slice & Dice, fertilizer machines...).
 * Modded crops extending CropBlock are covered as long as they don't fully override these methods
 * (overrides of performBonemeal usually call growCrops, which is also blocked).
 */
@Mixin(CropBlock.class)
public abstract class CropBlockMixin {

    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient, CallbackInfoReturnable<Boolean> cir) {
        if (IFFunctions.isBonemealBlocked(level, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (IFFunctions.isBonemealBlocked(level, pos)) ci.cancel();
    }

    @Inject(method = "growCrops", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$growCrops(Level level, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (IFFunctions.isBonemealBlocked(level, pos)) ci.cancel();
    }
}
