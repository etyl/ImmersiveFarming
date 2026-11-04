package net.etylop.immersivefarming.item.custom;

import net.etylop.immersivefarming.block.custom.Soil;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.state.BlockState;

public class CompostItem extends Item {

    private static final int FERTILITY = 1;

    public CompostItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        BlockState block = pContext.getLevel().getBlockState(pContext.getClickedPos());
        if (!(block.getBlock() instanceof Soil))
            return InteractionResult.FAIL;

        if (block.getValue(Soil.FERTILITY) >= FERTILITY)
            return  InteractionResult.FAIL;

        pContext.getLevel().setBlock(pContext.getClickedPos(), block.setValue(Soil.FERTILITY, CompostItem.FERTILITY), 3);

        Player player = pContext.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            pContext.getItemInHand().shrink(1);
        return InteractionResult.CONSUME;
    }
}
