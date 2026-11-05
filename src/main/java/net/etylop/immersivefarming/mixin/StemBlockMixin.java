package net.etylop.immersivefarming.mixin;

import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.StemBlock;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plants on soil can't be bonemealed, see {@link CropBlockMixin}. */
@Mixin(StemBlock.class)
public abstract class StemBlockMixin {

    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient, CallbackInfoReturnable<Boolean> cir) {
        if (IFFunctions.isBonemealBlocked(level, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (IFFunctions.isBonemealBlocked(level, pos)) ci.cancel();
    }
}
