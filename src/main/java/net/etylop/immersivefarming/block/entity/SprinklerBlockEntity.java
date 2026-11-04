package net.etylop.immersivefarming.block.entity;

import blusunrize.immersiveengineering.api.IEEnums;
import blusunrize.immersiveengineering.api.IEProperties;
import blusunrize.immersiveengineering.api.Lib;
import blusunrize.immersiveengineering.api.fluid.IFluidPipe;
import blusunrize.immersiveengineering.api.utils.CapabilityReference;
import blusunrize.immersiveengineering.api.utils.DirectionUtils;
import blusunrize.immersiveengineering.client.utils.TextUtils;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlock;
import blusunrize.immersiveengineering.common.blocks.IEBaseBlockEntity;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockBounds;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IBlockOverlayText;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IConfigurableSides;
import blusunrize.immersiveengineering.common.blocks.IEBlockInterfaces.IHasDummyBlocks;
import blusunrize.immersiveengineering.common.blocks.metal.FluidPipeBlockEntity;
import blusunrize.immersiveengineering.common.blocks.ticking.IEClientTickableBE;
import blusunrize.immersiveengineering.common.blocks.ticking.IEServerTickableBE;
import blusunrize.immersiveengineering.common.config.IEClientConfig;
import blusunrize.immersiveengineering.common.util.ResettableCapability;
import blusunrize.immersiveengineering.common.util.Utils;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.fluid.IFFluids;
import net.etylop.immersivefarming.particle.IFParticles;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.fluids.capability.templates.FluidTank;
import org.jetbrains.annotations.Nullable;

import javax.annotation.Nonnull;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.Map;

