package net.etylop.immersivefarming.block.custom;

import net.etylop.immersivefarming.block.IFBlockEntities;
import net.etylop.immersivefarming.block.entity.CompostingBinBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * A 2x2 wooden composting bin: the bin itself (lower half) and a back frame holding the open lid (upper half).
 * The lower left part (as seen from the front) is the master and holds the block entity,
 * the other parts only forward interactions to it.
 */
public class CompostingBinBlock extends BaseEntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final EnumProperty<DoubleBlockHalf> HALF = BlockStateProperties.DOUBLE_BLOCK_HALF;
    public static final EnumProperty<Stage> STAGE = EnumProperty.create("stage", Stage.class);
    public static final int MAX_LEVEL = 8;
    public static final IntegerProperty LEVEL = IntegerProperty.create("level", 0, MAX_LEVEL);

    private static final VoxelShape OPEN_SHAPE = Shapes.box(0, 0, 0, 1, 14/16f, 1);
    private static final VoxelShape CLOSED_SHAPE = Shapes.block();
    // Depth of the upper half shape from the back: the posts and beam, plus the lid when it is open
    private static final double UPPER_DEPTH_OPEN = 7/16d;
    private static final double UPPER_DEPTH_CLOSED = 2/16d;

    public CompostingBinBlock(Properties props)
    {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.LEFT)
                .setValue(HALF, DoubleBlockHalf.LOWER)
                .setValue(STAGE, Stage.FILLING)
                .setValue(LEVEL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, PART, HALF, STAGE, LEVEL);
    }

    /**
     * Direction from the left part to the right part, for a bin whose front faces {@code facing}
     */
    public static Direction toRight(Direction facing)
    {
        return facing.getCounterClockWise();
    }

    /**
     * The other part of the same half (left - right)
     */
    public static BlockPos getPartnerPos(BlockPos pos, BlockState state)
    {
        Direction right = toRight(state.getValue(FACING));
        return state.getValue(PART)==Part.LEFT?pos.relative(right): pos.relative(right.getOpposite());
    }

    /**
     * The other half of the same part (lower - upper)
     */
    public static BlockPos getVerticalPartnerPos(BlockPos pos, BlockState state)
    {
        return state.getValue(HALF)==DoubleBlockHalf.LOWER?pos.above(): pos.below();
    }

    public static BlockPos getMasterPos(BlockPos pos, BlockState state)
    {
        BlockPos lower = state.getValue(HALF)==DoubleBlockHalf.LOWER?pos: pos.below();
        return state.getValue(PART)==Part.LEFT?lower: lower.relative(toRight(state.getValue(FACING)).getOpposite());
    }

    public static boolean isMaster(BlockState state)
    {
        return state.getValue(PART)==Part.LEFT&&state.getValue(HALF)==DoubleBlockHalf.LOWER;
    }

    /**
     * Positions of the 4 parts: lower left (master), lower right, upper left, upper right
     */
    public static BlockPos[] getAllParts(BlockPos masterPos, Direction facing)
    {
        BlockPos right = masterPos.relative(toRight(facing));
        return new BlockPos[]{masterPos, right, masterPos.above(), right.above()};
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        Direction facing = ctx.getHorizontalDirection().getOpposite();
        Level level = ctx.getLevel();
        BlockPos[] parts = getAllParts(ctx.getClickedPos(), facing);
        if(parts[2].getY() >= level.getMaxBuildHeight())
            return null;
        for(int i = 1; i < parts.length; i++)
            if(!level.getWorldBorder().isWithinBounds(parts[i])||!level.getBlockState(parts[i]).canBeReplaced(ctx))
                return null;
        return defaultBlockState().setValue(FACING, facing);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if(!level.isClientSide)
        {
            BlockPos[] parts = getAllParts(pos, state.getValue(FACING));
            BlockState[] states = {
                    state,
                    state.setValue(PART, Part.RIGHT),
                    state.setValue(HALF, DoubleBlockHalf.UPPER),
                    state.setValue(PART, Part.RIGHT).setValue(HALF, DoubleBlockHalf.UPPER)
            };
            // Place all the parts before any shape update, so that no part sees a missing partner
            for(int i = 1; i < parts.length; i++)
                level.setBlock(parts[i], states[i], Block.UPDATE_CLIENTS|Block.UPDATE_KNOWN_SHAPE);
            for(int i = 0; i < parts.length; i++)
            {
                level.blockUpdated(parts[i], states[i].getBlock());
                states[i].updateNeighbourShapes(level, parts[i], Block.UPDATE_ALL);
            }
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        // A part without its partners disappears (without drops, the item is dropped by the master part)
        if(neighborPos.equals(getPartnerPos(pos, state)))
        {
            if(!neighborState.is(this)||neighborState.getValue(FACING)!=state.getValue(FACING)
                    ||neighborState.getValue(PART)==state.getValue(PART)||neighborState.getValue(HALF)!=state.getValue(HALF))
                return Blocks.AIR.defaultBlockState();
        }
        else if(neighborPos.equals(getVerticalPartnerPos(pos, state)))
        {
            if(!neighborState.is(this)||neighborState.getValue(FACING)!=state.getValue(FACING)
                    ||neighborState.getValue(PART)!=state.getValue(PART)||neighborState.getValue(HALF)==state.getValue(HALF))
                return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        // The item is dropped by the master part: when another part is broken, break the master too
        if(!level.isClientSide&&!isMaster(state))
        {
            BlockPos masterPos = getMasterPos(pos, state);
            BlockState masterState = level.getBlockState(masterPos);
            if(masterState.is(this)&&isMaster(masterState))
                level.destroyBlock(masterPos, !player.isCreative(), player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof CompostingBinBlockEntity bin)
            bin.dropContents();
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        if(level.getBlockEntity(getMasterPos(pos, state)) instanceof CompostingBinBlockEntity bin)
            return bin.interact(player, hand);
        return InteractionResult.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        boolean closed = state.getValue(STAGE)==Stage.COMPOSTING;
        if(state.getValue(HALF)==DoubleBlockHalf.UPPER)
            return backStrip(state.getValue(FACING), closed?UPPER_DEPTH_CLOSED: UPPER_DEPTH_OPEN);
        return closed?CLOSED_SHAPE: OPEN_SHAPE;
    }

    /**
     * Full height box along the back side of the block (opposite to the front)
     */
    private static VoxelShape backStrip(Direction facing, double depth)
    {
        return switch(facing)
        {
            case SOUTH -> Shapes.box(0, 0, 0, 1, 1, depth);
            case EAST -> Shapes.box(0, 0, 0, depth, 1, 1);
            case WEST -> Shapes.box(1-depth, 0, 0, 1, 1, 1);
            default -> Shapes.box(0, 0, 1-depth, 1, 1, 1);
        };
    }

    @Override
    public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random)
    {
        if(state.getValue(HALF)!=DoubleBlockHalf.LOWER)
            return;
        Stage stage = state.getValue(STAGE);
        double x = pos.getX()+0.1+0.8*random.nextDouble();
        double z = pos.getZ()+0.1+0.8*random.nextDouble();
        if(stage==Stage.COMPOSTING)
        {
            // Steam escaping from the lid
            if(random.nextInt(3)==0)
                level.addParticle(ParticleTypes.SMOKE, x, pos.getY()+1.0, z, 0, 0.02, 0);
            if(random.nextInt(6)==0)
                level.addParticle(ParticleTypes.COMPOSTER, x, pos.getY()+1.0, z, 0, 0, 0);
        }
        else if(stage==Stage.TURNING&&random.nextInt(4)==0)
            level.addParticle(ParticleTypes.MYCELIUM, x, pos.getY()+0.9, z, 0, 0, 0);
        else if(stage==Stage.READY&&random.nextInt(5)==0)
            level.addParticle(ParticleTypes.HAPPY_VILLAGER, x, pos.getY()+0.9, z, 0, 0, 0);
    }

    @SuppressWarnings("deprecation")
    @Override
    public RenderShape getRenderShape(BlockState state)
    {
        return RenderShape.MODEL;
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState rotate(BlockState state, Rotation rotation)
    {
        return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
    }

    @SuppressWarnings("deprecation")
    @Override
    public BlockState mirror(BlockState state, Mirror mirror)
    {
        return state.rotate(mirror.getRotation(state.getValue(FACING)));
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return isMaster(state)?new CompostingBinBlockEntity(pos, state): null;
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        if(level.isClientSide||!isMaster(state))
            return null;
        return createTickerHelper(type, IFBlockEntities.COMPOSTING_BIN.get(), (lvl, pos, st, be) -> be.tickServer());
    }

    public enum Part implements StringRepresentable
    {
        LEFT, RIGHT;

        @Override
        public String getSerializedName()
        {
            return name().toLowerCase();
        }
    }

    public enum Stage implements StringRepresentable
    {
        FILLING, COMPOSTING, TURNING, READY;

        @Override
        public String getSerializedName()
        {
            return name().toLowerCase();
        }
    }
}
