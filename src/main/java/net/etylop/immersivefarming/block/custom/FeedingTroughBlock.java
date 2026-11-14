package net.etylop.immersivefarming.block.custom;

import net.etylop.immersivefarming.block.entity.FeedingTroughBlockEntity;
import net.etylop.immersivefarming.entity.Livestock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
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
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.pathfinder.PathComputationType;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.Map;

/**
 * A wooden feeding trough, two blocks long, holding several kinds of food in a single inventory shared by
 * both halves (see {@link FeedingTroughBlockEntity}). Hungry baby animals come and eat from it.
 * It is built as a piece of fence, to be placed in the fence of a pen: fences connect to its ends,
 * and animals can't jump over it.
 * FACING points from a half towards the other half, so both halves use the same model, rotated.
 * The main half (the one placed by the player) drops the item.
 */
public class FeedingTroughBlock extends BaseEntityBlock
{
    public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
    public static final EnumProperty<Part> PART = EnumProperty.create("part", Part.class);
    public static final int MAX_FILL = 4;
    public static final IntegerProperty FILL = IntegerProperty.create("fill", 0, MAX_FILL);

    /*
     * The trough is a piece of fence: a manger between two fence posts (one at the outer end of each half),
     * joined by a rail at the height of the fences' upper bar. Shapes are given for a half whose partner is to the north,
     * and rotated like the model.
     */
    // Outline: trough, post and rail
    private static final Map<Direction, VoxelShape> SHAPES = shapes(
            new double[]{2, 0, 0, 14, 8, 12},
            new double[]{6, 0, 12, 10, 16, 16},
            new double[]{7, 12, 0, 9, 15, 12}
    );
    // Collision: 1.5 blocks high, as fences, so that animals can't jump over
    private static final Map<Direction, VoxelShape> COLLISION_SHAPES = shapes(new double[]{2, 0, 0, 14, 24, 16});
    // Only the outer end is a full face: fences, walls and panes connect to the ends of the trough, not to its sides
    private static final Map<Direction, VoxelShape> SUPPORT_SHAPES = shapes(new double[]{0, 0, 15, 16, 16, 16});

