package net.etylop.immersivefarming.config;

import net.etylop.immersivefarming.ImmersiveFarming;
import net.etylop.immersivefarming.network.clientbound.SyncConfigMessage;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.event.config.ModConfigEvent;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.server.ServerLifecycleHooks;

/**
 * Server side of the config sync: sends the server's config values to the players when they join,
 * and again when the config file is edited while the server is running.
 * The host of a single player (or LAN) world shares the server's config, so it isn't sent to them.
 */
public final class ConfigSync {
    private ConfigSync() {
    }

    public static void register(final IEventBus forgeBus, final IEventBus modBus) {
        forgeBus.<PlayerEvent.PlayerLoggedInEvent>addListener(e -> {
            if (e.getEntity() instanceof ServerPlayer player) send(player);
        });
        modBus.<ModConfigEvent.Reloading>addListener(e -> {
            if (e.getConfig().getSpec() != IFConfig.spec()) return;
            MinecraftServer server = ServerLifecycleHooks.getCurrentServer();
            // Fired from the config file watcher thread
            if (server != null) server.execute(() -> server.getPlayerList().getPlayers().forEach(ConfigSync::send));
        });
    }

    private static void send(final ServerPlayer player) {
        if (player.server.isSingleplayerOwner(player.getGameProfile())) return;
        try {
            ImmersiveFarming.CHANNEL.send(PacketDistributor.PLAYER.with(() -> player), new SyncConfigMessage(IFConfig.snapshot()));
        } catch (RuntimeException e) {
            ImmersiveFarming.log.error("Could not send the config to {}", player.getName().getString(), e);
        }
    }
}
