package net.etylop.immersivefarming.block.custom;

import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.entity.SprinklerRegistry;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.particle.IFParticles;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.etylop.immersivefarming.utils.ModTags;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FarmBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.List;

import static net.etylop.immersivefarming.utils.IFFunctions.getHoeSpeed;

public class Soil extends FarmBlock {
    public static final int TILL_MAX = 7;
    public static final IntegerProperty TILL = IntegerProperty.create("till",0,TILL_MAX);
    public static final int FERTILITY_MAX = 2;
    public static final IntegerProperty FERTILITY = IntegerProperty.create("fertility", 0, FERTILITY_MAX);

    public static final IntegerProperty WATER = MOISTURE;
    // Next to a water block
    public static final int WATER_FLOODED = MAX_MOISTURE;
    // Max level reached by watering
    public static final int WATER_SATURATED = WATER_FLOODED - 1;

    private static final IFConfig.CropConfig cropConfig = IFConfig.get().cropConfig;

    public static final BooleanProperty CONTAMINATED = BooleanProperty.create("contaminated");
    // Treated with Bordeaux mixture: the crop can't get sick until it is fully grown (or removed)
    public static final BooleanProperty PROTECTED = BooleanProperty.create("protected");

    // Progress towards rich soil (one step per crop grown to maturity on it), RICHNESS_MAX = rich.
    // Rich soil gives a yield bonus, see RichSoilYieldModifier.
    public static final int RICHNESS_MAX = 10;
    public static final IntegerProperty RICHNESS = IntegerProperty.create("richness", 0, RICHNESS_MAX);


