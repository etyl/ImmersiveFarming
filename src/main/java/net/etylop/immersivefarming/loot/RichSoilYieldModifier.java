package net.etylop.immersivefarming.loot;

import com.google.common.base.Suppliers;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.loot.IGlobalLootModifier;
import net.minecraftforge.common.loot.LootModifier;
import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Yield bonus of rich soil: everything dropped by a mature plant growing on rich soil is increased by
 * rich_yield_bonus (0.3 = +30%). Fractions are rounded randomly, so the average gain is exact.
 */
public class RichSoilYieldModifier extends LootModifier {
    public static final Supplier<Codec<RichSoilYieldModifier>> CODEC = Suppliers.memoize(() ->
            RecordCodecBuilder.create(inst -> codecStart(inst).apply(inst, RichSoilYieldModifier::new)));

    // Tallest plant (in blocks) that is searched when looking for the soil under a plant
    private static final int MAX_PLANT_HEIGHT = 8;

    public RichSoilYieldModifier(LootItemCondition[] conditions) {
        super(conditions);
    }

    @Override
    protected @NotNull ObjectArrayList<ItemStack> doApply(ObjectArrayList<ItemStack> loot, LootContext context) {
        BlockState plant = context.getParamOrNull(LootContextParams.BLOCK_STATE);
        Vec3 origin = context.getParamOrNull(LootContextParams.ORIGIN);
        if (plant == null || origin == null || loot.isEmpty() || !IFFunctions.isRichSoilHarvest(plant)) {
            return loot;
        }

        // The broken block may already be gone: look for the soil under the rest of the plant
        ServerLevel level = context.getLevel();
        Block plantBlock = plant.getBlock();
        BlockPos base = BlockPos.containing(origin);
        for (int i = 0; i < MAX_PLANT_HEIGHT && level.getBlockState(base.below()).is(plantBlock); i++) {
            base = base.below();
        }
        if (!Soil.isRich(level.getBlockState(base.below()))) {
            return loot;
        }

        double bonus = IFConfig.get().cropConfig.rich_yield_bonus.get();
        ObjectArrayList<ItemStack> result = new ObjectArrayList<>();
        for (ItemStack stack : loot) {
            double extra = stack.getCount() * bonus;
            int count = stack.getCount() + (int) extra + (context.getRandom().nextDouble() < extra % 1 ? 1 : 0);
            // Split into several stacks if the bonus goes over the stack size
            while (count > 0) {
                int size = Math.min(count, stack.getMaxStackSize());
                result.add(stack.copyWithCount(size));
                count -= size;
            }
        }
        return result;
    }

    @Override
    public Codec<? extends IGlobalLootModifier> codec() {
        return CODEC.get();
    }
}
