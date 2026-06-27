package elocindev.tierify;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ProjectileWeaponItem;
import net.minecraft.world.item.ShieldItem;
import net.minecraft.world.item.component.CustomData;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import draylar.tiered.api.*;
import elocindev.tierify.command.CommandInit;
import elocindev.tierify.config.ClientConfig;
import elocindev.tierify.config.CommonConfig;
import elocindev.tierify.data.AttributeDataLoader;

import java.util.*;

@SuppressWarnings({"unused", "null", "deprecation"})
public class Tierify implements ModInitializer {

    public static CommonConfig CONFIG = new CommonConfig();
    public static ClientConfig CLIENT_CONFIG = new ClientConfig();

    /**
     * Attribute Data Loader instance which handles loading attribute .json files from "data/modid/item_attributes".
     * <p>
     * This field is registered to the server's data manager in {@link ServerResourceManagerMixin}
     */
    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    public static final UUID[] MODIFIERS = new UUID[] { UUID.fromString("baf8e074-f7f9-4549-ba1f-e21f82684b8c"), UUID.fromString("9b3416de-98d1-407f-bc6b-e673c2ab5252"),
            UUID.fromString("1e3ceca6-aa30-4165-9715-20bb63c11348"), UUID.fromString("c99bfa17-4886-4cbb-86c2-ebf9369616d5"), UUID.fromString("19e4dc8d-3892-4ffe-a558-f96c68491144"),
            UUID.fromString("b1641cff-84ed-4b63-85f8-2634005adc9b"), UUID.fromString("92f546e9-0d00-4159-8c8f-0499e49f5811"), UUID.fromString("e25c7fa8-13b0-4ea0-8db7-e26b78f36c90"),
            UUID.fromString("2f9dcfce-bd03-4181-86b7-91c88f71e67c") };

    public static final Logger LOGGER = LogManager.getLogger();

    public static final Identifier ATTRIBUTE_SYNC_PACKET = Identifier.parse("attribute_sync");

    public static final CustomPacketPayload.Type<AttributeSyncPayload> ATTRIBUTE_SYNC_PAYLOAD_ID = new CustomPacketPayload.Type<>(ATTRIBUTE_SYNC_PACKET);

    public static final StreamCodec<RegistryFriendlyByteBuf, AttributeSyncPayload> ATTRIBUTE_SYNC_PAYLOAD_CODEC = CustomPacketPayload.codec(
            (payload, buf) -> {
                buf.writeInt(payload.attributes().size());
                payload.attributes().forEach((id, attributeJson) -> {
                    buf.writeIdentifier(id);
                    buf.writeUtf(attributeJson);
                });
            },
            buf -> {
                Map<Identifier, String> attributes = new HashMap<>();
                int size = buf.readInt();
                for (int i = 0; i < size; i++) {
                    attributes.put(buf.readIdentifier(), buf.readUtf());
                }
                return new AttributeSyncPayload(attributes);
            }
    );

    public static final String NBT_SUBTAG_KEY = "Tiered";
    public static final String NBT_SUBTAG_DATA_KEY = "Tier";
    public static final String NBT_SUBTAG_TEMPLATE_DATA_KEY = "Template";
    public static final String NBT_SUBTAG_MARKER_KEY = "TieredAssigned";

    @Override
    public void onInitialize() {
        
        AutoConfig.register(CommonConfig.class, JanksonConfigSerializer::new);
        AutoConfig.register(ClientConfig.class, JanksonConfigSerializer::new);

        CONFIG = AutoConfig.getConfigHolder(CommonConfig.class).getConfig();
        CLIENT_CONFIG = AutoConfig.getConfigHolder(ClientConfig.class).getConfig();

        TieredItemTags.init();
        CustomEntityAttributes.init();
        CommandInit.init();
        registerAttributeSyncer();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(Tierify.ATTRIBUTE_DATA_LOADER);

        PayloadTypeRegistry.clientboundPlay().register(ATTRIBUTE_SYNC_PAYLOAD_ID, ATTRIBUTE_SYNC_PAYLOAD_CODEC);

        ServerLifecycleEvents.END_DATA_PACK_RELOAD.register((server, serverResourceManager, success) -> {
            if (success) {
                for (int i = 0; i < server.getPlayerList().getPlayers().size(); i++)
                    updateItemStackNbt(server.getPlayerList().getPlayers().get(i).getInventory());
                LOGGER.info("Finished reload on {}", Thread.currentThread());
            } else
                LOGGER.error("Failed to reload on {}", Thread.currentThread());
        });
        ServerPlayConnectionEvents.INIT.register((handler, server) -> {
            updateItemStackNbt(handler.player.getInventory());
        });



    }

    /**
     * Returns an {@link Identifier} namespaced with this mod's modid ("tiered").
     *
     * @param path path of identifier (eg. apple in "minecraft:apple")
     * @return Identifier created with a namespace of this mod's modid ("tiered") and provided path
     */
    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath("tiered", path);
    }

    public static boolean isPreferredEquipmentSlot(ItemStack stack, EquipmentSlot slot) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        if (equippable != null) {
            return equippable.slot().equals(slot);
        }
        if (stack.getItem() instanceof ShieldItem || stack.getItem() instanceof ProjectileWeaponItem || stack.is(TieredItemTags.MAIN_OFFHAND_ITEM)) {
            return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        }
        return slot == EquipmentSlot.MAINHAND;
    }

    public static void registerAttributeSyncer() {
        ServerPlayConnectionEvents.JOIN.register((network, packetSender, minecraftServer) -> {
            Map<Identifier, String> serializedAttributes = new HashMap<>();
            ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                serializedAttributes.put(id, AttributeDataLoader.GSON.toJson(attribute));
            });
            ServerPlayNetworking.send(network.player, new AttributeSyncPayload(serializedAttributes));
        });
    }

    public static void updateItemStackNbt(Inventory playerInventory) {
        for (int u = 0; u < playerInventory.getContainerSize(); u++) {
            ItemStack itemStack = playerInventory.getItem(u);
            if (itemStack.isEmpty()) {
                continue;
            }

            ModifierUtils.applyTierIfNeeded(itemStack);

            Identifier tierId = ModifierUtils.getAttributeID(itemStack);
            if (tierId != null) {
                int appliedCount = ModifierUtils.applyTierAttributes(itemStack);
                if (appliedCount == 0) {
                    LOGGER.warn("updateItemStackNbt kept tier {} on {} but generated zero modifiers", tierId, BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
                }
                playerInventory.setItem(u, itemStack);
            }
        }
    }

    public record AttributeSyncPayload(Map<Identifier, String> attributes) implements CustomPacketPayload {
        @Override
        public CustomPacketPayload.Type<AttributeSyncPayload> type() {
            return ATTRIBUTE_SYNC_PAYLOAD_ID;
        }
    }
}
