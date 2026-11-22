package net.etylop.immersivefarming.block.multiblocks.animalpen;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockBEHelper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockBE;
import blusunrize.immersiveengineering.api.multiblocks.blocks.registry.MultiblockPartBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.pathfinder.BlockPathTypes;
import org.jetbrains.annotations.Nullable;

/**
 * Block of the animal pen. Its shapes are never a full block, so mobs would take the whole machine for open
 * ground and walk into its walls: only the stall is open to them, while its gate is.
 */
public class AnimalPenBlock extends MultiblockPartBlock<AnimalPenLogic.State>
{
    private final MultiblockRegistration<AnimalPenLogic.State> multiblock;

    public AnimalPenBlock(Properties properties, MultiblockRegistration<AnimalPenLogic.State> multiblock)
    {
        super(properties, multiblock);
        this.multiblock = multiblock;
    }

    @Override
    @Nullable
    public BlockPathTypes getBlockPathType(BlockState state, BlockGetter level, BlockPos pos, @Nullable Mob mob)
    {
        if(level.getBlockEntity(pos) instanceof IMultiblockBE<?> be)
        {
            IMultiblockBEHelper<AnimalPenLogic.State> helper = be.getHelper().asType(multiblock);
            AnimalPenLogic.State pen = helper==null?null: helper.getState();
            if(pen!=null&&AnimalPenShapes.isStall(helper.getPositionInMB())&&!pen.stall.closed)
                return null;
        }
        return BlockPathTypes.BLOCKED;
    }
}
