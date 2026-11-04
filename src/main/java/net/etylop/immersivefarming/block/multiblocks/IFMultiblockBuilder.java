package net.etylop.immersivefarming.block.multiblocks;

import blusunrize.immersiveengineering.api.multiblocks.blocks.MultiblockRegistrationBuilder;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IMultiblockComponent.StateWrapper;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl.RSState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.gui.IFMenuTypes.MultiblockContainerIF;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

public class IFMultiblockBuilder<S extends IMultiblockState>
        extends MultiblockRegistrationBuilder<S, IFMultiblockBuilder<S>>
{
    public IFMultiblockBuilder(IMultiblockLogic<S> logic, String name)
    {
        super(logic, new ResourceLocation(ImmersiveFarming.MOD_ID, name));
    }

    public IFMultiblockBuilder<S> gui(MultiblockContainerIF<S, ?> menu)
    {
        return component(new IFMultiblockGui<>(menu));
    }

    public IFMultiblockBuilder<S> redstone(StateWrapper<S, RSState> getState, BlockPos... positions)
    {
        redstoneAware();
        return selfWrappingComponent(new RedstoneControl<>(getState, positions));
    }

    @Override
    protected IFMultiblockBuilder<S> self()
    {
        return this;
    }
}
