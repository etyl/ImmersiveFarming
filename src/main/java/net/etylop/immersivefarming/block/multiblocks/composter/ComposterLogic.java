package net.etylop.immersivefarming.block.multiblocks.composter;

import blusunrize.immersiveengineering.api.crafting.FluidTagInput;
import blusunrize.immersiveengineering.api.energy.AveragingEnergyStorage;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IClientTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.IServerTickableComponent;
import blusunrize.immersiveengineering.api.multiblocks.blocks.component.RedstoneControl.RSState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IInitialMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockContext;
import blusunrize.immersiveengineering.api.multiblocks.blocks.env.IMultiblockLevel;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockLogic;
import blusunrize.immersiveengineering.api.multiblocks.blocks.logic.IMultiblockState;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.CapabilityPosition;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MBInventoryUtils;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.MultiblockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.RelativeBlockFace;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.ShapeType;
import blusunrize.immersiveengineering.api.multiblocks.blocks.util.StoredCapability;
import blusunrize.immersiveengineering.api.utils.CapabilityReference;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcess;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcessInMachine;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.MultiblockProcessor.InMachineProcessor;
import blusunrize.immersiveengineering.common.blocks.multiblocks.process.ProcessContext.ProcessContextInMachine;
import blusunrize.immersiveengineering.common.fluids.ArrayFluidHandler;
import blusunrize.immersiveengineering.common.util.IESounds;
import blusunrize.immersiveengineering.common.util.Utils;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraint;
import blusunrize.immersiveengineering.common.util.inventory.SlotwiseItemHandler.IOConstraintGroup;
import blusunrize.immersiveengineering.common.util.sound.MultiblockSound;
import net.etylop.immersivefarming.api.crafting.ComposterRecipe;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.IFluidTank;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.IFluidHandler.FluidAction;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.IItemHandlerModifiable;

import java.util.List;
import java.util.function.BiFunction;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

