package net.etylop.immersivefarming;

import net.etylop.immersivefarming.api.crafting.IFRecipeTypes;
import net.etylop.immersivefarming.block.IFBlockEntities;
import net.etylop.immersivefarming.block.IFBlocks;
import net.etylop.immersivefarming.block.IFMultiblockLogic;
import net.etylop.immersivefarming.block.IFMultiblocks;
import net.etylop.immersivefarming.client.ClientInitializer;
import net.etylop.immersivefarming.client.IFSounds;
import net.etylop.immersivefarming.crafting.IFRecipeSerializer;
import net.etylop.immersivefarming.entity.IFEntities;
import net.etylop.immersivefarming.fluid.IFFluids;
import net.etylop.immersivefarming.gui.IFMenuTypes;
import net.etylop.immersivefarming.item.IFCreativeTab;
import net.etylop.immersivefarming.item.IFItems;
import net.etylop.immersivefarming.network.NetBuilder;
import net.etylop.immersivefarming.network.clientbound.UpdateDrawnMessage;
import net.etylop.immersivefarming.network.serverbound.ActionKeyMessage;
import net.etylop.immersivefarming.network.serverbound.ToggleSlowMessage;
import net.etylop.immersivefarming.particle.IFParticles;
import net.minecraft.client.renderer.ItemBlockRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.DistExecutor;
import net.minecraftforge.fml.ModLoadingContext;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.network.simple.SimpleChannel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;



// The value here should match an entry in the META-INF/mods.toml file
@Mod(ImmersiveFarming.MOD_ID)
public class ImmersiveFarming {
    public static final String MOD_ID = "immersivefarming";

    public static final Logger log = LogManager.getLogger(MOD_ID);

    public static final SimpleChannel CHANNEL = new NetBuilder(new ResourceLocation(MOD_ID, "main"))
            .version(1).optionalServer().requiredClient()
            .serverbound(ActionKeyMessage::new).consumer(() -> ActionKeyMessage::handle)
            .serverbound(ToggleSlowMessage::new).consumer(() -> ToggleSlowMessage::handle)
            .clientbound(UpdateDrawnMessage::new).consumer(() -> new UpdateDrawnMessage.Handler())
            .build();


    public ImmersiveFarming() {
        IEventBus eventBus = FMLJavaModLoadingContext.get().getModEventBus();

        final Initializer.Context ctx = new InitContext();
        DistExecutor.<Initializer>runForDist(() -> ClientInitializer::new, () -> CommonInitializer::new).init(ctx);

        IFMultiblockLogic.init();
        IFItems.register(eventBus);
        IFCreativeTab.register(eventBus);
        IFBlocks.register(eventBus);
        IFBlockEntities.register(eventBus);
        IFParticles.register(eventBus);
        IFFluids.register(eventBus);
        IFMenuTypes.register(eventBus);
        IFRecipeTypes.register(eventBus);
        IFRecipeSerializer.register(eventBus);
        IFEntities.register(eventBus);
        IFSounds.register(eventBus);

        IFMultiblocks.init();

        // Register the setup method for modloading
        eventBus.addListener(this::setup);
        eventBus.addListener(this::clientSetup);

        // Register ourselves for server and other game events we are interested in
        MinecraftForge.EVENT_BUS.register(this);
    }

    private void clientSetup(final FMLClientSetupEvent event) {
        ItemBlockRenderTypes.setRenderLayer(IFFluids.TREATED_WATER_BLOCK.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(IFFluids.TREATED_WATER_FLUID.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(IFFluids.TREATED_WATER_FLOWING.get(), RenderType.translucent());
        ItemBlockRenderTypes.setRenderLayer(IFBlocks.DEAD_CROP.get(), RenderType.cutout());
    }

    private void setup(final FMLCommonSetupEvent event) {
        // some preinit code
    }

    private static class InitContext implements Initializer.Context {
        @Override
        public ModLoadingContext context() {
            return ModLoadingContext.get();
        }

        @Override
        public IEventBus bus() {
            return MinecraftForge.EVENT_BUS;
        }

        @Override
        public IEventBus modBus() {
            return FMLJavaModLoadingContext.get().getModEventBus();
        }
    }

}
