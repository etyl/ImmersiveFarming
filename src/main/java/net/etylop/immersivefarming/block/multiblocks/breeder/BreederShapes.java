package net.etylop.immersivefarming.block.multiblocks.breeder;

import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockOrientation;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Shapes of the animal breeder, in pixels and in multiblock coordinates: x 0..64 from left to right, y 0..32,
 * z from 0 (back, the gates) to 48 (front, the trough). The shape of each block is what the boxes leave inside it.
 * <p>They are kept to a few boxes, not those of the model (see tools/gen_breeder.py): the two outer walls and the
 * pillars they end with at the front, the beams at the top of the frame and the posts that carry them, the floor
 * of the two stalls, the middle section and the trough.
 * <p>The gates are not part of the fixed shapes: they depend on the state of the multiblock, and are added by
 * {@link BreederLogic#postProcessAbsoluteShape}.
 */
public final class BreederShapes
{
    /** Inner faces of the outer walls, and both sides of the middle section between the stalls */
    public static final int[] STALL_MIN_X = {1, 44};
    public static final int[] STALL_MAX_X = {20, 63};
    /** Inner face of a closed gate */
    public static final int STALL_MIN_Z = 2;
    /** Stall side of the trough, which is in the middle of its blocks: right under the feed input */
    public static final int STALL_MAX_Z = 35;
    /** Front of the trough */
    private static final int TROUGH_MAX_Z = 45;
    /** A closed gate is as high as a fence (animals don't jump over it), an open one leaves this much room below */
    public static final int GATE_CLEARANCE = 24;
    /** What an animal brought by a conveyor stands on: the bars of an open gate leave it through */
    private static final int CONVEYOR_HEIGHT = 3;
    /** Top of the hatch of the newborns, at the back of the middle section: its strip curtain hangs from there */
    public static final int HATCH_HEIGHT = 15;
    /** From the lowest to the highest bar of a closed gate, which stays within the lower block */
    private static final int BARS_MIN_Y = 2;
    private static final int BARS_MAX_Y = 16;
    /** Top of the middle section */
    private static final int MIDDLE_HEIGHT = 20;
    /**
     * The block of the feed input, at the top of the machine above the trough: the left one of the two in the
     * middle. Its chute is in the middle of this block
     */
    public static final int INPUT_MIN_X = 16, INPUT_MAX_X = 32;
    public static final int INPUT_MIN_Z = 32, INPUT_MAX_Z = 48;
    /** Its chute ends right above the trough */
    private static final int CHUTE_MIN_Y = 14;
    /** Underside of the beams at the top of the frame, and how deep those of the back are */
    private static final int BEAMS_MIN_Y = 30;
    private static final int BEAMS_WIDTH = 5;

    /** What the player aims at, and what stops entities */
    private static final List<AABB> OUTLINE = new ArrayList<>();
    private static final List<AABB> COLLISION = new ArrayList<>();
    private static final Map<BlockPos, VoxelShape> OUTLINE_SHAPES = new ConcurrentHashMap<>();
    private static final Map<BlockPos, VoxelShape> COLLISION_SHAPES = new ConcurrentHashMap<>();
    private static final Map<GateKey, VoxelShape> GATE_SHAPES = new ConcurrentHashMap<>();

    static
    {
        // Outer walls, which end where the trough starts...
        both(px(0, 0, 0, 1, 32, STALL_MAX_Z));
        both(px(63, 0, 0, 64, 32, STALL_MAX_Z));
        // ... with the front pillars, at both ends of the trough
        both(px(0, 0, STALL_MAX_Z, 2, 32, STALL_MAX_Z+3));
        both(px(62, 0, STALL_MAX_Z, 64, 32, STALL_MAX_Z+3));
        // Top of the frame: the beams at the back and at the front...
        both(px(0, BEAMS_MIN_Y, 0, 64, 32, BEAMS_WIDTH));
        both(px(0, BEAMS_MIN_Y, STALL_MAX_Z, 64, 32, STALL_MAX_Z+3));
        for(int x : new int[]{0, 20, 42, 62})
        {
            // ... those that join them, above the outer walls and both sides of the middle section...
            both(px(x, BEAMS_MIN_Y, BEAMS_WIDTH, x+2, 32, STALL_MAX_Z));
            // ... and the posts that carry them, standing on the middle section
            if(x!=0&&x!=62)
            {
                both(px(x, MIDDLE_HEIGHT, 0, x+2, BEAMS_MIN_Y, BEAMS_WIDTH));
                both(px(x, MIDDLE_HEIGHT, STALL_MAX_Z-3, x+2, BEAMS_MIN_Y, STALL_MAX_Z));
            }
        }
        // Floor of the stalls
        for(int stall = 0; stall < 2; stall++)
            both(px(STALL_MIN_X[stall], 0, 0, STALL_MAX_X[stall], 1, STALL_MAX_Z));
        // Trough
        both(px(1, 0, STALL_MAX_Z, 63, 12, TROUGH_MAX_Z));

        // Middle section, hatch included: the newborn is put down behind it
        both(px(20, 0, 0, 44, MIDDLE_HEIGHT, STALL_MAX_Z));
        // The feed input, above the trough: a chute that goes down from the top of the frame, along the front of
        // the middle section, to the trough
        both(px(INPUT_MIN_X+3, CHUTE_MIN_Y, STALL_MAX_Z, INPUT_MAX_X-3, 32, TROUGH_MAX_Z));
        // The animals don't get over the trough, as they don't get over a fence. Nothing to aim at there:
        // the animals can be reached from the trough side
        COLLISION.add(px(1, 12, STALL_MAX_Z, 63, GATE_CLEARANCE, STALL_MAX_Z+1));
    }

    private BreederShapes()
    {
    }

    private static void both(AABB box)
    {
        OUTLINE.add(box);
        COLLISION.add(box);
    }

    /** A box given in pixels */
    public static AABB px(double x1, double y1, double z1, double x2, double y2, double z2)
    {
        return new AABB(x1/16, y1/16, z1/16, x2/16, y2/16, z2/16);
    }

    /** Fixed shape of a block of the multiblock (not rotated), relative to this block */
    public static VoxelShape getShape(BlockPos posInMultiblock, ShapeType type)
    {
        if(type==ShapeType.COLLISION)
            return COLLISION_SHAPES.computeIfAbsent(posInMultiblock.immutable(), pos -> cut(COLLISION, pos));
        return OUTLINE_SHAPES.computeIfAbsent(posInMultiblock.immutable(), pos -> cut(OUTLINE, pos));
    }

    /** The stall of this block: the left and right columns, up to the trough. -1 for the other blocks */
    public static int getStall(BlockPos posInMultiblock)
    {
        if(posInMultiblock.getZ() >= 2)
            return -1;
        return posInMultiblock.getX()==0?0: posInMultiblock.getX()==3?1: -1;
    }

    /**
     * The gate of this block: any block of the back row, the left half of the machine for the left gate.
     * -1 for the other blocks. A click on the block closes or opens this gate, whose shape is in the block
     * (a gate is wider than a block: it ends in the block of the middle section next to it).
     */
    public static int getGate(BlockPos posInMultiblock)
    {
        if(posInMultiblock.getZ()!=0)
            return -1;
        return posInMultiblock.getX() <= 1?0: 1;
    }

    /** A block of each gate, for its sounds */
    public static BlockPos getGatePos(int stall)
    {
        return new BlockPos(stall==0?0: 3, 0, 0);
    }

    /**
     * Shape of a gate in one of its blocks, rotated as the multiblock is: its bars, which bar the doorway when
     * it is closed and are stowed at the top when it is open. A closed gate stops entities from the ground up
     * to the height of a fence.
     */
    public static VoxelShape getGateShape(BlockPos posInMultiblock, boolean closed, MultiblockOrientation orientation, ShapeType type)
    {
        boolean collision = type==ShapeType.COLLISION;
        return GATE_SHAPES.computeIfAbsent(new GateKey(posInMultiblock.immutable(), closed, collision, orientation), key -> {
            int stall = getGate(key.pos);
            if(stall < 0)
                return Shapes.empty();
            AABB gate;
            if(!key.closed)
                gate = px(STALL_MIN_X[stall], GATE_CLEARANCE+(key.collision?CONVEYOR_HEIGHT: 0), 0, STALL_MAX_X[stall], 32, STALL_MIN_Z);
            else if(key.collision)
                gate = px(STALL_MIN_X[stall], 0, 0, STALL_MAX_X[stall], GATE_CLEARANCE, STALL_MIN_Z);
            else
                gate = px(STALL_MIN_X[stall], BARS_MIN_Y, 0, STALL_MAX_X[stall], BARS_MAX_Y, STALL_MIN_Z);
            return key.orientation.transformRelativeShape(cut(List.of(gate), key.pos));
        });
    }

    /** What the boxes leave in a block, relative to this block */
    private static VoxelShape cut(List<AABB> boxes, BlockPos pos)
    {
        AABB block = new AABB(pos);
        VoxelShape shape = Shapes.empty();
        for(AABB box : boxes)
        {
            if(!box.intersects(block))
                continue;
            AABB part = box.intersect(block).move(-pos.getX(), -pos.getY(), -pos.getZ());
            shape = Shapes.joinUnoptimized(shape, Shapes.create(part), BooleanOp.OR);
        }
        return shape.optimize();
    }

    private record GateKey(BlockPos pos, boolean closed, boolean collision, MultiblockOrientation orientation)
    {
    }
}
