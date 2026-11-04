package net.etylop.immersivefarming.fluid;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.item.IFItems;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.client.extensions.common.IClientFluidTypeExtensions;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fluids.FluidType;
import net.minecraftforge.fluids.ForgeFlowingFluid;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

import java.util.function.Consumer;

public class IFFluids {
    public static final ResourceLocation WATER_STILL_RL = new ResourceLocation("block/water_still");
    public static final ResourceLocation WATER_FLOWING_RL = new ResourceLocation("block/water_flow");
    public static final ResourceLocation WATER_OVERLAY_RL = new ResourceLocation("block/water_overlay");

    public static final DeferredRegister<Fluid> FLUIDS
            = DeferredRegister.create(ForgeRegistries.FLUIDS, ImmersiveFarming.MOD_ID);

    public static final DeferredRegister<FluidType> FLUID_TYPES
            = DeferredRegister.create(ForgeRegistries.Keys.FLUID_TYPES, ImmersiveFarming.MOD_ID);


    public static final RegistryObject<FluidType> TREATED_WATER_TYPE = registerType("treated_water_fluid", 0xbf0f0f0f);
    public static final RegistryObject<FluidType> DRY_MATTER_TYPE = registerType("dry_matter_fluid", 0xff7f1b00);
    public static final RegistryObject<FluidType> WET_MATTER_TYPE = registerType("wet_matter_fluid", 0xff007C0E);


    public static final RegistryObject<FlowingFluid> TREATED_WATER_FLUID
            = FLUIDS.register("treated_water_fluid", () -> new ForgeFlowingFluid.Source(IFFluids.TREATED_WATER_PROPERTIES));

    public static final RegistryObject<FlowingFluid> TREATED_WATER_FLOWING
            = FLUIDS.register("treated_water_flowing", () -> new ForgeFlowingFluid.Flowing(IFFluids.TREATED_WATER_PROPERTIES));

    public static final RegistryObject<FlowingFluid> DRY_MATTER_FLUID
            = FLUIDS.register("dry_matter_fluid", () -> new ForgeFlowingFluid.Source(IFFluids.DRY_MATTER_PROPERTIES));

    public static final RegistryObject<FlowingFluid> DRY_MATTER_FLOWING
            = FLUIDS.register("dry_matter_flowing", () -> new ForgeFlowingFluid.Source(IFFluids.DRY_MATTER_PROPERTIES));

    public static final RegistryObject<FlowingFluid> WET_MATTER_FLUID
            = FLUIDS.register("wet_matter_fluid", () -> new ForgeFlowingFluid.Source(IFFluids.WET_MATTER_PROPERTIES));

    public static final RegistryObject<FlowingFluid> WET_MATTER_FLOWING
            = FLUIDS.register("wet_matter_flowing", () -> new ForgeFlowingFluid.Source(IFFluids.WET_MATTER_PROPERTIES));


    public static final ForgeFlowingFluid.Properties TREATED_WATER_PROPERTIES = new ForgeFlowingFluid.Properties(
            TREATED_WATER_TYPE, () -> TREATED_WATER_FLUID.get(), () -> TREATED_WATER_FLOWING.get())
            .block(() -> IFFluids.TREATED_WATER_BLOCK.get()).bucket(() -> IFItems.TREATED_WATER_BUCKET.get());

    public static final ForgeFlowingFluid.Properties DRY_MATTER_PROPERTIES = new ForgeFlowingFluid.Properties(
            DRY_MATTER_TYPE, () -> DRY_MATTER_FLUID.get(), () -> DRY_MATTER_FLOWING.get());

    public static final ForgeFlowingFluid.Properties WET_MATTER_PROPERTIES = new ForgeFlowingFluid.Properties(
            WET_MATTER_TYPE, () -> WET_MATTER_FLUID.get(), () -> WET_MATTER_FLOWING.get());

    public static final RegistryObject<LiquidBlock> TREATED_WATER_BLOCK = IFBlocks.BLOCKS.register("treated_water",
            () -> new LiquidBlock(() -> IFFluids.TREATED_WATER_FLUID.get(), BlockBehaviour.Properties.of()
                    .mapColor(MapColor.WATER).replaceable().liquid().pushReaction(PushReaction.DESTROY)
                    .noCollission().strength(100f).noLootTable()));

    private static RegistryObject<FluidType> registerType(String name, int color) {
        return FLUID_TYPES.register(name, () -> new FluidType(FluidType.Properties.create()
                .descriptionId("fluid." + ImmersiveFarming.MOD_ID + "." + name)) {
            @Override
            public void initializeClient(Consumer<IClientFluidTypeExtensions> consumer) {
                consumer.accept(new IClientFluidTypeExtensions() {
                    @Override
                    public ResourceLocation getStillTexture() {
                        return WATER_STILL_RL;
                    }

                    @Override
                    public ResourceLocation getFlowingTexture() {
                        return WATER_FLOWING_RL;
                    }

                    @Override
                    public ResourceLocation getOverlayTexture() {
                        return WATER_OVERLAY_RL;
                    }

                    @Override
                    public int getTintColor() {
                        return color;
                    }
                });
            }
        });
    }

    public static void register(IEventBus eventBus) {
        FLUID_TYPES.register(eventBus);
        FLUIDS.register(eventBus);
    }
}
