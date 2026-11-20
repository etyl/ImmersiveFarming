package net.etylop.immersivefarming.block;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockItem;
import net.etylop.immersivefarming.block.multiblocks.IFMultiblockBuilder;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederBlock;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederLogic;
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

    // Not mirrored: both halves are the same. Its gates are part of its shapes, which therefore depend on its state
    public static final MultiblockRegistration<BreederLogic.State> BREEDER = new IFMultiblockBuilder<>(new BreederLogic(), "breeder")
            .defaultBEs(IFBlockEntities.REGISTER)
            .notMirrored()
            .customBlock(IFBlocks.BLOCKS, IFItems.ITEMS, reg -> new BreederBlock(IFBlocks.METAL_NO_OCCLUSION, reg), MultiblockItem::new)
            .postProcessesShape()
            .structure(() -> IFMultiblocks.BREEDER)
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
