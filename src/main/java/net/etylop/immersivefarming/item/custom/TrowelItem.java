package net.etylop.immersivefarming.item.custom;

import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class TrowelItem extends Item {

    public TrowelItem(Properties pProperties) {
        super(pProperties);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        BlockPos pos = pContext.getClickedPos();
        Player player = pContext.getPlayer();
        if (player == null)
            return InteractionResult.PASS;

        // Clicking a crop inspects the soil below it (any block of a tall plant works)
        pos = IFFunctions.getSoilPos(level, pos);
        BlockState soil = level.getBlockState(pos);
        if (!(soil.getBlock() instanceof Soil))
            return InteractionResult.PASS;

        if (!level.isClientSide()) {
            player.displayClientMessage(getSoilInfo(soil), true);
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    private static Component getSoilInfo(BlockState soil) {
        String key = "item.immersivefarming.trowel.";

        int till = soil.getValue(Soil.TILL);
        if (till < Soil.TILL_MAX) {
            return Component.translatable(key + "till", till, Soil.TILL_MAX).withStyle(ChatFormatting.GOLD);
        }

        int water = soil.getValue(Soil.WATER);
        MutableComponent waterInfo;
        if (water == 0)
            waterInfo = Component.translatable(key + "dry").withStyle(ChatFormatting.RED);
        else if (water == Soil.WATER_FLOODED)
            waterInfo = Component.translatable(key + "flooded").withStyle(ChatFormatting.RED);
        else
            waterInfo = Component.translatable(key + "water", water, Soil.waterSaturated()).withStyle(ChatFormatting.AQUA);

        MutableComponent info = waterInfo.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
        if (soil.getValue(Soil.FERTILITY) > 0)
            info.append(Component.translatable(key + "fertilized").withStyle(ChatFormatting.GREEN));
        else
            info.append(Component.translatable(key + "unfertilized").withStyle(ChatFormatting.GRAY));

        info.append(Component.literal(" | ").withStyle(ChatFormatting.GRAY));
        if (soil.getValue(Soil.CONTAMINATED))
            info.append(Component.translatable(key + "contaminated").withStyle(ChatFormatting.DARK_PURPLE));
        else
            info.append(Component.translatable(key + "healthy").withStyle(ChatFormatting.GREEN));
        return info;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.immersivefarming.trowel.tooltip").withStyle(ChatFormatting.GRAY));
    }
}
