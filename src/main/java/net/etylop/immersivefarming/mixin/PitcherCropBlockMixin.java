package net.etylop.immersivefarming.mixin;

import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.PitcherCropBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.ForgeHooks;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Plants on soil can't be bonemealed, see {@link CropBlockMixin}. */
@Mixin(PitcherCropBlock.class)
public abstract class PitcherCropBlockMixin {

    @Shadow
    private void grow(ServerLevel level, BlockState state, BlockPos pos, int ageIncrement) {
        throw new AssertionError();
    }

    // Vanilla pitcher crops grow without firing CropGrowEvent: fire it so that soil rules apply to them
    @Redirect(method = "randomTick", at = @At(value = "INVOKE",
            target = "Lnet/minecraft/world/level/block/PitcherCropBlock;grow(Lnet/minecraft/server/level/ServerLevel;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/BlockPos;I)V"))
    private void immersivefarming$growWithForgeHooks(PitcherCropBlock self, ServerLevel level, BlockState state, BlockPos pos, int ageIncrement) {
        if (ForgeHooks.onCropsGrowPre(level, pos, state, true)) {
            this.grow(level, state, pos, ageIncrement);
            ForgeHooks.onCropsGrowPost(level, pos, level.getBlockState(pos));
        }
    }

    @Inject(method = "isValidBonemealTarget", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state, boolean isClient, CallbackInfoReturnable<Boolean> cir) {
        if (IFFunctions.isBonemealBlocked(level, pos)) cir.setReturnValue(false);
    }

    @Inject(method = "performBonemeal", at = @At("HEAD"), cancellable = true)
    private void immersivefarming$performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state, CallbackInfo ci) {
        if (IFFunctions.isBonemealBlocked(level, pos)) ci.cancel();
    }
}
