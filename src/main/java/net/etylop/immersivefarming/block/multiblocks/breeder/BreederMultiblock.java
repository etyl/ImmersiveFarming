package net.etylop.immersivefarming.block.multiblocks.breeder;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.etylop.immersivefarming.block.multiblocks.IFTemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;

import java.util.ArrayList;
import java.util.List;

/**
 * Structure of the animal breeder: 4 wide, 2 high, 3 deep. It is formed from the trough side (the front for the
 * player); the gates are on the opposite side. The master is one of the two blocks between the gates.
 */
public class BreederMultiblock extends IFTemplateMultiblock
{
    public static final BlockPos MASTER = new BlockPos(1, 0, 0);
    /** One of the blocks of the trough */
    private static final BlockPos TRIGGER = new BlockPos(1, 0, 2);
    private static final int WIDTH = 4;

    /**
     * Objects of the model that move, drawn by {@link BreederRenderer} and hidden in the block model:
     * the two gates, the curtain of the hatch and the food in the trough
     */
    public static final String[] GATES = {"gate_left", "gate_right"};
    public static final String CURTAIN = "curtain";
    public static final String FEED = "feed";

    public BreederMultiblock()
    {
        super(new ResourceLocation(ImmersiveFarming.MOD_ID, "multiblocks/breeder"),
                MASTER,
                TRIGGER,
                new BlockPos(WIDTH, 2, 3),
                IFMultiblockLogic.BREEDER);
    }

    /**
     * The structure is formed with the hammer on its trough, which has no middle block. Any of its blocks will do:
     * the structure is looked for with each of them as the one that was hit.
     */
    @Override
    public boolean createStructure(Level world, BlockPos pos, Direction side, Player player)
    {
        if(!side.getAxis().isHorizontal())
            return false;
        // The player faces the trough, whose blocks run from left to right
        Direction right = side.getOpposite().getClockWise();
        for(int x = 0; x < WIDTH; x++)
            if(super.createStructure(world, pos.relative(right, TRIGGER.getX()-x), side, player))
                return true;
        return false;
    }

    public static List<String> getMovingParts()
    {
        List<String> parts = new ArrayList<>(List.of(GATES));
        parts.add(CURTAIN);
        parts.add(FEED);
        return parts;
    }

    // Both halves are the same
    @Override
    public boolean canBeMirrored()
    {
        return false;
    }

    @Override
    public float getManualScale()
    {
        return 16;
    }
}
