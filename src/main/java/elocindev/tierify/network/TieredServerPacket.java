package elocindev.tierify.network;

import elocindev.tierify.access.AnvilScreenHandlerAccess;
import elocindev.tierify.screen.ReforgeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.libz.network.LibzServerPacket;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.screen.AnvilScreenHandler;
import net.minecraft.screen.ScreenHandlerContext;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;

public class TieredServerPacket {

        public static final CustomPayload.Id<SetScreenPayload> SET_SCREEN_ID = new CustomPayload.Id<>(Identifier.of("tiered", "set_screen"));
        public static final CustomPayload.Id<ReforgeReadyPayload> REFORGE_READY_ID = new CustomPayload.Id<>(Identifier.of("tiered", "reforge_ready"));
        public static final CustomPayload.Id<ReforgePayload> REFORGE_ID = new CustomPayload.Id<>(Identifier.of("tiered", "reforge"));
        public static final CustomPayload.Id<HealthPayload> HEALTH_ID = new CustomPayload.Id<>(Identifier.of("tiered", "health"));

        public static final PacketCodec<RegistryByteBuf, SetScreenPayload> SET_SCREEN_CODEC = PacketCodec.of(
            (payload, buf) -> {
            buf.writeInt(payload.mouseX());
            buf.writeInt(payload.mouseY());
            buf.writeBoolean(payload.reforgingScreen());
            },
            buf -> new SetScreenPayload(buf.readInt(), buf.readInt(), buf.readBoolean())
        );
        public static final PacketCodec<RegistryByteBuf, ReforgePayload> REFORGE_CODEC = PacketCodec.of(
            (payload, buf) -> {
            },
            buf -> new ReforgePayload()
        );
        public static final PacketCodec<RegistryByteBuf, HealthPayload> HEALTH_CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeFloat(payload.health()),
            buf -> new HealthPayload(buf.readFloat())
        );
        public static final PacketCodec<RegistryByteBuf, ReforgeReadyPayload> REFORGE_READY_CODEC = PacketCodec.of(
            (payload, buf) -> buf.writeBoolean(payload.disableButton()),
            buf -> new ReforgeReadyPayload(buf.readBoolean())
        );

    public static void init() {
        PayloadTypeRegistry.playC2S().register(SetScreenPayload.ID, SET_SCREEN_CODEC);
        PayloadTypeRegistry.playC2S().register(ReforgePayload.ID, REFORGE_CODEC);
        PayloadTypeRegistry.playS2C().register(HealthPayload.ID, HEALTH_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeReadyPayload.ID, REFORGE_READY_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetScreenPayload.ID, (payload, context) -> {
            ServerPlayerEntity player = context.player();
            int mouseX = payload.mouseX();
            int mouseY = payload.mouseY();
            boolean reforgingScreen = payload.reforgingScreen();
            BlockPos pos = reforgingScreen ? (player.currentScreenHandler instanceof AnvilScreenHandler ? ((AnvilScreenHandlerAccess) player.currentScreenHandler).getPos() : null)
                    : (player.currentScreenHandler instanceof ReforgeScreenHandler ? ((ReforgeScreenHandler) player.currentScreenHandler).getPos() : null);
            if (pos != null) {
                context.server().execute(() -> {
                    if (reforgingScreen) {
                        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerx) -> {
                            return new ReforgeScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(playerx.getWorld(), pos));
                        }, Text.translatable("container.reforge")));
                    } else
                        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerx) -> {
                            return new AnvilScreenHandler(syncId, playerInventory, ScreenHandlerContext.create(playerx.getWorld(), pos));
                        }, Text.translatable("container.repair")));

                    LibzServerPacket.writeS2CMousePositionPacket(player, mouseX, mouseY);
                });
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ReforgePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().currentScreenHandler instanceof ReforgeScreenHandler)
                    ((ReforgeScreenHandler) context.player().currentScreenHandler).reforge();
            });
        });
    }

    public static void writeS2CHealthPacket(ServerPlayerEntity serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new HealthPayload(serverPlayerEntity.getHealth()));
    }

    public static void writeS2CReforgeReadyPacket(ServerPlayerEntity serverPlayerEntity, boolean disableButton) {
        ServerPlayNetworking.send(serverPlayerEntity, new ReforgeReadyPayload(disableButton));
    }

    public record SetScreenPayload(int mouseX, int mouseY, boolean reforgingScreen) implements CustomPayload {
        public static final CustomPayload.Id<SetScreenPayload> ID = SET_SCREEN_ID;
        public static final PacketCodec<RegistryByteBuf, SetScreenPayload> CODEC = SET_SCREEN_CODEC;

        @Override
        public CustomPayload.Id<SetScreenPayload> getId() {
            return ID;
        }
    }

    public record ReforgePayload() implements CustomPayload {
        public static final CustomPayload.Id<ReforgePayload> ID = REFORGE_ID;
        public static final PacketCodec<RegistryByteBuf, ReforgePayload> CODEC = REFORGE_CODEC;

        @Override
        public CustomPayload.Id<ReforgePayload> getId() {
            return ID;
        }
    }

    public record HealthPayload(float health) implements CustomPayload {
        public static final CustomPayload.Id<HealthPayload> ID = HEALTH_ID;
        public static final PacketCodec<RegistryByteBuf, HealthPayload> CODEC = HEALTH_CODEC;

        @Override
        public CustomPayload.Id<HealthPayload> getId() {
            return ID;
        }
    }

    public record ReforgeReadyPayload(boolean disableButton) implements CustomPayload {
        public static final CustomPayload.Id<ReforgeReadyPayload> ID = REFORGE_READY_ID;
        public static final PacketCodec<RegistryByteBuf, ReforgeReadyPayload> CODEC = REFORGE_READY_CODEC;

        @Override
        public CustomPayload.Id<ReforgeReadyPayload> getId() {
            return ID;
        }
    }
}
