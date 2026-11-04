package net.etylop.immersivefarming.block;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.etylop.immersivefarming.block.multiblocks.IFMultiblockBuilder;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterLogic;
import net.etylop.immersivefarming.gui.IFMenuTypes;
import net.etylop.immersivefarming.item.IFItems;

public class IFMultiblockLogic
{
    public static final MultiblockRegistration<ComposterLogic.State> COMPOSTER = metal(new ComposterLogic(), "composter")
            .structure(() -> IFMultiblocks.COMPOSTER)
            .redstone(s -> s.rsState, ComposterLogic.REDSTONE_POS)
            .gui(IFMenuTypes.COMPOSTER)
            .build();

    private static <S extends IMultiblockState>
    IFMultiblockBuilder<S> metal(IMultiblockLogic<S> logic, String name)
    {
        return new IFMultiblockBuilder<>(logic, name)
                .defaultBEs(IFBlockEntities.REGISTER)
                .defaultBlock(IFBlocks.BLOCKS, IFItems.ITEMS, IFBlocks.METAL_NO_OCCLUSION);
    }

    // Loads the class so the multiblock is added to the registers
    public static void init()
    {
    }
}
