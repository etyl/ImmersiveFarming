package net.etylop.immersivefarming.block.multiblocks.animalpen;

import blusunrize.immersiveengineering.api.IETags;
import blusunrize.immersiveengineering.api.energy.MutableEnergyStorage;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl.RSState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import blusunrize.immersiveengineering.api.utils.CapabilityReference;
import blusunrize.immersiveengineering.common.util.IEDamageSources;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.Utils;
import net.etylop.immersivefarming.block.multiblocks.AnimalStall;
import net.etylop.immersivefarming.block.multiblocks.TroughHandler;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.entity.AnimalFeeding;
import net.etylop.immersivefarming.entity.Livestock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.OwnableEntity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.BooleanOp;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.living.LivingDropsEvent;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenShapes.STALL_MAX_X;
import static net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenShapes.STALL_MAX_Z;
import static net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenShapes.STALL_MIN_X;
import static net.etylop.immersivefarming.block.multiblocks.animalpen.AnimalPenShapes.STALL_MIN_Z;

/**
 * The animal pen: it slaughters the animal it is given, once it is grown. A stall behind a gate at the back of the
 * machine, between two coils, and at the front a trough between the output box and the control box.
 * <ul>
 * <li>The open gate takes in the animal standing in its doorway, or carried through it by a conveyor, and closes
 * behind it, as a stall of the animal breeder does (see {@link AnimalStall}). Right clicking the back of the
 * machine opens or closes the gate: opening it lets the animal out, a gate closed on an empty stall keeps animals
 * out.</li>
 * <li>A baby eats from the trough when it is hungry, and grows as any other animal does (see
 * {@link AnimalFeeding}). The trough is filled through the feed input at the top of the machine, above the output
 * box (by hoppers, conveyors, or items dropped on it), and as that of the breeder: by hand, and from its sides.</li>
 * <li>A grown animal is electrocuted by the coils (after a few seconds in the stall), and what it drops comes out of
 * the output box, on the left of the trough: into the inventory or onto the conveyor in front of it, on the ground
 * otherwise. This is all the machine uses energy for, which it takes from the top of its power plug, at the back.
 * A redstone signal on the control box, on the right of the trough, stops it (or lets it run, when inverted with a
 * screwdriver): the grown animal then waits in its stall.</li>
 * </ul>
 * Positions are in multiblock coordinates: x from left to right (0 to 3), z from the gate (0) to the front (3).
 */
