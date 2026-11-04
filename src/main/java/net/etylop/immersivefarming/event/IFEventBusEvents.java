package net.etylop.immersivefarming.event;

import blusunrize.immersiveengineering.common.gui.IEContainerMenu;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.block.IFBlockEntities;
import net.etylop.immersivefarming.block.entity.SprinklerExtendedRenderer;
import net.etylop.immersivefarming.block.entity.SprinklerRenderer;
import net.etylop.immersivefarming.block.utils.IFDynamicModel;
import net.etylop.immersivefarming.gui.screen.ComposterScreen;
import net.etylop.immersivefarming.gui.IFMenuTypes;
import net.etylop.immersivefarming.particle.IFParticles;
import net.etylop.immersivefarming.particle.custom.ContaminationParticles;
import net.etylop.immersivefarming.particle.custom.SprinklerParticles;
import net.etylop.immersivefarming.utils.IFBasicClientProperties;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.MenuAccess;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.ModelEvent;
import net.minecraftforge.client.event.RegisterParticleProvidersEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;

import java.util.function.Supplier;

@Mod.EventBusSubscriber(modid = ImmersiveFarming.MOD_ID, bus = Mod.EventBusSubscriber.Bus.MOD, value = Dist.CLIENT)
public class IFEventBusEvents {
    @SubscribeEvent
    public static void registerParticleFactories(final RegisterParticleProvidersEvent event) {
        event.registerSpriteSet(IFParticles.SPRINKLER_PARTICLES.get(), SprinklerParticles.Provider::new);
        event.registerSpriteSet(IFParticles.CONTAMINATION_PARTICLES.get(), ContaminationParticles.Provider::new);
    }

    @SubscribeEvent
    public static void registerRenders(EntityRenderersEvent.RegisterRenderers event)
    {
        registerBERenders(event);
    }


    public static void registerBERenders(EntityRenderersEvent.RegisterRenderers event)
    {
        registerBERenderNoContext(event, IFBlockEntities.SPRINKLER.master(), SprinklerRenderer::new);
        registerBERenderNoContext(event, IFBlockEntities.SPRINKLER_EXTENDED.master(), SprinklerExtendedRenderer::new);
    }

    private static <T extends BlockEntity>
    void registerBERenderNoContext(
            EntityRenderersEvent.RegisterRenderers event, BlockEntityType<? extends T> type, Supplier<BlockEntityRenderer<T>> render
    )
    {
        event.registerBlockEntityRenderer(type, $ -> render.get());
    }

    @SubscribeEvent
    public static void registerModelLoaders(ModelEvent.RegisterAdditional ev)
    {
        // Fired on every resource reload
        if(SprinklerRenderer.SPRINKLER_TOP==null)
        {
            SprinklerRenderer.SPRINKLER_TOP = new IFDynamicModel(SprinklerRenderer.NAME_SPRINKLER);
            SprinklerExtendedRenderer.SPRINKLER_TOP = new IFDynamicModel(SprinklerExtendedRenderer.NAME_SPRINKLER);
            IFBasicClientProperties.initModels();
        }
        IFDynamicModel.registerModels(ev);
    }

    @SubscribeEvent
    public static void clientSetup(FMLClientSetupEvent ev)
    {
        ev.enqueueWork(IFEventBusEvents::registerContainersAndScreens);
    }

    private static void registerContainersAndScreens() {
        registerTileScreen(IFMenuTypes.COMPOSTER, ComposterScreen::new);
    }

    public static <C extends IEContainerMenu, S extends Screen & MenuAccess<C>>
    void registerTileScreen(IFMenuTypes.MultiblockContainerIF<?, C> type, MenuScreens.ScreenConstructor<C, S> factory)
    {
        MenuScreens.register(type.getType(), factory);
    }
}
