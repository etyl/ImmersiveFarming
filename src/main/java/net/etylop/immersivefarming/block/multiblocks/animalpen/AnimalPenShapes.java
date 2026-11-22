package net.etylop.immersivefarming.block.multiblocks.animalpen;

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
 * Shapes of the animal pen, in pixels and in multiblock coordinates: x 0..48 from left to right, y 0..32,
 * z from 0 (back, the gate) to 48 (front, the trough). The shape of each block is what the boxes leave inside it.
 * <p>They are kept to a few boxes, not those of the model (see tools/gen_animal_pen.py): the outer walls, the
 * two bays on both sides of the stall and their coils, the beams at the top of the frame, the floor, and the front
 * row: the trough, between the output box and the control box, and the feed input above the output box. The power
 * plug stands in the right bay, behind its coil.
 * <p>The gate is not part of the fixed shapes: it depends on the state of the multiblock, and is added by
 * {@link AnimalPenLogic#postProcessAbsoluteShape}.
 */
public final class AnimalPenShapes
{
    /** The stall, in the middle: between the two bays of the coils */
    public static final int STALL_MIN_X = 14;
    public static final int STALL_MAX_X = 34;
    /** Inner face of the closed gate */
    public static final int STALL_MIN_Z = 2;
    /** Stall side of the front row: of the trough, and of the two boxes at its ends */
    public static final int STALL_MAX_Z = 32;
    /**
     * The trough, in front of the stall: from the output box (left) to the control box (right). It is not as deep
     * as they are, and is in the middle of its block
     */
    public static final int TROUGH_MIN_X = 16;
    public static final int TROUGH_MAX_X = 32;
    private static final int TROUGH_MIN_Z = 34;
    private static final int TROUGH_MAX_Z = 46;
    public static final int TROUGH_HEIGHT = 12;
    /** The two boxes are a block each */
    public static final int BOX_WIDTH = 16;
    private static final int BOX_HEIGHT = 16;
    /** A closed gate is as high as a fence (animals don't jump over it), an open one leaves this much room below */
    public static final int GATE_CLEARANCE = 24;
    /** What an animal brought by a conveyor stands on: the bars of an open gate leave it through */
    private static final int CONVEYOR_HEIGHT = 3;
    /** From the lowest to the highest bar of a closed gate, which stays within the lower block */
    private static final int BARS_MIN_Y = 2;
    private static final int BARS_MAX_Y = 16;
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
        // Outer walls, up to the front row
        both(px(0, 0, 0, 1, 32, STALL_MAX_Z));
        both(px(47, 0, 0, 48, 32, STALL_MAX_Z));
        for(int x : new int[]{0, STALL_MAX_X})
        {
            // Back of the bays, on both sides of the gate...
            both(px(x, 0, 0, x+14, 32, STALL_MIN_Z));
            // ... and the beams above them
            both(px(x, BEAMS_MIN_Y, 0, x+14, 32, BEAMS_WIDTH));
        }
        // The beam above the gate, and the one at the front of the frame
        both(px(STALL_MIN_X, BEAMS_MIN_Y, STALL_MIN_Z, STALL_MAX_X, 32, BEAMS_WIDTH));
        both(px(0, BEAMS_MIN_Y, STALL_MAX_Z, 48, 32, STALL_MAX_Z+2));
        // Between the stall and each bay: rails, which the animal does not get through...
        both(px(STALL_MIN_X-1, 0, STALL_MIN_Z, STALL_MIN_X, GATE_CLEARANCE, STALL_MAX_Z));
        both(px(STALL_MAX_X, 0, STALL_MIN_Z, STALL_MAX_X+1, GATE_CLEARANCE, STALL_MAX_Z));
        // ... and the beams above them
        both(px(STALL_MIN_X-2, BEAMS_MIN_Y, BEAMS_WIDTH, STALL_MIN_X, 32, STALL_MAX_Z));
        both(px(STALL_MAX_X, BEAMS_MIN_Y, BEAMS_WIDTH, STALL_MAX_X+2, 32, STALL_MAX_Z));
        // The coils, in their bays: each in the middle of its block
        both(px(3, 0, 19, 13, 24, 29));
        both(px(35, 0, 19, 45, 24, 29));
        // Floor
        both(px(1, 0, 0, 47, 1, STALL_MAX_Z));
        // Front row: the output box, the trough (and the floor under it), and the control box
        both(px(0, 0, STALL_MAX_Z, BOX_WIDTH, BOX_HEIGHT, 48));
        both(px(TROUGH_MIN_X, 0, TROUGH_MIN_Z, TROUGH_MAX_X, TROUGH_HEIGHT, TROUGH_MAX_Z));
        both(px(TROUGH_MIN_X, 0, STALL_MAX_Z, TROUGH_MAX_X, 1, 48));
        both(px(48-BOX_WIDTH, 0, STALL_MAX_Z, 48, BOX_HEIGHT, 48));
        // The feed input: a chute that stands on the output box, up to the top of the machine
        both(px(3, BOX_HEIGHT, 35, 13, 32, 45));
        // The power plug: a column in the right bay, behind its coil, in the middle of its block
        both(px(36, 0, 4, 44, 32, 12));
        // The rails that close the front of the bays, above the two boxes: the stall is open above the trough
        both(px(2, 17, STALL_MAX_Z, STALL_MIN_X, 25, STALL_MAX_Z+1));
        both(px(STALL_MAX_X, 17, STALL_MAX_Z, 46, 25, STALL_MAX_Z+1));
        // The animal does not get past the trough, or over it, as it does not get over a fence. Nothing to aim at there
        COLLISION.add(px(STALL_MIN_X, 0, STALL_MAX_Z, STALL_MAX_X, GATE_CLEARANCE, STALL_MAX_Z+1));
    }

    private AnimalPenShapes()
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

    /** Whether this block is part of the stall: the middle column, up to the trough */
    public static boolean isStall(BlockPos posInMultiblock)
    {
        return posInMultiblock.getX()==1&&posInMultiblock.getZ() < 2;
    }

    /**
     * Whether this block is one of the gate: any block of the back row. A click on the block closes or opens the
     * gate, whose shape is in the block (the gate is wider than a block: it ends in the blocks of the bays).
     */
    public static boolean isGate(BlockPos posInMultiblock)
    {
        return posInMultiblock.getZ()==0;
    }

    /**
     * Shape of the gate in one of its blocks, rotated as the multiblock is: its bars, which bar the doorway when
     * it is closed and are stowed at the top when it is open. A closed gate stops entities from the ground up
     * to the height of a fence.
     */
    public static VoxelShape getGateShape(BlockPos posInMultiblock, boolean closed, MultiblockOrientation orientation, ShapeType type)
    {
        boolean collision = type==ShapeType.COLLISION;
        return GATE_SHAPES.computeIfAbsent(new GateKey(posInMultiblock.immutable(), closed, collision, orientation), key -> {
            if(!isGate(key.pos))
                return Shapes.empty();
            AABB gate;
            if(!key.closed)
                gate = px(STALL_MIN_X, GATE_CLEARANCE+(key.collision?CONVEYOR_HEIGHT: 0), 0, STALL_MAX_X, 32, STALL_MIN_Z);
            else if(key.collision)
                gate = px(STALL_MIN_X, 0, 0, STALL_MAX_X, GATE_CLEARANCE, STALL_MIN_Z);
            else
                gate = px(STALL_MIN_X, BARS_MIN_Y, 0, STALL_MAX_X, BARS_MAX_Y, STALL_MIN_Z);
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
