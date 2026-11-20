package net.etylop.immersivefarming.block.multiblocks.breeder;

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
 * Block of the animal breeder. Its shapes are never a full block, so mobs would take the whole machine for open
 * ground and walk into its walls and trough: only the stalls that can take an animal in are open to them.
 */
public class BreederBlock extends MultiblockPartBlock<BreederLogic.State>
{
    private final MultiblockRegistration<BreederLogic.State> multiblock;

    public BreederBlock(Properties properties, MultiblockRegistration<BreederLogic.State> multiblock)
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
            IMultiblockBEHelper<BreederLogic.State> helper = be.getHelper().asType(multiblock);
            BreederLogic.State breeder = helper==null?null: helper.getState();
            if(breeder!=null)
            {
                int stall = BreederShapes.getStall(helper.getPositionInMB());
                if(stall >= 0&&!breeder.stalls[stall].closed)
                    return null;
            }
        }
        return BlockPathTypes.BLOCKED;
    }
}
