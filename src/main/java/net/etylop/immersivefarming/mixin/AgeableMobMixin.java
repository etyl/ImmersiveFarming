package net.etylop.immersivefarming.mixin;

import net.etylop.immersivefarming.entity.AnimalFeeding;
import net.minecraft.world.entity.AgeableMob;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Babies that need food only grow while they are fed, see {@link AnimalFeeding}.
 * In AgeableMob#aiStep (server side), the first setAge call is the baby growing by one tick
 * (the second one is the adult's breeding cooldown, which is not affected). It is the last statement of the method,
 * so cancelling there only skips the growth.
 */
@Mixin(AgeableMob.class)
public abstract class AgeableMobMixin {

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/AgeableMob;setAge(I)V", ordinal = 0), cancellable = true)
    private void immersivefarming$growOnlyWhenFed(CallbackInfo ci) {
        if (!AnimalFeeding.tryGrow((AgeableMob) (Object) this)) ci.cancel();
    }
}
