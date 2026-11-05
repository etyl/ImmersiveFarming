package net.etylop.immersivefarming.block.custom;

import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.entity.SprinklerBlockEntity;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.particle.IFParticles;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
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
import net.minecraft.world.level.chunk.ChunkAccess;
import net.minecraftforge.common.IPlantable;
import net.minecraftforge.common.ToolAction;
import net.minecraftforge.common.ToolActions;
import org.jetbrains.annotations.Nullable;

import java.util.Set;

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


    public Soil(Properties properties) {
        super(properties);
        this.registerDefaultState(
            this.stateDefinition.any()
                .setValue(TILL, 0)
                .setValue(FERTILITY, 0)
                .setValue(CONTAMINATED, false)
        );
    }

    public static BlockState fromFarmland(BlockState farmland) {
        return IFBlocks.SOIL.get().defaultBlockState()
                .setValue(TILL, TILL_MAX)
                .setValue(WATER, Math.min(farmland.getOptionalValue(MOISTURE).orElse(0), WATER_SATURATED));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> pBuilder) {
        super.createBlockStateDefinition(pBuilder);
        pBuilder.add(TILL);
        pBuilder.add(FERTILITY);
        pBuilder.add(CONTAMINATED);
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
            return Blocks.DIRT.defaultBlockState();
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

    @Override
    public void randomTick(BlockState pState, ServerLevel pLevel, BlockPos pPos, RandomSource pRandom) {
        BlockState newState = pState;

        // Update water level
        int water = pState.getValue(WATER);
        int sprinkler = getSprinkler(pLevel, pPos);
        if (isNextToWater(pLevel, pPos)) {
            water = WATER_FLOODED;
        }
        else if (water == WATER_FLOODED) {
            water = WATER_SATURATED;
        }
        else if (sprinkler > 0 || pLevel.isRainingAt(pPos.above())) {
            water = Math.min(WATER_SATURATED, water + 1);
        }
        else if (water > 0 && pRandom.nextDouble() < cropConfig.evaporation.get()) {
            water--;
        }
        newState = newState.setValue(WATER, water);

        // Update contaminated level
        boolean contaminated = pState.getValue(CONTAMINATED);
        boolean isUnderCrops = isUnderCrops(pLevel, pPos);
        if (sprinkler == 2 || !isUnderCrops) {
            newState = newState.setValue(CONTAMINATED, false);
        }
        else if (!contaminated && Math.random()<cropConfig.start_contamination.get()) {
            newState = newState.setValue(CONTAMINATED, true);
        }
        else if (contaminated && Math.random()<cropConfig.lethality_contamination.get()) {
            IFFunctions.killPlant(pLevel, pPos.above(), 3);
            pLevel.setBlock(pPos, Blocks.DIRT.defaultBlockState(), 2);
            return;
        }
        else if (contaminated && cropConfig.spread_contamination.get()) {
            for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-1, 0, -1), pPos.offset(1, 1, 1))) {
                if (blockpos.equals(pPos))
                    continue;
                BlockState neighbour = pLevel.getBlockState(blockpos);
                if (neighbour.getBlock() instanceof Soil && !neighbour.getValue(CONTAMINATED) && isUnderCrops(pLevel, blockpos) && Math.random()<cropConfig.proximity_contamination.get()) {
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

    /**
     * Checks nearby blocks for active sprinklers
     * @return 0: no sprinkler found, 1: sprinkler found, 2: sprinkler using treated water found
     */
    private static int getSprinkler(LevelReader pLevel, BlockPos pPos) {
        ChunkAccess chunk = pLevel.getChunk(pPos);
        boolean found_sprinkler = false;
        int chunkPosX = chunk.getPos().x;
        int chunkPosZ = chunk.getPos().z;

        for (int i=chunkPosX-1; i<chunkPosX+2; i++) {
            for (int j=chunkPosZ-1; j<chunkPosZ+2; j++) {
                Set<BlockPos> entitiesPos = pLevel.getChunk(i,j).getBlockEntitiesPos();
                for (BlockPos pos : entitiesPos) {
                    if (pLevel.getBlockState(pos).getBlock() instanceof SprinklerBlock && pLevel.getBlockState(pos).getValue(SprinklerBlockEntity.ACTIVE) && distanceWater(pPos, pos)<7) {
                        found_sprinkler = true;
                        if (pLevel.getBlockState(pos).getValue(SprinklerBlockEntity.USING_PESTICIDE)) {
                            return 2;
                        }
                    }
                    else if (pLevel.getBlockState(pos).getBlock() instanceof SprinklerExtendedBlock &&  pLevel.getBlockState(pos).getValue(SprinklerBlockEntity.ACTIVE) && distanceWater(pPos, pos)<13) {
                        found_sprinkler = true;
                        if (pLevel.getBlockState(pos).getValue(SprinklerBlockEntity.USING_PESTICIDE)) {
                            return 2;
                        }
                    }
                }
            }
        }

        return found_sprinkler ? 1 : 0;
    }

    private static boolean isNextToWater(LevelReader pLevel, BlockPos pPos) {
        for (BlockPos blockpos : BlockPos.betweenClosed(pPos.offset(-1, 0, -1), pPos.offset(1, 1, 1))) {
            if (pLevel.getFluidState(blockpos).is(FluidTags.WATER)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isUnderCrops(BlockGetter pLevel, BlockPos pPos) {
        BlockState plant = pLevel.getBlockState(pPos.above());
        BlockState state = pLevel.getBlockState(pPos);
        return plant.getBlock() instanceof net.minecraftforge.common.IPlantable && state.canSustainPlant(pLevel, pPos, Direction.UP, (net.minecraftforge.common.IPlantable)plant.getBlock());
    }

    private static int distanceWater(BlockPos pos1, BlockPos pos2) {
        if (Math.abs(pos1.getY()-pos2.getY())>3) return 1000000;
        int res = Math.abs(pos1.getX()-pos2.getX());
        res = Math.max(res,Math.abs(pos1.getZ()-pos2.getZ()));
        return res;
    }

    @Override
    public boolean isFertile(BlockState state, BlockGetter level, BlockPos pos) {
        return state.getValue(WATER)>0;
    }
}