    public FeedingTroughBlock(Properties props)
    {
        super(props);
        registerDefaultState(stateDefinition.any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, Part.MAIN)
                .setValue(FILL, 0));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(FACING, PART, FILL);
    }

    public static BlockPos getPartnerPos(BlockPos pos, BlockState state)
    {
        return pos.relative(state.getValue(FACING));
    }

    public static BlockPos getMainPos(BlockPos pos, BlockState state)
    {
        return state.getValue(PART)==Part.MAIN?pos: getPartnerPos(pos, state);
    }

    @Nullable
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext ctx)
    {
        // Lies across the player's view, extending to their right (as the composting bin)
        Direction toPartner = ctx.getHorizontalDirection().getClockWise();
        BlockPos partner = ctx.getClickedPos().relative(toPartner);
        Level level = ctx.getLevel();
        if(!level.getWorldBorder().isWithinBounds(partner)||!level.getBlockState(partner).canBeReplaced(ctx))
            return null;
        return defaultBlockState().setValue(FACING, toPartner);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity placer, ItemStack stack)
    {
        super.setPlacedBy(level, pos, state, placer, stack);
        if(!level.isClientSide)
        {
            BlockPos partnerPos = getPartnerPos(pos, state);
            level.setBlock(partnerPos, state.setValue(FACING, state.getValue(FACING).getOpposite()).setValue(PART, Part.SECONDARY), Block.UPDATE_ALL);
            level.blockUpdated(pos, Blocks.AIR);
            state.updateNeighbourShapes(level, pos, Block.UPDATE_ALL);
        }
    }

    @Override
    public BlockState updateShape(BlockState state, Direction dir, BlockState neighborState, LevelAccessor level, BlockPos pos, BlockPos neighborPos)
    {
        // A half without its partner disappears (without drops, the item is dropped by the main half)
        if(dir==state.getValue(FACING)&&(!neighborState.is(this)
                ||neighborState.getValue(FACING)!=dir.getOpposite()||neighborState.getValue(PART)==state.getValue(PART)))
            return Blocks.AIR.defaultBlockState();
        return super.updateShape(state, dir, neighborState, level, pos, neighborPos);
    }

    @Override
    public void playerWillDestroy(Level level, BlockPos pos, BlockState state, Player player)
    {
        // The item is dropped by the main half: when the other half is broken, break the main half too
        if(!level.isClientSide&&state.getValue(PART)!=Part.MAIN)
        {
            BlockPos mainPos = getMainPos(pos, state);
            BlockState mainState = level.getBlockState(mainPos);
            if(mainState.is(this)&&mainState.getValue(PART)==Part.MAIN)
                level.destroyBlock(mainPos, !player.isCreative(), player);
        }
        super.playerWillDestroy(level, pos, state, player);
    }

    @SuppressWarnings("deprecation")
    @Override
    public void onRemove(BlockState state, Level level, BlockPos pos, BlockState newState, boolean isMoving)
    {
        if(!state.is(newState.getBlock())&&level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough)
            trough.dropContents();
        super.onRemove(state, level, pos, newState, isMoving);
    }

    @SuppressWarnings("deprecation")
    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        // Let blocks be placed against the trough (decided on both sides, so the client predicts the placement)
        ItemStack held = player.getItemInHand(hand);
        if(held.getItem() instanceof BlockItem&&!Livestock.isFeed(held, level))
            return InteractionResult.PASS;
        if(level.isClientSide)
            return InteractionResult.SUCCESS;
        // Both halves share the inventory of the main half
        if(level.getBlockEntity(pos) instanceof FeedingTroughBlockEntity trough)
        {
            FeedingTroughBlockEntity main = trough.getMain();
            if(main!=null)
                return main.interact(player, hand);
        }
        return InteractionResult.PASS;
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return SHAPES.get(state.getValue(FACING));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext ctx)
    {
        return COLLISION_SHAPES.get(state.getValue(FACING));
    }

    @SuppressWarnings("deprecation")
    @Override
    public VoxelShape getBlockSupportShape(BlockState state, BlockGetter level, BlockPos pos)
    {
        return SUPPORT_SHAPES.get(state.getValue(FACING));
    }

    /**
     * The union of these boxes (in pixels, for a half whose partner is to the north), for each facing
     */
    private static Map<Direction, VoxelShape> shapes(double[]... boxes)
    {
        Map<Direction, VoxelShape> shapes = new EnumMap<>(Direction.class);
        for(Direction facing : Direction.Plane.HORIZONTAL)
        {
            VoxelShape shape = Shapes.empty();
            for(double[] box : boxes)
                shape = Shapes.or(shape, rotatedBox(facing, box[0], box[1], box[2], box[3], box[4], box[5]));
            shapes.put(facing, shape.optimize());
        }
        return shapes;
    }

    /** Same rotation as the blockstate's y rotation (clockwise seen from above: north to east) */
    private static VoxelShape rotatedBox(Direction facing, double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return switch(facing)
        {
            case EAST -> Block.box(16-z2, y1, x1, 16-z1, y2, x2);
            case SOUTH -> Block.box(16-x2, y1, 16-z2, 16-x1, y2, 16-z1);
            case WEST -> Block.box(z1, y1, 16-x2, z2, y2, 16-x1);
            default -> Block.box(x1, y1, z1, x2, y2, z2);
        };
    }

    // Not walkable like a slab: mobs walk around the trough instead of planning paths through it
    @SuppressWarnings("deprecation")
    @Override
    public boolean isPathfindable(BlockState state, BlockGetter level, BlockPos pos, PathComputationType type)
    {
        return false;
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
        return new FeedingTroughBlockEntity(pos, state);
    }

    public enum Part implements StringRepresentable
    {
        MAIN, SECONDARY;

        @Override
        public String getSerializedName()
        {
            return name().toLowerCase();
        }
    }
}
