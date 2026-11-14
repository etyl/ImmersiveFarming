package net.etylop.immersivefarming.block.entity;

import net.etylop.immersivefarming.block.IFBlockEntities;
import net.etylop.immersivefarming.block.custom.FeedingTroughBlock;
import net.etylop.immersivefarming.client.FeedColors;
import net.etylop.immersivefarming.entity.Livestock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.DistExecutor;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Food held by a feeding trough: up to {@link #CAPACITY} items of a single kind of food, shared by both halves.
 * It is held by the main half, the other half forwards everything to it, so that the trough can be filled from either half.
 * Filled by hand or by any item handler (hoppers, conveyors...), emptied only by animals eating,
 * or by the player sneaking with an empty hand.
 */
public class FeedingTroughBlockEntity extends BlockEntity
{
    /** Items held (of a single kind of food) */
    public static final int CAPACITY = 64;

    /** Color of the food shown before the client knows the contents */
    public static final int DEFAULT_FEED_COLOR = 0xC8A050;

    private final FoodHandler inventory = new FoodHandler();
    private final LazyOptional<IItemHandler> insertOnly = LazyOptional.of(() -> new InsertOnlyHandler(inventory));
    /** Server: the food last sent to the clients, to only send it again when it changes */
    private ItemStack sentDisplayFood = ItemStack.EMPTY;
    /** Client: color of the food shown in the trough, read by the block color handler while building chunk meshes */
    private volatile int displayColor = DEFAULT_FEED_COLOR;

    public FeedingTroughBlockEntity(BlockPos pos, BlockState state)
    {
        super(IFBlockEntities.FEEDING_TROUGH.get(), pos, state);
    }

    /** The block entity of the main half, which holds the food (this one, or the one of the other half) */
    @Nullable
    public FeedingTroughBlockEntity getMain()
    {
        BlockState state = getBlockState();
        if(!(state.getBlock() instanceof FeedingTroughBlock)||state.getValue(FeedingTroughBlock.PART)==FeedingTroughBlock.Part.MAIN)
            return this;
        BlockPos mainPos = FeedingTroughBlock.getMainPos(worldPosition, state);
        // Don't load the neighbouring chunk when the trough lies across a chunk border
        if(level!=null&&level.isLoaded(mainPos)&&level.getBlockEntity(mainPos) instanceof FeedingTroughBlockEntity main)
            return main;
        return null;
    }

    public boolean hasFoodFor(Animal animal)
    {
        return !peekFoodFor(animal).isEmpty();
    }

    /** The food in the trough if this animal eats it (without removing it), empty otherwise */
    public ItemStack peekFoodFor(Animal animal)
    {
        FeedingTroughBlockEntity main = getMain();
        if(main==null)
            return ItemStack.EMPTY;
        ItemStack food = main.inventory.getFood();
        return !food.isEmpty()&&animal.isFood(food)?food: ItemStack.EMPTY;
    }

    /**
     * Remove one item this animal can eat
     * @return the item eaten, empty if there is none
     */
    public ItemStack takeFoodFor(Animal animal)
    {
        FeedingTroughBlockEntity main = getMain();
        if(main==null||main.peekFoodFor(animal).isEmpty())
            return ItemStack.EMPTY;
        return main.inventory.extractItem(0, 1, false);
    }

    /**
     * Interaction with either half, called on the main half
     */
    public InteractionResult interact(Player player, InteractionHand hand)
    {
        ItemStack held = player.getItemInHand(hand);
        if(held.isEmpty())
        {
            if(inventory.getFood().isEmpty())
                player.displayClientMessage(Component.translatable("block.immersivefarming.feeding_trough.empty"), true);
            else if(player.isShiftKeyDown())
            {
                // Take all the food back
                ItemStack taken = inventory.extractItem(0, CAPACITY, false);
                if(!player.getInventory().add(taken))
                    player.drop(taken, false);
                playSound(SoundEvents.ITEM_PICKUP);
            }
            else
                player.displayClientMessage(describeContents(), true);
            return InteractionResult.CONSUME;
        }
        if(!Livestock.isFeed(held, level))
        {
            player.displayClientMessage(Component.translatable("block.immersivefarming.feeding_trough.not_feed"), true);
            return InteractionResult.CONSUME;
        }

        // Sneaking inserts the whole stack
        ItemStack toInsert = held.copyWithCount(player.isShiftKeyDown()?held.getCount(): 1);
        ItemStack rest = inventory.insertItem(0, toInsert, false);
        int inserted = toInsert.getCount()-rest.getCount();
        if(inserted==0)
        {
            player.displayClientMessage(Component.translatable(inventory.isItemValid(0, toInsert)
                    ?"block.immersivefarming.feeding_trough.full"
                    :"block.immersivefarming.feeding_trough.other_food"), true);
            return InteractionResult.CONSUME;
        }
        if(!player.getAbilities().instabuild)
            held.shrink(inserted);
        playSound(SoundEvents.COMPOSTER_FILL);
        return InteractionResult.CONSUME;
    }

    /** "Contains 12 Wheat" */
    private Component describeContents()
    {
        ItemStack food = inventory.getFood();
        return Component.translatable("block.immersivefarming.feeding_trough.contents",
                Component.translatable("block.immersivefarming.feeding_trough.item", food.getCount(), food.getHoverName()));
    }

    /**
     * Called when the trough is broken: the food is not lost (only the main half holds food)
     */
    public void dropContents()
    {
        if(level==null)
            return;
        ItemStack food = inventory.getFood();
        if(!food.isEmpty())
            Containers.dropItemStack(level, worldPosition.getX(), worldPosition.getY(), worldPosition.getZ(), food.copy());
    }

    public static int getFillLevel(int count)
    {
        if(count <= 0)
            return 0;
        return Math.min(FeedingTroughBlock.MAX_FILL, (count*FeedingTroughBlock.MAX_FILL+CAPACITY-1)/CAPACITY);
    }

    /** Both halves show how full the shared inventory is */
    private void updateFillLevel()
    {
        if(level==null||level.isClientSide)
            return;
        BlockState state = level.getBlockState(worldPosition);
        if(!(state.getBlock() instanceof FeedingTroughBlock))
            return;
        int fill = getFillLevel(inventory.getFood().getCount());
        for(BlockPos pos : new BlockPos[]{worldPosition, FeedingTroughBlock.getPartnerPos(worldPosition, state)})
        {
            BlockState half = level.getBlockState(pos);
            if(half.getBlock() instanceof FeedingTroughBlock&&half.getValue(FeedingTroughBlock.FILL)!=fill)
                level.setBlock(pos, half.setValue(FeedingTroughBlock.FILL, fill), Block.UPDATE_CLIENTS);
        }
    }

    /** The food shown in the trough (one item), empty if the trough is empty */
    private ItemStack getDisplayFood()
    {
        return inventory.getFood().copyWithCount(1);
    }

    /** Send the food shown to the clients when it changes, so that they can color the trough content */
    private void syncDisplayFood()
    {
        if(level==null||level.isClientSide)
            return;
        ItemStack display = getDisplayFood();
        if(!ItemStack.isSameItemSameTags(display, sentDisplayFood))
        {
            sentDisplayFood = display;
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), Block.UPDATE_CLIENTS);
        }
    }

    /** Client: the color of the food in this trough (both halves use the main half's) */
    public int getDisplayColor()
    {
        return displayColor;
    }

    @Override
    public CompoundTag getUpdateTag()
    {
        // Only what the client needs: the food shown
        CompoundTag tag = new CompoundTag();
        ItemStack display = getDisplayFood();
        if(!display.isEmpty())
            tag.put("display", display.save(new CompoundTag()));
        return tag;
    }

    @Override
    public ClientboundBlockEntityDataPacket getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        readDisplayFood(tag);
    }

    @Override
    public void onDataPacket(Connection connection, ClientboundBlockEntityDataPacket packet)
    {
        readDisplayFood(packet.getTag());
    }

    /** Client, main thread: compute the color of the food received, and redraw both halves if it changed */
    private void readDisplayFood(@Nullable CompoundTag tag)
    {
        ItemStack display = tag!=null&&tag.contains("display", Tag.TAG_COMPOUND)?ItemStack.of(tag.getCompound("display")): ItemStack.EMPTY;
        Integer color = display.isEmpty()?null: DistExecutor.unsafeCallWhenOn(Dist.CLIENT, () -> () -> FeedColors.getColor(display));
        int newColor = color==null?DEFAULT_FEED_COLOR: color;
        if(newColor==displayColor)
            return;
        displayColor = newColor;
        if(level!=null&&level.isClientSide)
        {
            BlockState state = getBlockState();
            level.sendBlockUpdated(worldPosition, state, state, Block.UPDATE_IMMEDIATE);
            if(state.getBlock() instanceof FeedingTroughBlock)
            {
                BlockPos partner = FeedingTroughBlock.getPartnerPos(worldPosition, state);
                BlockState partnerState = level.getBlockState(partner);
                level.sendBlockUpdated(partner, partnerState, partnerState, Block.UPDATE_IMMEDIATE);
            }
        }
    }

    private void playSound(SoundEvent sound)
    {
        if(level!=null)
            level.playSound(null, worldPosition, sound, SoundSource.BLOCKS, 1, 1);
    }

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if(cap==ForgeCapabilities.ITEM_HANDLER)
        {
            // Either half accepts food into the shared inventory
            FeedingTroughBlockEntity main = getMain();
            return main==null?LazyOptional.empty(): main.insertOnly.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        insertOnly.invalidate();
    }

    @Override
    protected void saveAdditional(CompoundTag nbt)
    {
        super.saveAdditional(nbt);
        nbt.put("food", inventory.getFood().save(new CompoundTag()));
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);
        inventory.setFood(ItemStack.of(nbt.getCompound("food")));
        // The clients receive it with the chunk
        sentDisplayFood = getDisplayFood();
    }

    /** A single slot of {@link #CAPACITY} items: the trough holds one kind of food until it is empty */
    private class FoodHandler extends ItemStackHandler
    {
        FoodHandler()
        {
            super(1);
        }

        ItemStack getFood()
        {
            return stacks.get(0);
        }

        /** When loading: no block update */
        void setFood(ItemStack food)
        {
            stacks.set(0, food);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            ItemStack current = stacks.get(slot);
            return current.isEmpty()?Livestock.isFeed(stack, level): ItemHandlerHelper.canItemStacksStack(current, stack);
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return CAPACITY;
        }

        @Override
        protected void onContentsChanged(int slot)
        {
            setChanged();
            updateFillLevel();
            syncDisplayFood();
        }
    }

    /**
     * Automation can fill the trough, but not empty it (a hopper below would otherwise drain it)
     */
    private record InsertOnlyHandler(IItemHandler inner) implements IItemHandler
    {
        @Override
        public int getSlots()
        {
            return inner.getSlots();
        }

        @Override
        public @NotNull ItemStack getStackInSlot(int slot)
        {
            return inner.getStackInSlot(slot);
        }

        @Override
        public @NotNull ItemStack insertItem(int slot, @NotNull ItemStack stack, boolean simulate)
        {
            return inner.insertItem(slot, stack, simulate);
        }

        @Override
        public @NotNull ItemStack extractItem(int slot, int amount, boolean simulate)
        {
            return ItemStack.EMPTY;
        }

        @Override
        public int getSlotLimit(int slot)
        {
            return inner.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, @NotNull ItemStack stack)
        {
            return inner.isItemValid(slot, stack);
        }
    }
}
