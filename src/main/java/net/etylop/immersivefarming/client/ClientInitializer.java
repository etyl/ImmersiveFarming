package net.etylop.immersivefarming.client;


import net.etylop.immersivefarming.CommonInitializer;
import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.client.renderer.IFModelLayers;
import net.etylop.immersivefarming.client.renderer.entity.PlowRenderer;
import net.etylop.immersivefarming.client.renderer.entity.SowerRenderer;
import net.etylop.immersivefarming.client.renderer.entity.model.PlowModel;
import net.etylop.immersivefarming.client.renderer.entity.model.SowerModel;
import net.etylop.immersivefarming.entity.IFEntities;
import net.etylop.immersivefarming.gui.IFMenuTypes;
import net.etylop.immersivefarming.gui.screen.PlowScreen;
import net.etylop.immersivefarming.gui.screen.SowerScreen;
import net.etylop.immersivefarming.network.serverbound.ActionKeyMessage;
import net.etylop.immersivefarming.network.serverbound.ToggleSlowMessage;
import net.etylop.immersivefarming.world.IFWorld;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.event.EntityRenderersEvent;
import net.minecraftforge.client.event.InputEvent;
import net.minecraftforge.client.event.RegisterKeyMappingsEvent;
import net.minecraftforge.client.event.ScreenEvent;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import org.lwjgl.glfw.GLFW;

public final class ClientInitializer extends CommonInitializer {
    private final KeyMapping action = new KeyMapping("key.immersivefarming.desc", GLFW.GLFW_KEY_R, "key.categories.immersivefarming");

    @Override
    public void init(final Context mod) {
        super.init(mod);
        mod.bus().<TickEvent.ClientTickEvent>addListener(e -> {
            if (e.phase == TickEvent.Phase.END) {
                final Minecraft mc = Minecraft.getInstance();
                final Level world = mc.level;
                if (world != null) {
                    while (this.action.consumeClick()) {
                        ImmersiveFarming.CHANNEL.sendToServer(new ActionKeyMessage());
                    }
                    if (!mc.isPaused()) {
                        IFWorld.get(world).ifPresent(IFWorld::tick);
                    }
                }
            }
        });
        mod.bus().<InputEvent.Key>addListener(e -> {
            final Minecraft mc = Minecraft.getInstance();
            final Player player = mc.player;
            if (player != null) {
                if (ToggleSlowMessage.getCart(player).isPresent()) {
                    final KeyMapping binding = mc.options.keySprint;
                    while (binding.consumeClick()) {
                        ImmersiveFarming.CHANNEL.sendToServer(new ToggleSlowMessage());
                        KeyMapping.set(binding.getKey(), false);
                    }
                }
            }
        });
        mod.bus().<ScreenEvent.Opening>addListener(e -> {
            if (e.getNewScreen() instanceof InventoryScreen) {
                final LocalPlayer player = Minecraft.getInstance().player;
            }
        });
        mod.modBus().<FMLClientSetupEvent>addListener(e -> {
            MenuScreens.register(IFMenuTypes.PLOW_CART.get(), PlowScreen::new);
            MenuScreens.register(IFMenuTypes.SOWER_CART.get(), SowerScreen::new);
        });
        mod.modBus().<RegisterKeyMappingsEvent>addListener(e -> e.register(this.action));
        mod.modBus().<EntityRenderersEvent.RegisterRenderers>addListener(e -> {
            e.registerEntityRenderer(IFEntities.PLOW.get(), PlowRenderer::new);
            e.registerEntityRenderer(IFEntities.SOWER.get(), SowerRenderer::new);

        });
        mod.modBus().<EntityRenderersEvent.RegisterLayerDefinitions>addListener(e -> {
            ForgeHooksClient.registerLayerDefinition(IFModelLayers.PLOW, PlowModel::createLayer);
            ForgeHooksClient.registerLayerDefinition(IFModelLayers.SOWER, SowerModel::createLayer);

        });
    }
}
