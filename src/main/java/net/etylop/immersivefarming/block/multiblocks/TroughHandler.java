package net.etylop.immersivefarming.block.multiblocks;

import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemHandlerHelper;
import net.minecraftforge.items.ItemStackHandler;
import org.jetbrains.annotations.NotNull;

import java.util.function.Consumer;
import java.util.function.Predicate;

/**
 * The trough of a machine that feeds its animals (animal breeder, animal pen): a single slot, which holds one
 * kind of food until it is empty.
 */
public class TroughHandler extends ItemStackHandler
{
    private final int capacity;
    private final Predicate<ItemStack> isFeed;
    private final Runnable onChange;

    /**
     * @param isFeed   what an empty trough takes
     * @param onChange called when its content changes, but when loading
     */
    public TroughHandler(int capacity, Predicate<ItemStack> isFeed, Runnable onChange)
    {
        super(1);
        this.capacity = capacity;
        this.isFeed = isFeed;
        this.onChange = onChange;
    }

    public ItemStack getFood()
    {
        return stacks.get(0);
    }

    /** When loading: no update */
    public void setFood(ItemStack food)
    {
        stacks.set(0, food);
    }

    /** Automation can fill the trough, but not empty it */
    public IItemHandler insertOnly()
    {
        return new InsertOnly(this);
    }

    /**
     * Used by hand, as a feeding trough: food is added one item at a time, or a stack when sneaking, and taken back
     * when sneaking with an empty hand.
     */
    public void click(Player player, InteractionHand hand, Consumer<SoundEvent> playSound)
    {
        ItemStack held = player.getItemInHand(hand);
        if(held.isEmpty())
        {
            if(player.isShiftKeyDown()&&!getFood().isEmpty())
            {
                ItemStack taken = extractItem(0, capacity, false);
                if(!player.getInventory().add(taken))
                    player.drop(taken, false);
                playSound.accept(SoundEvents.ITEM_PICKUP);
            }
            return;
        }
        if(!isItemValid(0, held))
            return;
        ItemStack toInsert = held.copyWithCount(player.isShiftKeyDown()?held.getCount(): 1);
        int inserted = toInsert.getCount()-insertItem(0, toInsert, false).getCount();
        if(inserted==0)
            return;
        if(!player.getAbilities().instabuild)
            held.shrink(inserted);
        playSound.accept(SoundEvents.COMPOSTER_FILL);
    }

    /** Takes the food dropped there: on the input of the machine, or into its trough */
    public void collect(Level level, AABB... areas)
    {
        for(AABB area : areas)
            for(ItemEntity dropped : level.getEntitiesOfClass(ItemEntity.class, area, item -> item.isAlive()&&!item.getItem().isEmpty()))
            {
                ItemStack stack = dropped.getItem();
                ItemStack left = insertItem(0, stack.copy(), false);
                if(left.getCount()==stack.getCount())
                    continue;
                if(left.isEmpty())
                    dropped.discard();
                else
                    dropped.setItem(left);
            }
    }

    @Override
    public boolean isItemValid(int slot, @NotNull ItemStack stack)
    {
        ItemStack current = stacks.get(slot);
        return current.isEmpty()?isFeed.test(stack): ItemHandlerHelper.canItemStacksStack(current, stack);
    }

    @Override
    public int getSlotLimit(int slot)
    {
        return capacity;
    }

    @Override
    protected void onContentsChanged(int slot)
    {
        onChange.run();
    }

    private record InsertOnly(IItemHandler inner) implements IItemHandler
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
