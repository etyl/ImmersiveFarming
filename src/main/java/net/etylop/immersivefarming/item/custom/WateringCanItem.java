package net.etylop.immersivefarming.item.custom;

import net.etylop.immersivefarming.block.custom.Soil;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public class WateringCanItem extends Item {

    public static final int CAPACITY = 32;
    private static final String NBT_WATER = "water";
    private static final int COOLDOWN = 5;

    public WateringCanItem(Properties pProperties) {
        super(pProperties);
    }

    public static int getWater(ItemStack stack) {
        return stack.hasTag() ? stack.getTag().getInt(NBT_WATER) : 0;
    }

    public static void setWater(ItemStack stack, int water) {
        stack.getOrCreateTag().putInt(NBT_WATER, water);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level pLevel, Player pPlayer, InteractionHand pUsedHand) {
        ItemStack stack = pPlayer.getItemInHand(pUsedHand);

        if (getWater(stack) < CAPACITY) {
            BlockHitResult hit = getPlayerPOVHitResult(pLevel, pPlayer, ClipContext.Fluid.SOURCE_ONLY);
            if (hit.getType() == HitResult.Type.BLOCK && pLevel.getFluidState(hit.getBlockPos()).is(FluidTags.WATER)) {
                pLevel.playSound(pPlayer, pPlayer.getX(), pPlayer.getY(), pPlayer.getZ(), SoundEvents.BUCKET_FILL, SoundSource.PLAYERS, 1.0F, 1.0F);
                if (!pLevel.isClientSide())
                    setWater(stack, CAPACITY);
                return InteractionResultHolder.sidedSuccess(stack, pLevel.isClientSide());
            }
        }
        return InteractionResultHolder.pass(stack);
    }

    @Override
    public InteractionResult useOn(UseOnContext pContext) {
        Level level = pContext.getLevel();
        Player player = pContext.getPlayer();
        ItemStack stack = pContext.getItemInHand();
        boolean creative = player != null && player.getAbilities().instabuild;
        if (getWater(stack) <= 0 && !creative)
            return InteractionResult.PASS;

        // Clicking a plant waters the soil below it
        BlockPos pos = pContext.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof Soil))
            pos = pos.below();
        BlockState soil = level.getBlockState(pos);
        if (!(soil.getBlock() instanceof Soil) || soil.getValue(Soil.WATER) >= Soil.WATER_SATURATED)
            return InteractionResult.PASS;

        level.playSound(player, pos, SoundEvents.GENERIC_SPLASH, SoundSource.PLAYERS, 0.5F, 1.2F);
        if (!level.isClientSide()) {
            level.setBlock(pos, soil.setValue(Soil.WATER, Soil.WATER_SATURATED), 2);
            ((ServerLevel) level).sendParticles(ParticleTypes.SPLASH,
                    pos.getX() + 0.5D, pos.getY() + 1.0D, pos.getZ() + 0.5D,
                    8, 0.3D, 0.1D, 0.3D, 0.0D);
            if (!creative)
                setWater(stack, getWater(stack) - 1);
        }
        if (player != null)
            player.getCooldowns().addCooldown(this, COOLDOWN);
        return InteractionResult.sidedSuccess(level.isClientSide());
    }

    @Override
    public boolean isBarVisible(ItemStack pStack) {
        return getWater(pStack) < CAPACITY;
    }

    @Override
    public int getBarWidth(ItemStack pStack) {
        return Math.round(13.0F * getWater(pStack) / CAPACITY);
    }

    @Override
    public int getBarColor(ItemStack pStack) {
        return 0x3F76E4;
    }

    @Override
    public boolean shouldCauseReequipAnimation(ItemStack oldStack, ItemStack newStack, boolean slotChanged) {
        return slotChanged || oldStack.getItem() != newStack.getItem();
    }

    @Override
    public void appendHoverText(ItemStack pStack, @Nullable Level pLevel, List<Component> pTooltipComponents, TooltipFlag pIsAdvanced) {
        pTooltipComponents.add(Component.translatable("item.immersivefarming.watering_can.water", getWater(pStack), CAPACITY)
                .withStyle(ChatFormatting.GRAY));
    }
}
