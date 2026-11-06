package net.etylop.immersivefarming.network.clientbound;

import net.etylop.immersivefarming.client.IFManual;
import net.etylop.immersivefarming.config.IFConfig;
import net.etylop.immersivefarming.network.ClientMessageContext;
import net.etylop.immersivefarming.network.Message;
import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;

/**
 * Sends the server's config values to a client, so that the client shows them (engineer's manual, JEI) instead of its own.
 * See {@link IFConfig#getSynced}.
 */
public final class SyncConfigMessage implements Message {
    private static final byte BOOLEAN = 0, INT = 1, LONG = 2, DOUBLE = 3, STRING = 4, STRING_LIST = 5;

    private Map<String, Object> values;

    public SyncConfigMessage() {
    }

    public SyncConfigMessage(final Map<String, Object> values) {
        this.values = values;
    }

    @Override
    public void encode(final FriendlyByteBuf buf) {
        Map<String, Object> supported = new LinkedHashMap<>();
        this.values.forEach((path, value) -> {
            if (value instanceof Boolean || value instanceof Integer || value instanceof Long || value instanceof Double
                    || value instanceof String || value instanceof List<?>) supported.put(path, value);
        });
        buf.writeVarInt(supported.size());
        supported.forEach((path, value) -> {
            buf.writeUtf(path);
            if (value instanceof Boolean b) buf.writeByte(BOOLEAN).writeBoolean(b);
            else if (value instanceof Integer i) buf.writeByte(INT).writeInt(i);
            else if (value instanceof Long l) buf.writeByte(LONG).writeLong(l);
            else if (value instanceof Double d) buf.writeByte(DOUBLE).writeDouble(d);
            else if (value instanceof String s) {
                buf.writeByte(STRING);
                buf.writeUtf(s);
            }
            else if (value instanceof List<?> list) {
                buf.writeByte(STRING_LIST);
                buf.writeCollection(list, (b, element) -> b.writeUtf(String.valueOf(element)));
            }
        });
    }

    @Override
    public void decode(final FriendlyByteBuf buf) {
        this.values = new LinkedHashMap<>();
        int count = buf.readVarInt();
        for (int i = 0; i < count; i++) {
            String path = buf.readUtf();
            Object value = switch (buf.readByte()) {
                case BOOLEAN -> buf.readBoolean();
                case INT -> buf.readInt();
                case LONG -> buf.readLong();
                case DOUBLE -> buf.readDouble();
                case STRING -> buf.readUtf();
                case STRING_LIST -> buf.readCollection(ArrayList::new, FriendlyByteBuf::readUtf);
                default -> throw new IllegalArgumentException("Unknown config value type");
            };
            this.values.put(path, value);
        }
    }

    public static final class Handler implements BiConsumer<SyncConfigMessage, ClientMessageContext> {
        @Override
        public void accept(final SyncConfigMessage msg, final ClientMessageContext ctx) {
            IFConfig.setSynced(msg.values);
            IFManual.reload();
        }
    }
}
