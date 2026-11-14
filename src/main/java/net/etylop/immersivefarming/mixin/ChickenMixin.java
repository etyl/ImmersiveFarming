package net.etylop.immersivefarming.mixin;

import net.etylop.immersivefarming.entity.AnimalFeeding;
import net.minecraft.world.entity.animal.Chicken;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/**
 * Adult chickens must eat before each egg, see {@link AnimalFeeding}.
 * In Chicken#aiStep (server side), laying the egg (sound, egg, game event, new egg timer) is the last statement of the
 * method, so cancelling at its sound only skips laying. A hungry chicken keeps its egg timer at 0: it lays as soon as it
 * has eaten, and its next egg then takes the usual 5 to 10 minutes.
 */
@Mixin(Chicken.class)
public abstract class ChickenMixin {

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/animal/Chicken;playSound(Lnet/minecraft/sounds/SoundEvent;FF)V"), cancellable = true)
    private void immersivefarming$layOnlyWhenFed(CallbackInfo ci) {
        Chicken chicken = (Chicken) (Object) this;
        if (AnimalFeeding.isHungry(chicken)) {
            chicken.eggTime = 0;
            ci.cancel();
        } else {
            AnimalFeeding.produce(chicken);
        }
    }
}
