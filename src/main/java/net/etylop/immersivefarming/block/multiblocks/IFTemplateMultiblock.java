package net.etylop.immersivefarming.block.multiblocks;

import blusunrize.immersiveengineering.api.multiblocks.ClientMultiblocks;
import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistration;
import blusunrize.immersiveengineering.common.blocks.multiblocks.IETemplateMultiblock;
import net.etylop.immersivefarming.utils.IFBasicClientProperties;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.function.Consumer;

public abstract class IFTemplateMultiblock extends IETemplateMultiblock
{
    public IFTemplateMultiblock(
            ResourceLocation loc, BlockPos masterFromOrigin, BlockPos triggerFromOrigin, BlockPos size,
            MultiblockRegistration<?> logic
    )
    {
        super(loc, masterFromOrigin, triggerFromOrigin, size, logic);
    }

    public BlockPos multiblockToModelPos(BlockPos posInMultiblock)
    {
        return posInMultiblock.subtract(masterFromOrigin);
    }

    @Override
    public void initializeClient(Consumer<ClientMultiblocks.MultiblockManualData> consumer)
    {
        consumer.accept(new IFBasicClientProperties(this));
    }
}