public class ComposterLogic implements IMultiblockLogic<ComposterLogic.State>, IServerTickableComponent<ComposterLogic.State>,
        IClientTickableComponent<ComposterLogic.State>
{
    public static final BlockPos REDSTONE_POS = new BlockPos(2, 1, 2);
    private static final CapabilityPosition ENERGY_INPUT = new CapabilityPosition(0, 1, 2, RelativeBlockFace.UP);
    private static final CapabilityPosition FLUID_INPUT = new CapabilityPosition(1, 0, 2, RelativeBlockFace.BACK);
    private static final BlockPos ITEM_INPUT = new BlockPos(0, 2, 1);
    // Block next to the multiblock where the produced items are inserted or dropped
    private static final MultiblockFace ITEM_OUTPUT = new MultiblockFace(3, 1, 1, RelativeBlockFace.LEFT);

    public static final int NUM_SLOTS = 1;
    public static final int INPUT_SLOT = 0;
    public static final int ENERGY_CAPACITY = 16000;
    public static final int TANK_VOLUME = 4*FluidType.BUCKET_VOLUME;

    @Override
    public void tickServer(IMultiblockContext<State> context)
    {
        final State state = context.getState();
        final IMultiblockLevel level = context.getLevel();
        final boolean rsEnabled = state.rsState.isEnabled(context);

        final boolean active = state.processor.tickServer(state, level, rsEnabled);
        boolean update = false;
        if(rsEnabled&&state.energy.getEnergyStored() > 0&&state.processor.getQueueSize()==0)
        {
            final Level rawLevel = level.getRawLevel();
            ComposterRecipe recipe = ComposterRecipe.findRecipe(rawLevel, state.getTankFluids(), state.inventory.getStackInSlot(INPUT_SLOT));
            if(recipe!=null)
            {
                MultiblockProcessInMachine<ComposterRecipe> process = new MultiblockProcessComposter(recipe).setInputTanks(0);
                update = state.processor.addProcessToQueue(process, rawLevel, false);
            }
        }

        if(update||active!=state.isActive)
        {
            state.isActive = active;
            context.markDirtyAndSync();
        }
    }

    @Override
    public void tickClient(IMultiblockContext<State> context)
    {
        final State state = context.getState();
        if(state.isActive&&!state.isSoundPlaying.getAsBoolean())
        {
            final Vec3 soundPos = context.getLevel().toAbsolute(new Vec3(1.5, 2, 1.5));
            state.isSoundPlaying = MultiblockSound.startSound(
                    () -> state.isActive, context.isValid(), soundPos, IESounds.mixer, 0.075f
            );
        }
    }

    @Override
    public State createInitialState(IInitialMultiblockContext<State> capabilitySource)
    {
        return new State(capabilitySource);
    }

    @Override
    public <T> LazyOptional<T> getCapability(IMultiblockContext<State> ctx, CapabilityPosition position, Capability<T> cap)
    {
        final State state = ctx.getState();
        if(cap==ForgeCapabilities.ENERGY&&ENERGY_INPUT.equalsOrNullFace(position))
            return state.energyCap.cast(ctx);
        else if(cap==ForgeCapabilities.FLUID_HANDLER&&FLUID_INPUT.equalsOrNullFace(position))
            return state.fluidInput.cast(ctx);
        else if(cap==ForgeCapabilities.ITEM_HANDLER&&ITEM_INPUT.equals(position.posInMultiblock()))
            return state.itemInput.cast(ctx);
        return LazyOptional.empty();
    }

    @Override
    public void dropExtraItems(State state, Consumer<ItemStack> drop)
    {
        MBInventoryUtils.dropItems(state.inventory, drop);
    }

    @Override
    public Function<BlockPos, VoxelShape> shapeGetter(ShapeType forType)
    {
        return ComposterLogic::getShape;
    }

    private static VoxelShape getShape(BlockPos posInMultiblock)
    {
        if(new BlockPos(2, 0, 2).equals(posInMultiblock))
            return Shapes.or(
                    Shapes.box(0, 0, 0, 1, .5f, 1),
                    Shapes.box(0.125, .5f, 0.625, 0.25, 1, 0.875),
                    Shapes.box(0.75, .5f, 0.625, 0.875, 1, 0.875)
            );
        else if(posInMultiblock.getY()==0&&posInMultiblock.getZ() < 2)
        {
            VoxelShape base = Shapes.box(0, 0, 0, 1, .5f, 1);
            if(new BlockPos(0, 0, 0).equals(posInMultiblock))
                return Shapes.or(base, Shapes.box(4*0.0625, 0.5, 3*0.0625, 8*0.0625, 1, 7*0.0625));
            else if(new BlockPos(0, 0, 1).equals(posInMultiblock))
                return Shapes.or(base, Shapes.box(4*0.0625, 0.5, 9*0.0625, 8*0.0625, 1, 13*0.0625));
            else if(new BlockPos(2, 0, 0).equals(posInMultiblock))
                return Shapes.or(base, Shapes.box(8*0.0625, 0.5, 3*0.0625, 12*0.0625, 1, 7*0.0625));
            else if(new BlockPos(2, 0, 1).equals(posInMultiblock))
                return Shapes.or(base, Shapes.box(8*0.0625, 0.5, 9*0.0625, 12*0.0625, 1, 13*0.0625));
            return base;
        }
        else if(new BlockPos(2, 1, 2).equals(posInMultiblock))
            return Shapes.box(0, 0, 0.5, 1, 1, 1);
        else if(new BlockPos(1, 1, 2).equals(posInMultiblock))
            return Shapes.box(4*0.0625, 0, 4*0.0625, 12*0.0625, 1, 12*0.0625);
        else if(new BlockPos(1, 2, 2).equals(posInMultiblock))
            return Shapes.box(4*0.0625, 0, 4*0.0625, 12*0.0625, 1, 12*0.0625);
        else if(new BlockPos(1, 3, 2).equals(posInMultiblock))
            return Shapes.box(4*0.0625, 0, 0, 12*0.0625, 12*0.0625, 12*0.0625);
        else if(new BlockPos(1, 3, 1).equals(posInMultiblock))
            return Shapes.box(4*0.0625, 0, 4*0.0625, 12*0.0625, 12*0.0625, 15*0.0625);
        else
            return Shapes.block();
    }

    public static class State implements IMultiblockState, ProcessContextInMachine<ComposterRecipe>
    {
        public final AveragingEnergyStorage energy = new AveragingEnergyStorage(ENERGY_CAPACITY);
        public final FluidTank[] tanks = new FluidTank[]{
                new FluidTank(TANK_VOLUME, f -> f.getFluid().is(FluidTags.WATER)), // water tank
                new FluidTank(TANK_VOLUME), // wet matter tank
                new FluidTank(TANK_VOLUME)  // dry matter tank
        };
        public final SlotwiseItemHandler inventory;
        public final InMachineProcessor<ComposterRecipe> processor;
        public final RSState rsState = RSState.enabledByDefault();

        // Client only
        public boolean isActive;
        private BooleanSupplier isSoundPlaying = () -> false;

        // Util
        private final CapabilityReference<IItemHandler> outputRef;
        private final StoredCapability<IFluidHandler> fluidInput;
        private final StoredCapability<IItemHandler> itemInput;
        private final StoredCapability<IEnergyStorage> energyCap;

        public State(IInitialMultiblockContext<State> ctx)
        {
            final Runnable markDirty = ctx.getMarkDirtyRunnable();
            this.inventory = SlotwiseItemHandler.makeWithGroups(
                    List.of(new IOConstraintGroup(IOConstraint.ANY_INPUT, NUM_SLOTS)), markDirty
            );
            this.processor = new InMachineProcessor<>(
                    1, 0, 1, markDirty, ComposterRecipe.RECIPES::getById
            );
            this.outputRef = ctx.getCapabilityAt(ForgeCapabilities.ITEM_HANDLER, ITEM_OUTPUT);
            this.fluidInput = new StoredCapability<>(ArrayFluidHandler.fillOnly(tanks[0], markDirty));
            this.itemInput = new StoredCapability<>(this.inventory);
            this.energyCap = new StoredCapability<>(this.energy);
        }

        private FluidStack[] getTankFluids()
        {
            return new FluidStack[]{tanks[0].getFluid(), tanks[1].getFluid(), tanks[2].getFluid()};
        }

        private void doProcessOutput(ItemStack output, IMultiblockLevel level)
        {
            output = Utils.insertStackIntoInventory(outputRef, output, false);
            if(!output.isEmpty())
                Utils.dropStackAtPos(
                        level.getRawLevel(), level.toAbsolute(ITEM_OUTPUT.posInMultiblock()), output,
                        level.toAbsolute(ITEM_OUTPUT.face())
                );
        }

        @Override
        public void writeSaveNBT(CompoundTag nbt)
        {
            nbt.put("tankWater", tanks[0].writeToNBT(new CompoundTag()));
            nbt.put("tankWetMatter", tanks[1].writeToNBT(new CompoundTag()));
            nbt.put("tankDryMatter", tanks[2].writeToNBT(new CompoundTag()));
            nbt.put("inventory", inventory.serializeNBT());
            nbt.put("energy", energy.serializeNBT());
            nbt.put("processor", processor.toNBT());
        }

        @Override
        public void readSaveNBT(CompoundTag nbt)
        {
            tanks[0].readFromNBT(nbt.getCompound("tankWater"));
            tanks[1].readFromNBT(nbt.getCompound("tankWetMatter"));
            tanks[2].readFromNBT(nbt.getCompound("tankDryMatter"));
            inventory.deserializeNBT(nbt.getCompound("inventory"));
            if(nbt.contains("energy"))
                energy.deserializeNBT(nbt.get("energy"));
            processor.fromNBT(nbt.get("processor"), MultiblockProcessComposter::new);
        }

        @Override
        public void writeSyncNBT(CompoundTag nbt)
        {
            nbt.putBoolean("isActive", isActive);
        }

        @Override
        public void readSyncNBT(CompoundTag nbt)
        {
            isActive = nbt.getBoolean("isActive");
        }

        @Override
        public AveragingEnergyStorage getEnergy()
        {
            return energy;
        }

        @Override
        public IItemHandlerModifiable getInventory()
        {
            return inventory.getRawHandler();
        }

        @Override
        public IFluidTank[] getInternalTanks()
        {
            return new IFluidTank[]{tanks[0]};
        }

        @Override
        public int[] getOutputTanks()
        {
            return new int[]{0};
        }
    }

    public static class MultiblockProcessComposter extends MultiblockProcessInMachine<ComposterRecipe>
    {
        public MultiblockProcessComposter(ComposterRecipe recipe)
        {
            super(recipe, INPUT_SLOT);
        }

        public MultiblockProcessComposter(BiFunction<Level, ResourceLocation, ComposterRecipe> getRecipe, CompoundTag data)
        {
            super(getRecipe, data);
        }

        @Override
        protected List<FluidStack> getRecipeFluidOutputs(Level level)
        {
            return List.of();
        }

        @Override
        protected List<FluidTagInput> getRecipeFluidInputs(ProcessContextInMachine<ComposterRecipe> context, Level level)
        {
            return List.of();
        }

        @Override
        public boolean canProcess(ProcessContextInMachine<ComposterRecipe> context, Level level)
        {
            LevelDependentData<ComposterRecipe> levelData = getLevelData(level);
            if(levelData.recipe()==null)
                return true;
            if(!(context instanceof State composter))
                return false;
            if(composter.energy.extractEnergy(levelData.energyPerTick(), true)!=levelData.energyPerTick())
                return false;
            ComposterRecipe recipe = levelData.recipe();
            if(recipe.fluidProduct)
            {
                // organic item -> wet/dry matter: the produced fluids must fit in their tanks
                FluidStack[] outputs = recipe.getFluidOutput();
                return canFill(composter.tanks[1], outputs[0])&&canFill(composter.tanks[2], outputs[1]);
            }
            // water + wet + dry matter -> compost: all three fluids must still be available
            return recipe.matches(composter.getTankFluids());
        }

        private static boolean canFill(FluidTank tank, FluidStack fluid)
        {
            return fluid==null||fluid.isEmpty()||tank.fill(fluid, FluidAction.SIMULATE)==fluid.getAmount();
        }

        @Override
        public void doProcessTick(ProcessContextInMachine<ComposterRecipe> context, IMultiblockLevel level)
        {
            LevelDependentData<ComposterRecipe> levelData = getLevelData(level.getRawLevel());
            if(levelData.recipe()==null)
            {
                this.clearProcess = true;
                return;
            }
            State composter = (State)context;
            ComposterRecipe recipe = levelData.recipe();
            if(this.processTick==recipe.getTotalProcessTime()-1)
            {
                if(recipe.fluidProduct)
                {
                    FluidStack[] fluidOuputs = recipe.getFluidOutput();
                    if(fluidOuputs[0]!=null)
                        composter.tanks[1].fill(fluidOuputs[0], FluidAction.EXECUTE);
                    if(fluidOuputs[1]!=null)
                        composter.tanks[2].fill(fluidOuputs[1], FluidAction.EXECUTE);
                }
                else
                {
                    FluidTagInput[] fluidInputs = recipe.getFluidInput();
                    for(int i = 0; i < 3; i++)
                        composter.tanks[i].drain(fluidInputs[i].getAmount(), FluidAction.EXECUTE);
                    ItemStack outputItems = recipe.itemOutput.getMatchingStacks()[0];
                    composter.doProcessOutput(outputItems.copy(), level);
                }
            }
            super.doProcessTick(context, level);
        }
    }

    public static MultiblockProcess<ComposterRecipe, ?> getActiveProcess(State state)
    {
        final List<MultiblockProcess<ComposterRecipe, ProcessContextInMachine<ComposterRecipe>>> queue = state.processor.getQueue();
        return queue.isEmpty()?null: queue.get(0);
    }
}
