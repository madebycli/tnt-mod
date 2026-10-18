package de.madebycli.tnt;

import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.InteractionHand;

public final class EditionNetworking {
    private EditionNetworking() {}

    public record OpenPayload(InteractionHand hand, int strength, int range, int color) implements CustomPacketPayload {
        public static final Type<OpenPayload> TYPE = new Type<>(TntEditions.id("open_tuner"));
        public static final StreamCodec<RegistryFriendlyByteBuf, OpenPayload> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeEnum(p.hand); buf.writeVarInt(p.strength); buf.writeVarInt(p.range); buf.writeVarInt(p.color); },
                buf -> new OpenPayload(buf.readEnum(InteractionHand.class), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<OpenPayload> type() { return TYPE; }
    }

    public record CreatePayload(InteractionHand hand, int strength, int range, int color) implements CustomPacketPayload {
        public static final Type<CreatePayload> TYPE = new Type<>(TntEditions.id("create_edition"));
        public static final StreamCodec<RegistryFriendlyByteBuf, CreatePayload> CODEC = StreamCodec.of(
                (buf, p) -> { buf.writeEnum(p.hand); buf.writeVarInt(p.strength); buf.writeVarInt(p.range); buf.writeVarInt(p.color); },
                buf -> new CreatePayload(buf.readEnum(InteractionHand.class), buf.readVarInt(), buf.readVarInt(), buf.readVarInt()));
        @Override public Type<CreatePayload> type() { return TYPE; }
    }

    public record ResultPayload(int result) implements CustomPacketPayload {
        public static final Type<ResultPayload> TYPE = new Type<>(TntEditions.id("craft_result"));
        public static final StreamCodec<RegistryFriendlyByteBuf, ResultPayload> CODEC = StreamCodec.of(
                (buf, p) -> buf.writeVarInt(p.result), buf -> new ResultPayload(buf.readVarInt()));
        @Override public Type<ResultPayload> type() { return TYPE; }
    }

    public static void register() {
        PayloadTypeRegistry.clientboundPlay().register(OpenPayload.TYPE, OpenPayload.CODEC);
        PayloadTypeRegistry.clientboundPlay().register(ResultPayload.TYPE, ResultPayload.CODEC);
        PayloadTypeRegistry.serverboundPlay().register(CreatePayload.TYPE, CreatePayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(CreatePayload.TYPE, (payload, context) -> {
            int result = EditionSettings.isValid(payload.strength, payload.range, payload.color)
                    ? EditionCrafting.craft(context.player(), payload.hand,
                    new EditionSettings(payload.strength, payload.range, payload.color)) : EditionCrafting.INVALID;
            ServerPlayNetworking.send(context.player(), new ResultPayload(result));
        });
    }
}
