package net.etylop.immersivefarming.network.serverbound;


import net.etylop.immersivefarming.entity.AbstractDrawnEntity;
import net.etylop.immersivefarming.network.Message;
import net.etylop.immersivefarming.network.ServerMessageContext;
import net.etylop.immersivefarming.world.IFWorld;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;

import java.util.Optional;

public final class ToggleSlowMessage implements Message {
    @Override
    public void encode(final FriendlyByteBuf buf) {
    }

    @Override
    public void decode(final FriendlyByteBuf buf) {
    }

    public static void handle(final ToggleSlowMessage msg, final ServerMessageContext ctx) {
        getCart(ctx.getPlayer()).ifPresent(AbstractDrawnEntity::toggleSlow);
    }

    public static Optional<AbstractDrawnEntity> getCart(final Player player) {
        final Entity ridden = player.getVehicle();
        if (ridden == null) return Optional.empty();
        if (ridden instanceof AbstractDrawnEntity) return Optional.of((AbstractDrawnEntity) ridden);
        return IFWorld.get(ridden.level()).resolve().flatMap(w -> w.getDrawn(ridden));
    }
}
