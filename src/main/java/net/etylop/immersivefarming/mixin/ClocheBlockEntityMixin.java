package net.etylop.immersivefarming.mixin;

import blusunrize.immersiveengineering.api.crafting.ClocheFertilizer;
import blusunrize.immersiveengineering.common.blocks.metal.ClocheBlockEntity;
import net.etylop.immersivefarming.config.IFConfig;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.common.ForgeConfigSpec;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/**
 * Garden cloche tweaks:
 * - a dose (water + fertilizer) is only taken when the fertilizer slot holds a valid fertilizer (immersiveengineering:fertilizer recipes,
 *   overridden in data/immersiveengineering/recipes/fertilizer to keep only Industrial Fertilizer at 1x growth),
 *   so the cloche stops once its current dose runs out (vanilla IE runs on water alone, fertilizer only speeds it up)
 * - the energy and fertilizer consumptions are multiplied by the configured factors
 * - the growth time is multiplied by the configured factor
 */
@Mixin(value = ClocheBlockEntity.class, remap = false)
public abstract class ClocheBlockEntityMixin {

    // Only call to getFluidAmount in tickServer: the check "tank has enough water to start a new dose"
    @Redirect(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/fluids/capability/templates/FluidTank;getFluidAmount()I"))
    private int immersivefarming$requireFertilizer(FluidTank tank) {
        if (IFConfig.get().cloche.require_fertilizer.get()) {
            ClocheBlockEntity self = (ClocheBlockEntity) (Object) this;
            ItemStack fertilizer = self.getInventory().get(2);
            if (fertilizer.isEmpty() || !ClocheFertilizer.isValidFertilizer(self.getLevel(), fertilizer)) return 0;
        }
        return tank.getFluidAmount();
    }

    // First IntValue.get() in tickServer is IEServerConfig.MACHINES.cloche_consumption
    @Redirect(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$IntValue;get()Ljava/lang/Object;", ordinal = 0))
    private Object immersivefarming$energyConsumption(ForgeConfigSpec.IntValue consumption) {
        return (int) Math.round(consumption.get() * IFConfig.get().cloche.energy_multiplier.get());
    }

    // Only DoubleValue.get() in tickServer is IEServerConfig.MACHINES.cloche_growth_mod, the growth added each tick.
    // Slowing the growth (instead of raising the recipe time) keeps the renderer and GUI progress (growth / time) correct
    @Redirect(method = "tickServer", at = @At(value = "INVOKE",
            target = "Lnet/minecraftforge/common/ForgeConfigSpec$DoubleValue;get()Ljava/lang/Object;"))
    private Object immersivefarming$growthTime(ForgeConfigSpec.DoubleValue growthMod) {
        return growthMod.get() / IFConfig.get().cloche.growth_time_multiplier.get();
    }

    // First write to fertilizerAmount in tickServer is the per-tick decrement (fertilizerAmount - 1), the second one is the refill
    @Redirect(method = "tickServer", at = @At(value = "FIELD", opcode = Opcodes.PUTFIELD,
            target = "Lblusunrize/immersiveengineering/common/blocks/metal/ClocheBlockEntity;fertilizerAmount:I", ordinal = 0))
    private void immersivefarming$fertilizerConsumption(ClocheBlockEntity self, int decremented) {
        int extra = IFConfig.get().cloche.fertilizer_consumption.get() - 1;
        self.fertilizerAmount = Math.max(0, decremented - extra);
    }
}
