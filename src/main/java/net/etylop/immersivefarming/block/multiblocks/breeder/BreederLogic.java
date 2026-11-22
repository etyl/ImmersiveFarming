package net.etylop.immersivefarming.block.multiblocks.breeder;

import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import blusunrize.immersiveengineering.api.tool.conveyor.ConveyorHandler;
import net.etylop.immersivefarming.block.multiblocks.AnimalStall;
import net.etylop.immersivefarming.block.multiblocks.TroughHandler;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.entity.Livestock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ItemParticleOption;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.animal.Animal;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
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
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

import static net.etylop.immersivefarming.block.multiblocks.breeder.BreederShapes.STALL_MAX_X;
import static net.etylop.immersivefarming.block.multiblocks.breeder.BreederShapes.STALL_MAX_Z;
import static net.etylop.immersivefarming.block.multiblocks.breeder.BreederShapes.STALL_MIN_X;
import static net.etylop.immersivefarming.block.multiblocks.breeder.BreederShapes.STALL_MIN_Z;

/**
 * The animal breeder: two stalls side by side, each behind a gate at the back of the machine, and a trough shared by
 * both stalls at the front.
 * <ul>
 * <li>An open gate takes in the adult animal standing in its doorway (it is walked to the middle of the stall),
 * and closes behind it. The second stall only takes a partner the first animal can breed with.
 * Right clicking the back of the machine opens or closes the gate on that side: opening a gate lets its animal
 * out, a gate closed on an empty stall keeps animals out.</li>
 * <li>The animals are real entities, kept apart by the middle section and held in their stalls by the shapes of the
 * multiblock: nothing is stored in the machine but who they are (see {@link AnimalStall}).</li>
 * <li>The trough is filled through the feed input above it, at the top of the machine (by hoppers, conveyors, or
 * items dropped on it), and as a feeding trough: by hand, and from its sides.</li>
 * <li>When both animals are ready to breed, each eats from the trough, and after a while the newborn comes out of
 * the hatch between the two gates: it is put down right behind the machine, on the conveyor if there is one.
 * The parents then rest, as after any breeding (vanilla cooldown), and start again.</li>
 * </ul>
 * Positions are in multiblock coordinates: x from left to right (0 to 4), z from the gates (0) to the trough (3).
 */
