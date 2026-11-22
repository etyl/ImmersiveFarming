package net.etylop.immersivefarming.block;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockItem;
import net.etylop.immersivefarming.block.multiblocks.IFMultiblockBuilder;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederBlock;
import net.etylop.immersivefarming.block.multiblocks.breeder.BreederLogic;
import net.etylop.immersivefarming.block.multiblocks.composter.ComposterLogic;
import net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenBlock;
import net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenLogic;
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
            .customBlock(IFBlocks.BLOCKS, IFItems.ITEMS, reg -> new BreederBlock(IFBlocks.METAL_MULTIBLOCK, reg), MultiblockItem::new)
            .postProcessesShape()
            .structure(() -> IFMultiblocks.BREEDER)
            .build();

    // Not mirrored: its hopper and its power socket are always on the same sides. Its gate is part of its shapes
    public static final MultiblockRegistration<AnimalPenLogic.State> ANIMAL_PEN = new IFMultiblockBuilder<>(new AnimalPenLogic(), "animal_pen")
            .defaultBEs(IFBlockEntities.REGISTER)
            .notMirrored()
            .customBlock(IFBlocks.BLOCKS, IFItems.ITEMS, reg -> new AnimalPenBlock(IFBlocks.METAL_MULTIBLOCK, reg), MultiblockItem::new)
            .postProcessesShape()
            .redstone(s -> s.rsState, AnimalPenLogic.REDSTONE_POS)
            .structure(() -> IFMultiblocks.ANIMAL_PEN)
            .build();

    private static <S extends IMultiblockState>
    IFMultiblockBuilder<S> metal(IMultiblockLogic<S> logic, String name)
    {
        return new IFMultiblockBuilder<>(logic, name)
                .defaultBEs(IFBlockEntities.REGISTER)
                .defaultBlock(IFBlocks.BLOCKS, IFItems.ITEMS, IFBlocks.METAL_MULTIBLOCK);
    }

    // Loads the class so the multiblock is added to the registers
    public static void init()
    {
    }
}