public class AnimalPenLogic implements IMultiblockLogic<AnimalPenLogic.State>, IServerTickableComponent<AnimalPenLogic.State>,
        IClientTickableComponent<AnimalPenLogic.State>
{
    /** Items held by the trough (of a single kind of food) */
    public static final int TROUGH_CAPACITY = 64;
    public static final int ENERGY_CAPACITY = 32000;
    /** The control box, on the right of the trough */
    public static final BlockPos REDSTONE_POS = new BlockPos(2, 0, 2);
    /** Top of the power plug: at the back of the machine, above the bay on the same side as the control box */
    private static final CapabilityPosition ENERGY_INPUT = new CapabilityPosition(2, 1, 0, RelativeBlockFace.UP);
    /** The block in front of the output box, on the left of the trough, where what is harvested is inserted or dropped */
    private static final MultiblockFace OUTPUT = new MultiblockFace(0, 0, 3, RelativeBlockFace.FRONT);
    private static final BlockPos TROUGH_POS = new BlockPos(1, 0, 2);
    /** Top of the feed input, above the output box */
    private static final CapabilityPosition FEED_INPUT = new CapabilityPosition(0, 1, 2, RelativeBlockFace.UP);
    private static final BlockPos GATE_POS = new BlockPos(1, 0, 0);

    /** How far behind the open gate an animal is taken in (blocks) */
    private static final double APPROACH = 0.875;
    /** Where the animal stands in the stall (z, in pixels): in the middle of it, clear of the gate */
    private static final double STAND_Z = 18;
    /** Animals that fit in the stall, and under the open gate, once they are grown */
    private static final float MAX_WIDTH = 1.1F;
    private static final float MAX_HEIGHT = AnimalPenShapes.GATE_CLEARANCE/16F;
    /** Ticks a grown animal stands in the stall before the coils fire: it is not struck the moment it comes in */
    private static final int SLAUGHTER_DELAY = 100;
    /** Ticks before trying again to slaughter an animal that did not die (protected by another mod, ...) */
    private static final int RETRY_DELAY = 100;
    /** Ticks the arcs between the coils last after a slaughter (client) */
    public static final int ARC_TIME = 14;

    /** What the animal being slaughtered drops (server thread), see {@link #onDrops} */
    @Nullable
    private static Harvest harvest;

    // ------------------------------------------------------------------ ticking

    @Override
    public void tickServer(IMultiblockContext<State> context)
    {
        final State state = context.getState();
        final IMultiblockLevel level = context.getLevel();
        final Level rawLevel = level.getRawLevel();
        state.locate(level);

        boolean sync = false;
        AnimalStall.Change change = state.stall.tick(rawLevel, state.place, AnimalPenLogic::fits);
        if(change!=AnimalStall.Change.NONE)
        {
            playSound(level, GATE_POS, change==AnimalStall.Change.TAKEN?SoundEvents.IRON_TRAPDOOR_CLOSE: SoundEvents.IRON_TRAPDOOR_OPEN);
            sync = true;
        }
        if(level.shouldTickModulo(8))
            state.trough.collect(rawLevel, state.feedAreas);

        Animal animal = state.stall.getAnimal();
        if(animal==null||animal.isBaby())
        {
            // Counted down once a grown animal stands in the stall
            state.slaughterDelay = SLAUGHTER_DELAY;
            if(animal!=null&&level.shouldTickModulo(20))
                feed(state, animal);
        }
        else
            sync |= slaughter(context, state, level, animal);

        // Received from the power plug
        if(state.energy.getEnergyStored()!=state.savedEnergy)
        {
            state.savedEnergy = state.energy.getEnergyStored();
            context.markMasterDirty();
        }
        if(sync)
            context.markDirtyAndSync();
    }

    /** One meal from the trough when the animal is hungry: it then grows for a while */
    private static void feed(State state, Animal animal)
    {
        ItemStack food = state.trough.getFood();
        if(food.isEmpty()||!AnimalFeeding.isHungry(animal)||!animal.isFood(food))
            return;
        // The trough tells the clients itself
        AnimalFeeding.feed(animal, state.trough.extractItem(0, 1, false));
    }

    /**
     * Electrocutes the grown animal, if the machine runs and has the energy for it: what it drops comes out of the
     * output box, and the stall is free for the next one.
     *
     * @return whether the clients must be told of a change
     */
    private static boolean slaughter(IMultiblockContext<State> context, State state, IMultiblockLevel level, Animal animal)
    {
        if(state.slaughterDelay > 0)
        {
            state.slaughterDelay--;
            return false;
        }
        int cost = getEnergyPerAnimal();
        if(state.energy.getEnergyStored() < cost||!state.rsState.isEnabled(context))
            return false;
        final Level rawLevel = level.getRawLevel();
        Harvest mine = new Harvest(animal, new ArrayList<>());
        harvest = mine;
        try
        {
            animal.hurt(IEDamageSources.causeTeslaDamage(rawLevel, Float.MAX_VALUE, false), Float.MAX_VALUE);
        } finally
        {
            harvest = null;
        }
        if(animal.isAlive())
        {
            state.slaughterDelay = RETRY_DELAY;
            return false;
        }
        state.energy.setStoredEnergy(state.energy.getEnergyStored()-cost);
        for(ItemStack stack : mine.items)
        {
            ItemStack left = Utils.insertStackIntoInventory(state.output, stack, false);
            if(!left.isEmpty())
                Utils.dropStackAtPos(rawLevel, level.toAbsolute(OUTPUT.posInMultiblock()), left, level.toAbsolute(RelativeBlockFace.BACK));
        }
        state.stall.clear();
        state.slaughters++;

        Vec3 middle = animal.getBoundingBox().getCenter();
        rawLevel.playSound(null, middle.x, middle.y, middle.z, IESounds.tesla.get(), SoundSource.BLOCKS, 1.5F, 1);
        playSound(level, GATE_POS, SoundEvents.IRON_TRAPDOOR_OPEN);
        if(rawLevel instanceof ServerLevel serverLevel)
            serverLevel.sendParticles(ParticleTypes.ELECTRIC_SPARK, middle.x, middle.y, middle.z, 24,
                    animal.getBbWidth()/3, animal.getBbHeight()/3, animal.getBbWidth()/3, 0.4);
        return true;
    }

    /**
     * Called when a living entity drops its loot: the drops of the animal an animal pen is slaughtering are
     * taken by the pen, they are not spawned.
     */
    public static void onDrops(LivingDropsEvent event)
    {
        Harvest mine = harvest;
        if(mine==null||event.getEntity()!=mine.animal)
            return;
        for(ItemEntity drop : event.getDrops())
            if(!drop.getItem().isEmpty())
                mine.items.add(drop.getItem().copy());
        event.setCanceled(true);
    }

    /** The animal an animal pen is slaughtering, and what it has dropped */
    private record Harvest(LivingEntity animal, List<ItemStack> items)
    {
    }

    // ------------------------------------------------------------------ animals

    /**
     * The animals the pen takes in, whatever their age: those that fit in the stall once they are grown, on their
     * own feet. Pets are left alone.
     */
    private static boolean fits(Animal animal)
    {
        if(!AnimalStall.isFree(animal)||(animal instanceof OwnableEntity pet&&pet.getOwnerUUID()!=null))
            return false;
        // A baby is as big as its type says once it is grown
        return Math.max(animal.getBbWidth(), animal.getType().getWidth()) <= MAX_WIDTH
                &&Math.max(animal.getBbHeight(), animal.getType().getHeight()) <= MAX_HEIGHT;
    }

    private static int getEnergyPerAnimal()
    {
        return IFConfig.get().animals.animal_pen_energy.get();
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult click(
            IMultiblockContext<State> ctx, BlockPos posInMultiblock, Player player, InteractionHand hand,
            BlockHitResult absoluteHit, boolean isClient
    )
    {
        boolean trough = TROUGH_POS.equals(posInMultiblock);
        ItemStack held = player.getItemInHand(hand);
        // The trough and the gate are all there is to use, and the screwdriver is for the redstone control
        if((!trough&&!AnimalPenShapes.isGate(posInMultiblock))||held.is(IETags.screwdrivers))
            return InteractionResult.PASS;
        // What the trough does not take is used as on any other block, and so is what is held when sneaking: a block
        // is placed against the machine. Sneaking only uses the trough, to put a whole stack of food in it
        if(!held.isEmpty()&&(trough?!ctx.getState().trough.isItemValid(0, held): player.isSecondaryUseActive()))
            return InteractionResult.PASS;
        if(isClient)
            return InteractionResult.SUCCESS;
        final State state = ctx.getState();
        final IMultiblockLevel level = ctx.getLevel();
        if(trough)
            state.trough.click(player, hand, sound -> playSound(level, TROUGH_POS, sound));
        else
        {
            state.locate(level);
            boolean closed = state.stall.toggle(level.getRawLevel(), state.place, AnimalPenLogic::fits);
            playSound(level, GATE_POS, closed?SoundEvents.IRON_TRAPDOOR_CLOSE: SoundEvents.IRON_TRAPDOOR_OPEN);
            ctx.markDirtyAndSync();
        }
        return InteractionResult.CONSUME;
    }

    private static void playSound(IMultiblockLevel level, BlockPos posInMultiblock, SoundEvent sound)
    {
        level.getRawLevel().playSound(null, level.toAbsolute(posInMultiblock), sound, SoundSource.BLOCKS, 1, 1);
    }

    @Override
    public <T> LazyOptional<T> getCapability(IMultiblockContext<State> ctx, CapabilityPosition position, Capability<T> cap)
    {
        final State state = ctx.getState();
        if(cap==ForgeCapabilities.ENERGY&&ENERGY_INPUT.equalsOrNullFace(position))
            return state.energyInput.cast(ctx);
        // The trough is filled through the top of the feed input, and from any of its sides, as a feeding trough
        if(cap==ForgeCapabilities.ITEM_HANDLER&&(FEED_INPUT.equalsOrNullFace(position)||TROUGH_POS.equals(position.posInMultiblock())))
            return state.feedInput.cast(ctx);
        return LazyOptional.empty();
    }

    @Override
    public void dropExtraItems(State state, Consumer<ItemStack> drop)
    {
        ItemStack food = state.trough.getFood();
        if(!food.isEmpty())
            drop.accept(food.copy());
    }

    // ------------------------------------------------------------------ shapes

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType)
    {
        return pos -> AnimalPenShapes.getShape(pos, forType);
    }

    @Override
    public VoxelShape postProcessAbsoluteShape(
            IMultiblockContext<State> ctx, VoxelShape defaultShape, CollisionContext shapeCtx, BlockPos posInMultiblock, ShapeType type
    )
    {
        if(!AnimalPenShapes.isGate(posInMultiblock))
            return defaultShape;
        VoxelShape gate = AnimalPenShapes.getGateShape(posInMultiblock, ctx.getState().stall.closed, ctx.getLevel().getOrientation(), type);
        return gate.isEmpty()?defaultShape: Shapes.joinUnoptimized(defaultShape, gate, BooleanOp.OR);
    }

    // ------------------------------------------------------------------ client

    @Override
    public void tickClient(IMultiblockContext<State> context)
    {
        final State state = context.getState();
        state.prevGate = state.gate;
        state.gate += Mth.clamp((state.stall.closed?1: 0)-state.gate, -0.1F, 0.1F);
        if(state.arcTicks > 0)
            state.arcTicks--;
    }

    @Override
    public State createInitialState(IInitialMultiblockContext<State> capabilitySource)
    {
        return new State(capabilitySource);
    }

    // ------------------------------------------------------------------ state

    public static class State implements IMultiblockState
    {
        public final AnimalStall stall = new AnimalStall();
        public final TroughHandler trough;
        /** Filled from the power plug, and only used to slaughter: nothing takes it back */
        public final MutableEnergyStorage energy = new MutableEnergyStorage(ENERGY_CAPACITY, ENERGY_CAPACITY, 0);
        public final RSState rsState = RSState.enabledByDefault();
        private int savedEnergy;
        /** Ticks before the grown animal of the stall is slaughtered (not saved: it waits again after a reload) */
        private int slaughterDelay = SLAUGHTER_DELAY;
        /** Number of animals slaughtered: the coils arc when it changes (synced) */
        private int slaughters;

        // Client only
        /** How far the gate is closed, from 0 (open) to 1 */
        public float gate;
        public float prevGate;
        /** Ticks the arcs between the coils still last */
        public int arcTicks;
        private boolean synced;

        // Absolute positions, computed once (server only)
        private AnimalStall.Place place;
        /** Where dropped food is taken: on the feed input, and in the trough */
        private AABB[] feedAreas;

        private final Supplier<Level> level;
        private final CapabilityReference<IItemHandler> output;
        private final StoredCapability<IItemHandler> feedInput;
        private final StoredCapability<IEnergyStorage> energyInput;

        public State(IInitialMultiblockContext<State> ctx)
        {
            final Runnable markDirty = ctx.getMarkDirtyRunnable();
            final Runnable sync = ctx.getSyncRunnable();
            this.level = ctx.levelSupplier();
            this.trough = new TroughHandler(TROUGH_CAPACITY, this::isFeed, () -> {
                markDirty.run();
                sync.run();
            });
            this.output = ctx.getCapabilityAt(ForgeCapabilities.ITEM_HANDLER, OUTPUT);
            this.feedInput = new StoredCapability<>(trough.insertOnly());
            this.energyInput = new StoredCapability<>(energy);
        }

        private void locate(IMultiblockLevel level)
        {
            if(place!=null)
                return;
            // The front of the multiblock (in IE's terms) is the direction its gate faces
            place = new AnimalStall.Place(
                    level.toAbsolute(AnimalPenShapes.px(STALL_MIN_X, 0, STALL_MIN_Z, STALL_MAX_X, 32, STALL_MAX_Z)),
                    level.toAbsolute(AnimalPenShapes.px(STALL_MIN_X, 0, -APPROACH*16, STALL_MAX_X, 32, STALL_MAX_Z)),
                    level.toAbsolute(new Vec3((STALL_MIN_X+STALL_MAX_X)/32D, 1/16D, STAND_Z/16)),
                    Mth.wrapDegrees(level.getOrientation().front().toYRot()+180)
            );
            feedAreas = new AABB[]{
                    level.toAbsolute(AnimalPenShapes.px(0, 32-2, STALL_MAX_Z, AnimalPenShapes.BOX_WIDTH, 32+8, 48)),
                    level.toAbsolute(AnimalPenShapes.px(
                            AnimalPenShapes.TROUGH_MIN_X, AnimalPenShapes.TROUGH_HEIGHT-2, STALL_MAX_Z,
                            AnimalPenShapes.TROUGH_MAX_X, AnimalPenShapes.TROUGH_HEIGHT+8, 48
                    ))
            };
        }

        /** Food of the animals that need to be fed, and whatever the animal in the stall eats */
        private boolean isFeed(ItemStack stack)
        {
            if(Livestock.isFeed(stack, level.get()))
                return true;
            Animal animal = stall.getAnimal();
            return animal!=null&&animal.isFood(stack);
        }

        /** Client: the food shown in the trough */
        public ItemStack getDisplayFood()
        {
            return trough.getFood();
        }

        @Override
        public void writeSaveNBT(CompoundTag nbt)
        {
            nbt.put("stall", stall.save());
            nbt.put("food", trough.getFood().save(new CompoundTag()));
            nbt.putInt("energy", energy.getEnergyStored());
            nbt.putInt("slaughters", slaughters);
        }

        @Override
        public void readSaveNBT(CompoundTag nbt)
        {
            stall.load(nbt.getCompound("stall"));
            trough.setFood(ItemStack.of(nbt.getCompound("food")));
            energy.setStoredEnergy(nbt.getInt("energy"));
            savedEnergy = energy.getEnergyStored();
            slaughters = nbt.getInt("slaughters");
        }

        @Override
        public void writeSyncNBT(CompoundTag nbt)
        {
            nbt.putBoolean("closed", stall.closed);
            if(!trough.getFood().isEmpty())
                nbt.put("food", trough.getFood().save(new CompoundTag()));
            nbt.putInt("slaughters", slaughters);
        }

        @Override
        public void readSyncNBT(CompoundTag nbt)
        {
            stall.closed = nbt.getBoolean("closed");
            trough.setFood(nbt.contains("food", Tag.TAG_COMPOUND)?ItemStack.of(nbt.getCompound("food")): ItemStack.EMPTY);
            int newSlaughters = nbt.getInt("slaughters");
            if(!synced)
            {
                // First data received: no animation
                gate = prevGate = stall.closed?1: 0;
                synced = true;
            }
            else if(newSlaughters!=slaughters)
                arcTicks = ARC_TIME;
            slaughters = newSlaughters;
        }
    }
}
