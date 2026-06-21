package elocindev.tierify.network;

import elocindev.tierify.access.AnvilScreenHandlerAccess;
import elocindev.tierify.screen.ReforgeScreenHandler;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.libz.network.LibzServerPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;

public class TieredServerPacket {

        public static final CustomPacketPayload.Type<SetScreenPayload> SET_SCREEN_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "set_screen"));
        public static final CustomPacketPayload.Type<ReforgeReadyPayload> REFORGE_READY_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "reforge_ready"));
        public static final CustomPacketPayload.Type<ReforgePayload> REFORGE_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "reforge"));
        public static final CustomPacketPayload.Type<HealthPayload> HEALTH_ID = new CustomPacketPayload.Type<>(ResourceLocation.fromNamespaceAndPath("tiered", "health"));

        public static final StreamCodec<RegistryFriendlyByteBuf, SetScreenPayload> SET_SCREEN_CODEC = StreamCodec.ofMember(
            (payload, buf) -> {
            buf.writeInt(payload.mouseX());
            buf.writeInt(payload.mouseY());
            buf.writeBoolean(payload.reforgingScreen());
            },
            buf -> new SetScreenPayload(buf.readInt(), buf.readInt(), buf.readBoolean())
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ReforgePayload> REFORGE_CODEC = StreamCodec.ofMember(
            (payload, buf) -> {
            },
            buf -> new ReforgePayload()
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, HealthPayload> HEALTH_CODEC = StreamCodec.ofMember(
            (payload, buf) -> buf.writeFloat(payload.health()),
            buf -> new HealthPayload(buf.readFloat())
        );
        public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeReadyPayload> REFORGE_READY_CODEC = StreamCodec.ofMember(
            (payload, buf) -> buf.writeBoolean(payload.disableButton()),
            buf -> new ReforgeReadyPayload(buf.readBoolean())
        );

    public static void init() {
        PayloadTypeRegistry.playC2S().register(SetScreenPayload.ID, SET_SCREEN_CODEC);
        PayloadTypeRegistry.playC2S().register(ReforgePayload.ID, REFORGE_CODEC);
        PayloadTypeRegistry.playS2C().register(HealthPayload.ID, HEALTH_CODEC);
        PayloadTypeRegistry.playS2C().register(ReforgeReadyPayload.ID, REFORGE_READY_CODEC);

        ServerPlayNetworking.registerGlobalReceiver(SetScreenPayload.ID, (payload, context) -> {
            ServerPlayer player = context.player();
            int mouseX = payload.mouseX();
            int mouseY = payload.mouseY();
            boolean reforgingScreen = payload.reforgingScreen();
            BlockPos pos = reforgingScreen ? (player.containerMenu instanceof AnvilMenu ? ((AnvilScreenHandlerAccess) player.containerMenu).getPos() : null)
                    : (player.containerMenu instanceof ReforgeScreenHandler ? ((ReforgeScreenHandler) player.containerMenu).getPos() : null);
            if (pos != null) {
                context.server().execute(() -> {
                    if (reforgingScreen) {
                        player.openMenu(new SimpleMenuProvider((syncId, playerInventory, playerx) -> {
                            return new ReforgeScreenHandler(syncId, playerInventory, ContainerLevelAccess.create(playerx.level(), pos));
                        }, Component.translatable("container.reforge")));
                    } else
                        player.openMenu(new SimpleMenuProvider((syncId, playerInventory, playerx) -> {
                            return new AnvilMenu(syncId, playerInventory, ContainerLevelAccess.create(playerx.level(), pos));
                        }, Component.translatable("container.repair")));

                    LibzServerPacket.writeS2CMousePositionPacket(player, mouseX, mouseY);
                });
            }
        });
        ServerPlayNetworking.registerGlobalReceiver(ReforgePayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                if (context.player().containerMenu instanceof ReforgeScreenHandler)
                    ((ReforgeScreenHandler) context.player().containerMenu).reforge();
            });
        });
    }

    public static void writeS2CHealthPacket(ServerPlayer serverPlayerEntity) {
        ServerPlayNetworking.send(serverPlayerEntity, new HealthPayload(serverPlayerEntity.getHealth()));
    }

    public static void writeS2CReforgeReadyPacket(ServerPlayer serverPlayerEntity, boolean disableButton) {
        ServerPlayNetworking.send(serverPlayerEntity, new ReforgeReadyPayload(disableButton));
    }

    public record SetScreenPayload(int mouseX, int mouseY, boolean reforgingScreen) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<SetScreenPayload> ID = SET_SCREEN_ID;
        public static final StreamCodec<RegistryFriendlyByteBuf, SetScreenPayload> CODEC = SET_SCREEN_CODEC;

        @Override
        public CustomPacketPayload.Type<SetScreenPayload> type() {
            return ID;
        }
    }

    public record ReforgePayload() implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReforgePayload> ID = REFORGE_ID;
        public static final StreamCodec<RegistryFriendlyByteBuf, ReforgePayload> CODEC = REFORGE_CODEC;

        @Override
        public CustomPacketPayload.Type<ReforgePayload> type() {
            return ID;
        }
    }

    public record HealthPayload(float health) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<HealthPayload> ID = HEALTH_ID;
        public static final StreamCodec<RegistryFriendlyByteBuf, HealthPayload> CODEC = HEALTH_CODEC;

        @Override
        public CustomPacketPayload.Type<HealthPayload> type() {
            return ID;
        }
    }

    public record ReforgeReadyPayload(boolean disableButton) implements CustomPacketPayload {
        public static final CustomPacketPayload.Type<ReforgeReadyPayload> ID = REFORGE_READY_ID;
        public static final StreamCodec<RegistryFriendlyByteBuf, ReforgeReadyPayload> CODEC = REFORGE_READY_CODEC;

        @Override
        public CustomPacketPayload.Type<ReforgeReadyPayload> type() {
            return ID;
        }
    }
}
