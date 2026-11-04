package net.etylop.immersivefarming.entity;

import com.google.common.collect.ImmutableList;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.gui.container.SowerContainer;
import net.etylop.immersivefarming.item.IFItems;
import net.etylop.immersivefarming.item.custom.CompostItem;
import net.etylop.immersivefarming.utils.cart.CartItemStackHandler;
import net.etylop.immersivefarming.utils.cart.ProxyItemUseContext;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemNameBlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.CropBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.network.NetworkHooks;

import java.util.ArrayList;


public final class SowerEntity extends AbstractDrawnInventoryEntity {
    private static final int SEED_COUNT = 9;
    private static final int FERTILIZER_COUNT = 9;
    private static final double BLADEOFFSET = 1.7D;
    private static final EntityDataAccessor<Boolean> PLOWING = SynchedEntityData.defineId(SowerEntity.class, EntityDataSerializers.BOOLEAN);

    static final ImmutableList<EntityDataAccessor<ItemStack>> INVENTORY;

    static {
        ArrayList<EntityDataAccessor<ItemStack>> INV = new ArrayList<>();
        for (int i=0; i<SEED_COUNT+FERTILIZER_COUNT; i++) INV.add(SynchedEntityData.defineId(SowerEntity.class, EntityDataSerializers.ITEM_STACK));
        INVENTORY = ImmutableList.copyOf(INV);
    }


    public SowerEntity(final EntityType<? extends Entity> entityTypeIn, final Level worldIn) {
        super(entityTypeIn, worldIn);
        this.spacing = 1.3D;
    }

    @Override
    protected IFConfig.CartConfig getConfig() {
        return IFConfig.get().plow;
    }

    @Override
    protected ItemStackHandler initInventory() {
        return new CartItemStackHandler<SowerEntity>(SEED_COUNT+FERTILIZER_COUNT, this) {
            @Override
            protected void onLoad() {
                for (int i = 0; i < INVENTORY.size(); i++) {
                    this.cart.getEntityData().set(INVENTORY.get(i), this.getStackInSlot(i));
                }
            }

            @Override
            protected void onContentsChanged(final int slot) {
                this.cart.updateSlot(slot);
            }
        };
    }

    public boolean getPlowing() {
        return this.entityData.get(PLOWING);
    }

    @Override
    public void pulledTick() {
        super.pulledTick();
        if (this.getPulling() == null) {
            return;
        }
        if (!this.level().isClientSide) {
            Player player = null;
            if (this.getPulling() instanceof Player pl) {
                player = pl;
            } else if (this.getPulling().getControllingPassenger() instanceof Player pl) {
                player = pl;
            }
            if (this.entityData.get(PLOWING) && player != null) {
                if (this.xo != this.getX() || this.zo != this.getZ()) {
                    this.plow(player);
                }
            }
        }
    }

    private ItemStack getSeeds() {
        for (int i=0; i<SEED_COUNT; i++) {
            final ItemStack stack = this.getStackInSlot(i);
            if (stack.getItem() instanceof ItemNameBlockItem blockItem && blockItem.getBlock() instanceof CropBlock) {
                return stack;
            }
        }
        return null;
    }

    private ItemStack getFertilizer() {
        for (int i=0; i<FERTILIZER_COUNT; i++) {
            final ItemStack stack = this.getStackInSlot(SEED_COUNT+i);
            if (stack.getItem() instanceof CompostItem) {
                return stack;
            }
        }
        return null;
    }

    private void plow(final Player player) {
        for (int i = 0; i < 3; i++) {

            final float offset = 38.0F - i * 38.0F;
            final double blockPosX = this.getX() + Mth.sin((float) Math.toRadians(this.getYRot() - offset)) * BLADEOFFSET;
            final double blockPosZ = this.getZ() - Mth.cos((float) Math.toRadians(this.getYRot() - offset)) * BLADEOFFSET;
            final BlockPos blockPos = BlockPos.containing(blockPosX, this.getY() - 0.5D, blockPosZ);

            ItemStack seeds = getSeeds();
            if (seeds != null) {
                InteractionResult result = seeds.getItem().useOn(new ProxyItemUseContext(player, seeds, new BlockHitResult(Vec3.ZERO, Direction.UP, blockPos, false)));
                if (result == InteractionResult.CONSUME) {
                    seeds.setCount(seeds.getCount()-1);
                }
            }

            ItemStack fertilizer = getFertilizer();
            if (fertilizer!=null) {
                InteractionResult result = fertilizer.getItem().useOn(new ProxyItemUseContext(player, seeds, new BlockHitResult(Vec3.ZERO, Direction.UP, blockPos, false)));
                if (result == InteractionResult.CONSUME) {
                    fertilizer.setCount(fertilizer.getCount()-1);
                }
            }
        }
    }

    @Override
    public InteractionResult interact(final Player player, final InteractionHand hand) {
        if (player.isSecondaryUseActive()) {
            this.openContainer(player);
            return InteractionResult.sidedSuccess(this.level().isClientSide);
        }
        if (!this.level().isClientSide) {
            this.entityData.set(PLOWING, !this.entityData.get(PLOWING));
        }
        return InteractionResult.sidedSuccess(this.level().isClientSide);
    }

    public void updateSlot(final int slot) {
        if (!this.level().isClientSide) {
            if (this.inventory.getStackInSlot(slot).isEmpty()) {
                this.entityData.set(INVENTORY.get(slot), ItemStack.EMPTY);
            } else {
                this.entityData.set(INVENTORY.get(slot), this.inventory.getStackInSlot(slot));
            }

        }
    }

    public ItemStack getStackInSlot(final int i) {
        return this.entityData.get(INVENTORY.get(i));
    }

    @Override
    public Item getCartItem() {
        return IFItems.SOWER.get();
    }

    @Override
    protected void defineSynchedData() {
        super.defineSynchedData();
        this.entityData.define(PLOWING, false);
        for (final EntityDataAccessor<ItemStack> param : INVENTORY) {
            this.entityData.define(param, ItemStack.EMPTY);
        }
    }

    @Override
    protected void readAdditionalSaveData(final CompoundTag compound) {
        super.readAdditionalSaveData(compound);
        this.entityData.set(PLOWING, compound.getBoolean("Plowing"));
    }

    @Override
    protected void addAdditionalSaveData(final CompoundTag compound) {
        super.addAdditionalSaveData(compound);
        compound.putBoolean("Plowing", this.entityData.get(PLOWING));
    }

    private void openContainer(final Player player) {
        if (player instanceof ServerPlayer serverPlayer) {
            NetworkHooks.openScreen(serverPlayer,
                new SimpleMenuProvider((windowId, playerInventory, p) -> new SowerContainer(windowId, playerInventory, this), this.getDisplayName()),
                buf -> buf.writeInt(this.getId())
            );
        }
    }
}
