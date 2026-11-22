package net.etylop.immersivefarming.block.multiblocks.animalpen;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.etylop.immersivefarming.block.multiblocks.IFTemplateMultiblock;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

/**
 * Structure of the animal pen: 3 wide, 2 high, 3 deep. It is formed from the trough side (the front for the
 * player), with the hammer on the trough; the gate is on the opposite side. The master is the block of the stall
 * next to the trough.
 */
public class AnimalPenMultiblock extends IFTemplateMultiblock
{
    public static final BlockPos MASTER = new BlockPos(1, 0, 1);
    private static final BlockPos TRIGGER = new BlockPos(1, 0, 2);

    /**
     * Objects of the model that move, drawn by {@link AnimalPenRenderer} and hidden in the block model:
     * the gate and the food in the trough
     */
    public static final String GATE = "gate";
    public static final String FEED = "feed";

    public AnimalPenMultiblock()
    {
        super(new ResourceLocation(ImmersiveFarming.MOD_ID, "multiblocks/animal_pen"),
                MASTER,
                TRIGGER,
                new BlockPos(3, 2, 3),
                IFMultiblockLogic.ANIMAL_PEN);
    }

    public static List<String> getMovingParts()
    {
        return List.of(GATE, FEED);
    }

    // The output is always on the left of the trough, the control box and the power plug on its right
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
