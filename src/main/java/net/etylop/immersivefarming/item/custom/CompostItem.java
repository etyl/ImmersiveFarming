package net.etylop.immersivefarming.item.custom;

import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.core.BlockPos;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class CompostItem extends Item {

    private static final int FERTILITY = 1;

    public CompostItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        // Clicking a plant fertilizes the soil below it (any block of a tall plant works)
        Level level = pContext.getLevel();
        BlockPos pos = IFFunctions.getSoilPos(level, pContext.getClickedPos());
        BlockState block = level.getBlockState(pos);
        if (!(block.getBlock() instanceof Soil))
            return InteractionResult.FAIL;

        if (block.getValue(Soil.FERTILITY) >= FERTILITY)
            return  InteractionResult.FAIL;

        level.setBlock(pos, block.setValue(Soil.FERTILITY, CompostItem.FERTILITY), 3);

        Player player = pContext.getPlayer();
        if (player == null || !player.getAbilities().instabuild)
            pContext.getItemInHand().shrink(1);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }
}