    public Soil(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(TILL, 0)
                .setValue(FERTILITY, 0)
                .setValue(CONTAMINATED, false)
                .setValue(PROTECTED, false)
                .setValue(RICHNESS, 0)
        );
    }

    public static BlockState fromFarmland(BlockState farmland) {
        return IFBlocks.SOIL.get().defaultBlockState()
                .setValue(TILL, TILL_MAX)
                .setValue(WATER, Math.min(farmland.getOptionalValue(MOISTURE).orElse(0), WATER_SATURATED))
                .setValue(RICHNESS, farmland.is(ModTags.Blocks.RICH_FARMLAND) ? RICHNESS_MAX : 0);
    }

    public static boolean isRich(BlockState state) {
        return state.getBlock() instanceof Soil && state.getValue(RICHNESS) == RICHNESS_MAX;
    }

    /** Soil after a crop matured on it: one step closer to rich soil. */
    public static BlockState enrich(BlockState state) {
        int richness = state.getValue(RICHNESS);
        int needed = cropConfig.harvests_to_enrich.get();
        if (needed == 0 || richness == RICHNESS_MAX) return state;
        return state.setValue(RICHNESS, richness + 1 >= needed ? RICHNESS_MAX : richness + 1);
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(TILL);
        pBuilder.add(FERTILITY);
        pBuilder.add(CONTAMINATED);
        pBuilder.add(PROTECTED);
        pBuilder.add(RICHNESS);
    }


    @Override
    public boolean canSustainPlant(BlockState state, BlockGetter level, BlockPos pos, Direction facing, IPlantable plantable) {
        if (state.hasProperty(Soil.TILL) && state.getValue(Soil.TILL) == Soil.TILL_MAX) {
            BlockState farmlandState = Blocks.FARMLAND.defaultBlockState();
            return farmlandState.canSustainPlant(level, pos, facing, plantable);
        }
        return false;
    }

    // Soil can't be trampled: only apply the regular fall damage (skips FarmBlock#fallOn)
    @Override
    public void fallOn(Level level, BlockState state, BlockPos pos, Entity entity, float fallDistance) {
        entity.causeFallDamage(fallDistance, 1.0F, entity.damageSources().fall());
    }

    @Nullable
    @Override
    public BlockState getToolModifiedState(BlockState state, UseOnContext context, ToolAction toolAction, boolean simulate) {
        if (toolAction == ToolActions.SHOVEL_FLATTEN) {
            Block richSoil = isRich(state) ? IFFunctions.getRichSoilBlock() : null;
            return richSoil != null ? richSoil.defaultBlockState() : Blocks.DIRT.defaultBlockState();
        }
        // Called by hoes, and by any modded tool or machine able to till (Create plough, Tinkers', Mekanism...)
        if (toolAction == ToolActions.HOE_TILL) {
            int till = state.getValue(TILL);
            if (till >= TILL_MAX || context.getClickedFace() == Direction.DOWN ||
                !context.getLevel().getBlockState(context.getClickedPos().above()).isAir()) {
                return null;
            }
            return state.setValue(TILL, Math.min(TILL_MAX, till + getHoeSpeed(context.getItemInHand())));
        }
        return super.getToolModifiedState(state, context, toolAction, simulate);
    }

    // Rich soil drops Farmer's Delight rich soil instead of dirt, when that mod is installed
    @Override
    public List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        Block richSoil = isRich(state) ? IFFunctions.getRichSoilBlock() : null;
        if (richSoil == null) return drops;
        return drops.stream()
                .map(stack -> stack.is(Items.DIRT) ? new ItemStack(richSoil, stack.getCount()) : stack)
                .toList();
    }

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockState newState = pState;

        // Update water level
        int water = pState.getValue(WATER);
        int sprinkler = SprinklerRegistry.getWatering(pLevel, pPos);
        if (isNextToWater(pLevel, pPos)) {
            water = WATER_FLOODED;
        }
        else if (water == WATER_FLOODED) {
            water = WATER_SATURATED;
        }
        else if (sprinkler != SprinklerRegistry.NONE || pLevel.isRainingAt(pPos.above())) {
            water = Math.min(WATER_SATURATED, water + 1);
        }
        else if (water > 0 && pRandom.nextDouble() < cropConfig.evaporation.get()) {
            water--;
        }
        newState = newState.setValue(WATER, water);

        // Update contaminated level
        boolean contaminated = pState.getValue(CONTAMINATED);
        boolean isUnderCrops = isUnderCrops(pLevel, pPos);
        if (!isUnderCrops) {
            // The protection only lasts for the crop it was applied to
            newState = newState.setValue(CONTAMINATED, false).setValue(PROTECTED, false);
        }
        else if (sprinkler == SprinklerRegistry.TREATED_WATER || pState.getValue(PROTECTED)) {
            newState = newState.setValue(CONTAMINATED, false);
        }
        else if (!contaminated && Math.random()<cropConfig.start_contamination.get()) {
            newState = newState.setValue(CONTAMINATED, true);
        }
        else if (contaminated && Math.random()<cropConfig.lethality_contamination.get()) {
            IFFunctions.killPlant(pLevel, pPos.above(), 3);
            // Keep the soil (water, fertility, tilling) but clear the contamination with the crop gone
            pLevel.setBlock(pPos, newState.setValue(CONTAMINATED, false), 2);
            return;
        }
        else if (contaminated && cropConfig.spread_contamination.get()) {
            for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-1, 0, -1), pPos.offset(1, 1, 1))) {
                if (blockpos.equals(pPos))
                    continue;
                BlockState neighbour = pLevel.getBlockState(blockpos);
                if (neighbour.getBlock() instanceof Soil && !neighbour.getValue(CONTAMINATED) && !neighbour.getValue(PROTECTED) && isUnderCrops(pLevel, blockpos) && Math.random()<cropConfig.proximity_contamination.get()) {
                    pLevel.setBlock(blockpos, neighbour.setValue(CONTAMINATED, true), 2);
                }
            }
        }

        if (newState != pState) {
            pLevel.setBlock(pPos, newState, 2);
        }
    }

    @Override
    public void animateTick(BlockState pState, Level pLevel, BlockPos pPos, RandomSource pRandom) {
        if (pState.getValue(CONTAMINATED)) {
            spawnContaminatedParticles(pLevel, pPos);
        }
    }

    private static void spawnContaminatedParticles(Level level, BlockPos pos) {
        for(int i = 0; i < 10; i++) {
            level.addParticle(IFParticles.CONTAMINATION_PARTICLES.get(),
                    pos.getX() + Math.random(), pos.getY() + 1 + Math.random(), pos.getZ() + Math.random(),
                    0,0,0);
        }
    }

    private static boolean isNextToWater(LevelReader pLevel, BlockPos pPos) {
        for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-1, 0, -1), pPos.offset(1, 1, 1))) {
            if (pLevel.getFluidState(blockpos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    /** Whether the soil holds a crop that can be contaminated: one that is still growing (not dead, not fully grown). */
    private static boolean isUnderCrops(BlockGetter pLevel, BlockPos pPos) {
        BlockState plant = pLevel.getBlockState(pPos.above());
        if (plant.getBlock() instanceof DeadCrop || !IFFunctions.isGrowing(plant)) return false;
        BlockState state = pLevel.getBlockState(pPos);
        return plant.getBlock() instanceof net.minecraftforge.common.IPlantable && state.canSustainPlant(pLevel, pPos, Direction.UP, (net.minecraftforge.common.IPlantable)plant.getBlock());
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATER)>0;
    }
}