public class BreederLogic implements IMultiblockLogic<BreederLogic.State>, IServerTickableComponent<BreederLogic.State>,
        IClientTickableComponent<BreederLogic.State>
{
    public static final int STALLS = 2;
    /** Items held by the trough (of a single kind of food) */
    public static final int TROUGH_CAPACITY = 64;
    private static final BlockPos TROUGH_POS = new BlockPos(1, 0, 2);
    private static final BlockPos HATCH_POS = new BlockPos(1, 0, 0);
    /** The blocks of the trough, and those above them */
    private static final int TROUGH_Z = 2;
    /** Top of the feed input, above the trough */
    private static final CapabilityPosition FEED_INPUT = new CapabilityPosition(1, 1, TROUGH_Z, RelativeBlockFace.UP);

    /** How far behind an open gate an animal is taken in (blocks) */
    private static final double APPROACH = 0.875;
    /** Where an animal stands in its stall (z, in pixels): its head over the trough, clear of the gate */
    private static final double STAND_Z = 24;
    /** Animals that fit in a stall, and under an open gate */
    private static final float MAX_WIDTH = 1.1F;
    private static final float MAX_HEIGHT = BreederShapes.GATE_CLEARANCE/16F;
    /** The two blocks behind the hatch (which is astride them), where the newborn is put down */
    private static final BlockPos[] OUTPUT_BLOCKS = {new BlockPos(1, 0, -1), new BlockPos(2, 0, -1)};
    /** Room the newborn needs there (blocks) */
    private static final double OUTPUT_WIDTH = 0.9;
    private static final double OUTPUT_HEIGHT = 0.8;
    /** The newborn is put down on what is behind the hatch (a conveyor, a carpet, a slab), up to this height */
    private static final double OUTPUT_MAX_STEP = 0.5;
    /** Ticks the hatch stays open after a birth (client) */
    private static final int FLAP_TIME = 30;

    /** Where the entities spawned by the breeding in progress are moved (server thread), see {@link #onEntityJoin} */
    @Nullable
    private static Birth birth;

    // ------------------------------------------------------------------ ticking

    @Override
    public void tickServer(IMultiblockContext<State> context)
    {
        final State state = context.getState();
        final IMultiblockLevel level = context.getLevel();
        state.locate(level);

        boolean sync = false;
        for(int i = 0; i < STALLS; i++)
            sync |= tickStall(state, level, i);
        if(level.shouldTickModulo(8))
            state.trough.collect(level.getRawLevel(), state.feedAreas);
        sync |= tickBreeding(context, state, level);
        if(sync)
            context.markDirtyAndSync();
    }

    /** @return whether the clients must be told of a change */
    private boolean tickStall(State state, IMultiblockLevel level, int index)
    {
        // The second stall only takes a partner the first animal can breed with
        Animal other = state.stalls[1-index].getAnimal();
        AnimalStall.Change change = state.stalls[index].tick(level.getRawLevel(), state.places[index],
                animal -> isAccepted(animal)&&animal!=other&&(other==null||canBreed(animal, other)));
        if(change==AnimalStall.Change.NONE)
            return false;
        playSound(level, gatePos(index), change==AnimalStall.Change.TAKEN?SoundEvents.IRON_TRAPDOOR_CLOSE: SoundEvents.IRON_TRAPDOOR_OPEN);
        return true;
    }

    private boolean tickBreeding(IMultiblockContext<State> context, State state, IMultiblockLevel level)
    {
        Animal first = state.stalls[0].getAnimal();
        Animal second = state.stalls[1].getAnimal();
        if(first==null||second==null)
        {
            state.progress = 0;
            return false;
        }
        if(state.progress > 0)
        {
            state.progress++;
            if(state.progress%20==0)
            {
                context.markMasterDirty();
                // Hearts, as animals in love
                level.getRawLevel().broadcastEntityEvent(state.progress%40==0?first: second, (byte)18);
            }
            if(state.progress < getBreedingTime())
                return false;
            Vec3 output = findOutput(level);
            if(output==null)
            {
                // Something was put behind the hatch in the meantime: the newborn waits for it to be removed
                state.progress = getBreedingTime()-1;
                return false;
            }
            state.progress = 0;
            if(!breed(level, first, second, output))
                return false;
            state.births++;
            return true;
        }
        if(!level.shouldTickModulo(20)||getStatus(context, state)!=Status.READY)
            return false;
        // One meal each
        int meals = getFoodPerAnimal();
        ItemStack food = state.trough.getFood().copyWithCount(1);
        state.trough.extractItem(0, 2*meals, false);
        eat(first, food);
        eat(second, food);
        state.progress = 1;
        return true;
    }

    /**
     * Breeds the two animals as the vanilla breeding does (offspring, events, cooldown of the parents, experience),
     * what it spawns comes out behind the hatch instead of appearing next to the parents.
     */
    private static boolean breed(IMultiblockLevel level, Animal first, Animal second, Vec3 output)
    {
        if(!(level.getRawLevel() instanceof ServerLevel serverLevel))
            return false;
        // Vanilla animals only breed while in love
        first.setInLoveTime(600);
        second.setInLoveTime(600);
        birth = new Birth(output, level.getOrientation().front().toYRot());
        try
        {
            first.spawnChildFromBreeding(serverLevel, second);
        } finally
        {
            birth = null;
        }
        first.resetLove();
        second.resetLove();
        // Animals whose breeding does not set the vanilla cooldown would breed without ever resting
        if(first.getAge()==0)
            first.setAge(6000);
        if(second.getAge()==0)
            second.setAge(6000);
        playSound(level, HATCH_POS, SoundEvents.WOOL_PLACE);
        return true;
    }

    /**
     * Called for every entity added to a level: those spawned by a breeding in progress (the newborn, the experience,
     * or the egg some animals lay instead) are moved behind the hatch before they are added.
     */
    public static void onEntityJoin(Entity entity, Level level)
    {
        Birth here = birth;
        if(here==null||level.isClientSide||entity.tickCount!=0)
            return;
        entity.moveTo(here.output.x, here.output.y, here.output.z, here.yaw, 0);
        entity.setDeltaMovement(Vec3.ZERO);
    }

    /** Where a breeder puts what it breeds, its back to the machine */
    private record Birth(Vec3 output, float yaw)
    {
    }

    /**
     * Where the newborn is put down: right behind the hatch, on top of what is there. The hatch is astride two
     * blocks: if one of them is a conveyor, the newborn is put in the middle of it, so that it is carried away.
     *
     * @return null if there is no room for it
     */
    @Nullable
    private static Vec3 findOutput(IMultiblockLevel level)
    {
        final Level rawLevel = level.getRawLevel();
        double conveyorX = Double.NaN;
        double conveyorTop = 0;
        double highest = 0;
        for(BlockPos behindHatch : OUTPUT_BLOCKS)
        {
            BlockPos pos = level.toAbsolute(behindHatch);
            BlockState block = rawLevel.getBlockState(pos);
            // Asked with a context: without one, the answer comes from a cache made without the block entity,
            // where a conveyor has no shape at all
            VoxelShape shape = block.getCollisionShape(rawLevel, pos, CollisionContext.empty());
            double top = shape.isEmpty()?0: shape.max(Direction.Axis.Y);
            highest = Math.max(highest, top);
            if(Double.isNaN(conveyorX)&&ConveyorHandler.isConveyorBlock(block.getBlock()))
            {
                conveyorX = behindHatch.getX()+0.5;
                conveyorTop = top;
            }
        }
        boolean onConveyor = !Double.isNaN(conveyorX);
        double x = onConveyor?conveyorX: OUTPUT_BLOCKS[1].getX();
        double y = onConveyor?conveyorTop: highest;
        double z = OUTPUT_BLOCKS[0].getZ()+0.5;
        if(y > OUTPUT_MAX_STEP)
            return null;
        AABB room = level.toAbsolute(new AABB(x-OUTPUT_WIDTH/2, y, z-OUTPUT_WIDTH/2, x+OUTPUT_WIDTH/2, y+OUTPUT_HEIGHT, z+OUTPUT_WIDTH/2));
        if(!rawLevel.noCollision(room.deflate(1e-4)))
            return null;
        return level.toAbsolute(new Vec3(x, y, z));
    }

    private static void eat(Animal animal, ItemStack food)
    {
        animal.playSound(animal.getEatingSound(food), 1, 1);
        if(animal.level() instanceof ServerLevel level)
            level.sendParticles(new ItemParticleOption(ParticleTypes.ITEM, food), animal.getX(),
                    animal.getY()+animal.getBbHeight()*0.6, animal.getZ(), 8, 0.15, 0.1, 0.15, 0.05);
    }

    // ------------------------------------------------------------------ animals

    /** Adults that fit in a stall, on their own feet. Leashed animals are left to whoever leads them */
    private static boolean isAccepted(Animal animal)
    {
        return AnimalStall.isFree(animal)&&!animal.isBaby()&&animal.getBbWidth() <= MAX_WIDTH&&animal.getBbHeight() <= MAX_HEIGHT;
    }

    /**
     * Whether these two animals can have offspring, whatever their mood: asks the animals themselves, as tamed,
     * sitting or mixed animals have their own rules. Vanilla requires both to be in love, which they are for the
     * time of the question.
     */
    private static boolean canBreed(Animal first, Animal second)
    {
        int firstLove = first.getInLoveTime();
        int secondLove = second.getInLoveTime();
        first.setInLoveTime(600);
        second.setInLoveTime(600);
        try
        {
            return first.canMate(second);
        } catch(RuntimeException e)
        {
            return false;
        } finally
        {
            first.setInLoveTime(firstLove);
            second.setInLoveTime(secondLove);
        }
    }

    // ------------------------------------------------------------------ status

    public enum Status
    {
        WAITING, WAITING_PARTNER, INCOMPATIBLE, RESTING, NO_FOOD, WRONG_FOOD, OUTPUT_BLOCKED, READY, BREEDING
    }

    /** What the breeder is doing, or waiting for (server side) */
    public static Status getStatus(IMultiblockContext<State> context, State state)
    {
        Animal first = state.stalls[0].getAnimal();
        Animal second = state.stalls[1].getAnimal();
        if(first==null&&second==null)
            return Status.WAITING;
        if(first==null||second==null)
            return Status.WAITING_PARTNER;
        if(state.progress > 0)
            return Status.BREEDING;
        if(!canBreed(first, second))
            return Status.INCOMPATIBLE;
        if(first.getAge()!=0||second.getAge()!=0)
            return Status.RESTING;
        ItemStack food = state.trough.getFood();
        if(food.isEmpty())
            return Status.NO_FOOD;
        if(!first.isFood(food)||!second.isFood(food))
            return Status.WRONG_FOOD;
        if(food.getCount() < 2*getFoodPerAnimal())
            return Status.NO_FOOD;
        if(findOutput(context.getLevel())==null)
            return Status.OUTPUT_BLOCKED;
        return Status.READY;
    }

    private static int getBreedingTime()
    {
        return Math.max(1, IFConfig.get().animals.breeder_time.get()*20);
    }

    private static int getFoodPerAnimal()
    {
        return IFConfig.get().animals.breeder_food.get();
    }

    // ------------------------------------------------------------------ interaction

    @Override
    public InteractionResult click(
            IMultiblockContext<State> ctx, BlockPos posInMultiblock, Player player, InteractionHand hand,
            BlockHitResult absoluteHit, boolean isClient
    )
    {
        int gate = BreederShapes.getGate(posInMultiblock);
        // The blocks of the trough, not those above them
        boolean trough = posInMultiblock.getZ()==TROUGH_Z&&posInMultiblock.getY()==0;
        // The trough and the gates are all there is to use
        if(!trough&&gate < 0)
            return InteractionResult.PASS;
        // What the trough does not take is used as on any other block, and so is what is held when sneaking: a block
        // is placed against the machine. Sneaking only uses the trough, to put a whole stack of food in it
        ItemStack held = player.getItemInHand(hand);
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
            boolean closed = state.stalls[gate].toggle(level.getRawLevel(), state.places[gate], BreederLogic::isAccepted);
            playSound(level, gatePos(gate), closed?SoundEvents.IRON_TRAPDOOR_CLOSE: SoundEvents.IRON_TRAPDOOR_OPEN);
            ctx.markDirtyAndSync();
        }
        return InteractionResult.CONSUME;
    }

    private static BlockPos gatePos(int stall)
    {
        return BreederShapes.getGatePos(stall);
    }

    private static void playSound(IMultiblockLevel level, BlockPos posInMultiblock, SoundEvent sound)
    {
        level.getRawLevel().playSound(null, level.toAbsolute(posInMultiblock), sound, SoundSource.BLOCKS, 1, 1);
    }

    @Override
    public <T> LazyOptional<T> getCapability(IMultiblockContext<State> ctx, CapabilityPosition position, Capability<T> cap)
    {
        // The trough is filled through the top of the feed input, and from any of its sides, as a feeding trough
        final BlockPos pos = position.posInMultiblock();
        if(cap==ForgeCapabilities.ITEM_HANDLER&&(FEED_INPUT.equalsOrNullFace(position)||(pos.getZ()==TROUGH_Z&&pos.getY()==0)))
            return ctx.getState().troughInput.cast(ctx);
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
        return pos -> BreederShapes.getShape(pos, forType);
    }

    @Override
    public VoxelShape postProcessAbsoluteShape(
            IMultiblockContext<State> ctx, VoxelShape defaultShape, CollisionContext shapeCtx, BlockPos posInMultiblock, ShapeType type
    )
    {
        int stall = BreederShapes.getGate(posInMultiblock);
        if(stall < 0)
            return defaultShape;
        VoxelShape gate = BreederShapes.getGateShape(posInMultiblock, ctx.getState().stalls[stall].closed, ctx.getLevel().getOrientation(), type);
        return gate.isEmpty()?defaultShape: Shapes.joinUnoptimized(defaultShape, gate, BooleanOp.OR);
    }

    // ------------------------------------------------------------------ client

    @Override
    public void tickClient(IMultiblockContext<State> context)
    {
        final State state = context.getState();
        for(int i = 0; i < STALLS; i++)
        {
            float target = state.stalls[i].closed?1: 0;
            state.prevGate[i] = state.gate[i];
            state.gate[i] += Mth.clamp(target-state.gate[i], -0.1F, 0.1F);
        }
        if(state.flapTicks > 0)
            state.flapTicks--;
        state.prevFlap = state.flap;
        state.flap += Mth.clamp((state.flapTicks > 0?1: 0)-state.flap, -0.2F, 0.2F);
    }

    @Override
    public State createInitialState(IInitialMultiblockContext<State> capabilitySource)
    {
        return new State(capabilitySource);
    }

    // ------------------------------------------------------------------ state

    public static class State implements IMultiblockState
    {
        public final AnimalStall[] stalls = {new AnimalStall(), new AnimalStall()};
        public final TroughHandler trough;
        /** Ticks since the animals ate, 0 when they are not breeding */
        private int progress;
        /** Number of births, the hatch opens when it changes (synced) */
        private int births;

        // Client only
        /** How far each gate is closed, from 0 (open) to 1 */
        public final float[] gate = new float[STALLS];
        public final float[] prevGate = new float[STALLS];
        /** How far the hatch is open, from 0 to 1 */
        public float flap;
        public float prevFlap;
        private int flapTicks;
        private boolean synced;

        // Absolute positions, computed once (server only)
        private boolean located;
        private final AnimalStall.Place[] places = new AnimalStall.Place[STALLS];
        /** Where dropped food is taken: on the feed input, and in the trough */
        private AABB[] feedAreas;

        private final Supplier<Level> level;
        private final StoredCapability<IItemHandler> troughInput;

        public State(IInitialMultiblockContext<State> ctx)
        {
            final Runnable markDirty = ctx.getMarkDirtyRunnable();
            final Runnable sync = ctx.getSyncRunnable();
            this.level = ctx.levelSupplier();
            this.trough = new TroughHandler(TROUGH_CAPACITY, this::isFeed, () -> {
                markDirty.run();
                sync.run();
            });
            this.troughInput = new StoredCapability<>(trough.insertOnly());
        }

        private void locate(IMultiblockLevel level)
        {
            if(located)
                return;
            // The front of the multiblock (in IE's terms) is the direction its gates face
            float troughYaw = Mth.wrapDegrees(level.getOrientation().front().toYRot()+180);
            for(int i = 0; i < STALLS; i++)
                places[i] = new AnimalStall.Place(
                        level.toAbsolute(BreederShapes.px(STALL_MIN_X[i], 0, STALL_MIN_Z, STALL_MAX_X[i], 32, STALL_MAX_Z)),
                        level.toAbsolute(BreederShapes.px(STALL_MIN_X[i], 0, -APPROACH*16, STALL_MAX_X[i], 32, STALL_MAX_Z)),
                        level.toAbsolute(new Vec3((STALL_MIN_X[i]+STALL_MAX_X[i])/32D, 1/16D, STAND_Z/16)),
                        troughYaw
                );
            feedAreas = new AABB[]{
                    level.toAbsolute(BreederShapes.px(BreederShapes.INPUT_MIN_X, 32-2, BreederShapes.INPUT_MIN_Z,
                            BreederShapes.INPUT_MAX_X, 32+8, BreederShapes.INPUT_MAX_Z)),
                    level.toAbsolute(BreederShapes.px(2, 10, STALL_MAX_Z, 62, 20, 48))
            };
            located = true;
        }

        /** Food of the animals that need to be fed, and whatever the animals in the stalls eat */
        private boolean isFeed(ItemStack stack)
        {
            if(Livestock.isFeed(stack, level.get()))
                return true;
            for(AnimalStall stall : stalls)
            {
                Animal animal = stall.getAnimal();
                if(animal!=null&&animal.isFood(stack))
                    return true;
            }
            return false;
        }

        /** Client: the food shown in the trough */
        public ItemStack getDisplayFood()
        {
            return trough.getFood();
        }

        @Override
        public void writeSaveNBT(CompoundTag nbt)
        {
            for(int i = 0; i < STALLS; i++)
                nbt.put("stall"+i, stalls[i].save());
            nbt.put("food", trough.getFood().save(new CompoundTag()));
            nbt.putInt("progress", progress);
            nbt.putInt("births", births);
        }

        @Override
        public void readSaveNBT(CompoundTag nbt)
        {
            for(int i = 0; i < STALLS; i++)
                stalls[i].load(nbt.getCompound("stall"+i));
            trough.setFood(ItemStack.of(nbt.getCompound("food")));
            progress = nbt.getInt("progress");
            births = nbt.getInt("births");
        }

        @Override
        public void writeSyncNBT(CompoundTag nbt)
        {
            for(int i = 0; i < STALLS; i++)
            {
                nbt.putBoolean("closed"+i, stalls[i].closed);
            }
            if(!trough.getFood().isEmpty())
                nbt.put("food", trough.getFood().save(new CompoundTag()));
            nbt.putInt("births", births);
        }

        @Override
        public void readSyncNBT(CompoundTag nbt)
        {
            for(int i = 0; i < STALLS; i++)
            {
                stalls[i].closed = nbt.getBoolean("closed"+i);
            }
            trough.setFood(nbt.contains("food", Tag.TAG_COMPOUND)?ItemStack.of(nbt.getCompound("food")): ItemStack.EMPTY);
            int newBirths = nbt.getInt("births");
            if(!synced)
            {
                // First data received: no animation
                for(int i = 0; i < STALLS; i++)
                    gate[i] = prevGate[i] = stalls[i].closed?1: 0;
                synced = true;
            }
            else if(newBirths!=births)
                flapTicks = FLAP_TIME;
            births = newBirths;
        }
    }
}
