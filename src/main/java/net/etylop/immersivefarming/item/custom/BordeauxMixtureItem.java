package net.etylop.immersivefarming.item.custom;

import net.etylop.immersivefarming.block.custom.Soil;
import net.etylop.immersivefarming.utils.IFFunctions;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Hand-applied fungicide: right click a growing crop to cure it and protect it from diseases until it is fully grown.
 * A bottle can be used several times, then leaves an empty glass bottle.
 */
public class BordeauxMixtureItem extends Item {

    public static final int USES = 8;
    /** Tint of the liquid in the bottle (layer 0 of the item model) */
    public static final int COLOR = 0x2EC4B6;
    private static final String NBT_USES = "uses";

    public BordeauxMixtureItem(Properties pProperties) {
        super(pProperties);
    }

    /** Remaining uses, a freshly crafted bottle (no tag) is full */
    public static int getUses(ItemStack stack) {
        return stack.hasTag() && stack.getTag().contains(NBT_USES) ? stack.getTag().getInt(NBT_USES) : USES;
    }

    public static void setUses(ItemStack stack, int uses) {
        stack.getOrCreateTag().putInt(NBT_USES, uses);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        // Clicking a plant treats the soil below it (any block of a tall plant works)
        Level level = pContext.getLevel();
        BlockPos pos = IFFunctions.getSoilPos(level, pContext.getClickedPos());
        BlockState soil = level.getBlockState(pos);
        if (!(soil.getBlock() instanceof Soil) || !IFFunctions.isGrowing(level.getBlockState(pos.above())))
            return InteractionResult.PASS;
        // Nothing to do: already protected and healthy
        if (soil.getValue(Soil.PROTECTED) && !soil.getValue(Soil.CONTAMINATED))
            return InteractionResult.PASS;

        Player player = pContext.getPlayer();
        level.playSound(player, pos, SoundEvents.BOTTLE_EMPTY, SoundSource.PLAYERS, 0.8F, 1.0F);
        if (!level.isClientSide()) {
            level.setBlock(pos, soil.setValue(Soil.CONTAMINATED, false).setValue(Soil.PROTECTED, true), 2);
            ((ServerLevel) level).sendParticles(ParticleTypes.HAPPY_VILLAGER,
                    pos.getX() + 0.5D, pos.getY() + 1.3D, pos.getZ() + 0.5D,
                    6, 0.3D, 0.3D, 0.3D, 0.0D);

            if (player == null || !player.getAbilities().instabuild) {
                ItemStack stack = pContext.getItemInHand();
                int uses = getUses(stack) - 1;
                if (uses > 0) {
                    setUses(stack, uses);
                } else if (player != null) {
                    player.setItemInHand(pContext.getHand(), new ItemStack(Items.GLASS_BOTTLE));
                } else {
                    stack.shrink(1);
                }
            }
        }
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return getUses(pStack) < USES;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return Math.round(13.0F * getUses(pStack) / USES);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return COLOR;
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.immersivefarming.bordeaux_mixture.uses", getUses(pStack), USES)
                .withStyle(ChatFormatting.GRAY));
    }
}