public class SprinklerBlockEntity extends IEBaseBlockEntity implements IEServerTickableBE, IFluidPipe,
        IEClientTickableBE, IBlockBounds, IHasDummyBlocks, IConfigurableSides, IBlockOverlayText {

    public FluidTank tank = new FluidTank(FluidType.BUCKET_VOLUME);
    public Map<Direction, IEEnums.IOSideConfig> sideConfig = new EnumMap<>(Direction.class);
    {
        for(Direction d : DirectionUtils.VALUES)
        {
            if(d==Direction.DOWN)
                sideConfig.put(d, IEEnums.IOSideConfig.INPUT);
            else
                sideConfig.put(d, IEEnums.IOSideConfig.NONE);
        }
    }

    private final Map<Direction, ResettableCapability<IFluidHandler>> sidedFluidHandler = new EnumMap<>(Direction.class);

    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");
    public static final BooleanProperty USING_PESTICIDE = BooleanProperty.create("using_pesticide");

    private final Map<Direction, CapabilityReference<IFluidHandler>> neighborFluids = CapabilityReference.forAllNeighbors(
            this, ForgeCapabilities.FLUID_HANDLER
    );
    protected int WATER_CONSUMPTION = 1;

    public float sprinklerRotation = 0;
    protected float ROTATION_SPEED = 18;
    protected Block REFERENCE_BLOCK = IFBlocks.SPRINKLER.get();


    public SprinklerBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void readCustomNBT(CompoundTag nbt, boolean descPacket)
    {
        int[] sideConfigArray = nbt.getIntArray("sideConfig");
        for(Direction d : DirectionUtils.VALUES)
            sideConfig.put(d, IEEnums.IOSideConfig.VALUES[sideConfigArray[d.ordinal()]]);
        tank.readFromNBT(nbt.getCompound("tank"));
        if(descPacket)
            this.markContainingBlockForUpdate(null);
    }

    @Override
    public void writeCustomNBT(CompoundTag nbt, boolean descPacket)
    {
        int[] sideConfigArray = new int[6];
        for(Direction d : DirectionUtils.VALUES)
            sideConfigArray[d.ordinal()] = sideConfig.get(d).ordinal();
        nbt.putIntArray("sideConfig", sideConfigArray);
        nbt.put("tank", tank.writeToNBT(new CompoundTag()));
    }

    @Override
    public void tickServer() {
        if (isDummy())
            return;

        if (getLevelNonnull().getGameTime()%20!=0)
            return;

        if (!isRSPowered() && this.tank.getFluidAmount() >= WATER_CONSUMPTION)
        {
            tank.drain(WATER_CONSUMPTION, IFluidHandler.FluidAction.EXECUTE);

            if (this.tank.getFluid().getFluid() == IFFluids.TREATED_WATER_FLUID.get()) {
                setState(getBlockState().setValue(ACTIVE, true).setValue(USING_PESTICIDE, true));
            }
            else {
                setState(getBlockState().setValue(ACTIVE, true).setValue(USING_PESTICIDE, false));
            }
        }
        else
        {
            setState(getBlockState().setValue(ACTIVE, false));
        }


        if (this.tank.getFluidAmount() > 0)
        {
            int i = outputFluid(tank.getFluid(), IFluidHandler.FluidAction.EXECUTE);
            tank.drain(i, IFluidHandler.FluidAction.EXECUTE);
        }

        inputFluid(IFluidHandler.FluidAction.EXECUTE);
    }

    @Override
    public void tickClient()
    {
        if (isDummy()) {
            return;
        }
        if (getBlockState().getValue(ACTIVE)) {
            sprinklerRotation = (sprinklerRotation + ROTATION_SPEED) % 360;
            if (getLevelNonnull().getGameTime()%20==0)
            {
                spawnParticles();
                BlockPos pPos = getBlockPos().above();
                double d0 = pPos.getX() + 0.5D;
                double d1 = pPos.getY();
                double d2 = pPos.getZ() + 0.5D;
                getLevelNonnull().playLocalSound(d0, d1, d2, SoundEvents.GRASS_FALL, SoundSource.BLOCKS, 2.0F, 2.0F, false);
                if (getBlockState().getValue(USING_PESTICIDE)) {
                    spawnPesticideParticles();
                }
            }
        }
    }

    public int outputFluid(FluidStack fs, IFluidHandler.FluidAction action)
    {
        if(fs.isEmpty())
            return 0;

        int canAccept = fs.getAmount();
        if(canAccept <= 0)
            return 0;

        final int fluidForSort = canAccept;
        int maxFluidOutput = 0;
        HashMap<FluidPipeBlockEntity.DirectionalFluidOutput, Integer> sorting = new HashMap<>();
        for(Direction f : Direction.values()) {
            if(sideConfig.get(f) != IEEnums.IOSideConfig.OUTPUT)
                continue;

            IFluidHandler handler = neighborFluids.get(f).getNullable();
            if(handler == null)
                continue;

            BlockEntity tile = getLevelNonnull().getBlockEntity(worldPosition.relative(f));
            FluidStack insertResource = Utils.copyFluidStackWithAmount(fs, fs.getAmount(), true);
            int possiblePipeFluid = handler.fill(insertResource, IFluidHandler.FluidAction.SIMULATE);
            if(possiblePipeFluid > 0)
            {
                sorting.put(new FluidPipeBlockEntity.DirectionalFluidOutput(handler, f, tile), possiblePipeFluid);
                maxFluidOutput += possiblePipeFluid;
            }
        }
        if(maxFluidOutput > 0)
        {
            int f = 0;
            int i = 0;
            for(FluidPipeBlockEntity.DirectionalFluidOutput output : sorting.keySet())
            {
                float prio = sorting.get(output)/(float)maxFluidOutput;
                int amount = (int)(fluidForSort*prio);
                if(i++==sorting.size()-1)
                    amount = canAccept;
                FluidStack insertResource = Utils.copyFluidStackWithAmount(fs, amount, true);
                insertResource.getOrCreateTag().putBoolean(IFluidPipe.NBT_PRESSURIZED, true);
                int r = output.output().fill(insertResource, action);
                f += r;
                canAccept -= r;
                if(canAccept <= 0)
                    break;
            }
            return f;
        }
        return 0;
    }

    public void inputFluid(IFluidHandler.FluidAction action) {
        int maxFluid = tank.getCapacity() - tank.getFluidAmount();
        for (Direction direction : Direction.values()) {
            if(sideConfig.get(direction) != IEEnums.IOSideConfig.INPUT)
                continue;

            IFluidHandler handler = neighborFluids.get(direction).getNullable();
            if(handler == null)
                continue;

            FluidStack pipeResource = handler.drain(maxFluid, IFluidHandler.FluidAction.SIMULATE);

            if (isFluidValid(pipeResource.getFluid())) {
                if (pipeResource.getFluid() != tank.getFluid().getFluid() && tank.getFluidAmount()<WATER_CONSUMPTION)
                    tank.drain(tank.getFluidAmount(), action);
                int amountFilled = tank.fill(pipeResource, action);
                handler.drain(amountFilled, action);
                maxFluid -= amountFilled;
                if (maxFluid == 0)
                    return;
            }
        }
    }

    private boolean isFluidValid(Fluid fluid) {
        if (tank.getFluidAmount()<this.WATER_CONSUMPTION) {
            return fluid == Fluids.WATER || fluid == IFFluids.TREATED_WATER_FLUID.get();
        }
        else return fluid == tank.getFluid().getFluid();
    }

    @Nullable
    @Override
    public VoxelShape getBlockBounds(@javax.annotation.Nullable CollisionContext ctx)
    {
        if(!isDummy())
            return Shapes.block();
        return Shapes.box(.1875f, 0, .1875f, .8125f, .8f, .8125f);
    }

    public void setDummy(boolean dummy)
    {
        BlockState old = getBlockState();
        BlockState newState = old.setValue(IEProperties.MULTIBLOCKSLAVE, dummy);
        setState(newState);
    }

    @Nonnull
    @Override
    public <T> LazyOptional<T> getCapability(@Nonnull Capability<T> capability, @javax.annotation.Nullable Direction facing)
    {
        if(capability==ForgeCapabilities.FLUID_HANDLER && facing!=null && !isDummy())
        {
            if(!sidedFluidHandler.containsKey(facing))
                sidedFluidHandler.put(facing, registerCapability(new SprinklerBlockEntity.SidedFluidHandler(this, facing)));
            return sidedFluidHandler.get(facing).cast();
        }
        return super.getCapability(capability, facing);
    }

    @Override
    public void placeDummies(BlockPlaceContext ctx, BlockState state)
    {
        BlockPos dummyPos = worldPosition.above();
        getLevelNonnull().setBlockAndUpdate(dummyPos, IEBaseBlock.applyLocationalWaterlogging(
                state.setValue(IEProperties.MULTIBLOCKSLAVE, true), getLevelNonnull(), dummyPos
        ));
    }

    @Override
    public void breakDummies(BlockPos pos, BlockState state)
    {
        for(int i = 0; i <= 1; i++)
            if(Utils.isBlockAt(getLevelNonnull(), getBlockPos().offset(0, isDummy()?-1: 0, 0).offset(0, i, 0), REFERENCE_BLOCK))
                level.removeBlock(getBlockPos().offset(0, isDummy()?-1: 0, 0).offset(0, i, 0), false);
    }

    @Override
    public boolean isDummy()
    {
        return getBlockState().getValue(IEProperties.MULTIBLOCKSLAVE);
    }


    @Nullable
    @Override
    public SprinklerBlockEntity master()
    {
        if(!isDummy())
            return this;
        BlockPos masterPos = getBlockPos().below();
        BlockEntity te = Utils.getExistingTileEntity(level, masterPos);
        return te instanceof SprinklerBlockEntity sprinkler ? sprinkler : null;
    }

    @Override
    public Component[] getOverlayText(Player player, HitResult mop, boolean hammer)
    {
        if(hammer && IEClientConfig.showTextOverlay.get() && !isDummy() && mop instanceof BlockHitResult brtr)
        {
            IEEnums.IOSideConfig i = sideConfig.get(brtr.getDirection());
            IEEnums.IOSideConfig j = sideConfig.get(brtr.getDirection().getOpposite());
            return TextUtils.sideConfigWithOpposite(Lib.DESC_INFO+"blockSide.connectFluid.", i, j);
        }
        return null;
    }

    @Override
    public boolean useNixieFont(Player player, HitResult mop) {
        return false;
    }

    static class SidedFluidHandler implements IFluidHandler {
        SprinklerBlockEntity sprinkler;
        Direction facing;

        SidedFluidHandler(SprinklerBlockEntity sprinkler, Direction facing)
        {
            this.sprinkler = sprinkler;
            this.facing = facing;
        }

        @Override
        public int getTanks()
        {
            return sprinkler.tank.getTanks();
        }

        @Nonnull
        @Override
        public FluidStack getFluidInTank(int tank)
        {
            return sprinkler.tank.getFluidInTank(tank);
        }

        @Override
        public int getTankCapacity(int tank)
        {
            return sprinkler.tank.getTankCapacity(tank);
        }

        @Override
        public boolean isFluidValid(int tank, @Nonnull FluidStack stack)
        {
            if(sprinkler.sideConfig.get(facing)!= IEEnums.IOSideConfig.INPUT)
                return false;
            return sprinkler.tank.isFluidValid(tank, stack);
        }

        @Override
        public int fill(FluidStack resource, FluidAction action)
        {
            if(resource.isEmpty()||sprinkler.sideConfig.get(facing)!= IEEnums.IOSideConfig.INPUT)
                return 0;
            return sprinkler.tank.fill(resource, action);
        }

        @Override
        public FluidStack drain(FluidStack resource, FluidAction action)
        {
            return this.drain(resource.getAmount(), action);
        }

        @Override
        public FluidStack drain(int maxDrain, FluidAction action)
        {
            if(sprinkler.sideConfig.get(facing)!= IEEnums.IOSideConfig.OUTPUT)
                return FluidStack.EMPTY;
            return sprinkler.tank.drain(maxDrain, action);
        }
    }



    @Override
    public IEEnums.IOSideConfig getSideConfig(Direction side)
    {
        return sideConfig.get(side);
    }

    @Override
    public boolean toggleSide(Direction side, Player p)
    {
        if(side!=Direction.UP&&!isDummy())
        {
            sideConfig.put(side, IEEnums.IOSideConfig.next(sideConfig.get(side)));
            this.setChanged();
            this.markContainingBlockForUpdate(null);
            getLevelNonnull().blockEvent(getBlockPos(), this.getBlockState().getBlock(), 0, 0);
            return true;
        }
        return false;
    }

    protected void spawnParticles() {
        BlockPos pos = getBlockPos().above();
        for(int i = 0; i < 100; i++) {
            double velocity = 0.3 + 0.5*Math.random();
            getLevelNonnull().addParticle(IFParticles.SPRINKLER_PARTICLES.get(),
                    pos.getX() + 0.5d, pos.getY() + 0.5d, pos.getZ() + 0.5d,
                    Math.cos(i)*velocity, 0.6*velocity, Math.sin(i)*velocity);
        }
    }

    protected void spawnPesticideParticles() {
        BlockPos pos = getBlockPos().above();
        for (int i=0; i<6; i++) {
            getLevelNonnull().addParticle(ParticleTypes.HAPPY_VILLAGER.getType(),
                    pos.getX() + Math.random(), pos.getY() + Math.random(), pos.getZ() + Math.random(),
                    0, 0,0);
        }
    }

}
