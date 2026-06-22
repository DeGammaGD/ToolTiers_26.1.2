package elocindev.tierify;

import me.shedaniel.autoconfig.AutoConfig;
import me.shedaniel.autoconfig.serializer.JanksonConfigSerializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
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
import elocindev.tierify.registry.ItemRegistry;

import java.util.*;

@SuppressWarnings("unused")
public class Tierify implements ModInitializer {

    public static CommonConfig CONFIG = new CommonConfig();
    public static ClientConfig CLIENT_CONFIG = new ClientConfig();

    /**
     * Attribute Data Loader instance which handles loading attribute .json files from "data/modid/item_attributes".
     * <p>
     * This field is registered to the server's data manager in {@link ServerResourceManagerMixin}
     */
    public static final AttributeDataLoader ATTRIBUTE_DATA_LOADER = new AttributeDataLoader();

    // Same UUIDs as in ArmorItem
    // public static final UUID[] MODIFIERS = new UUID[] { UUID.fromString("845DB27C-C624-495F-8C9F-6020A9A58B6B"), UUID.fromString("D8499B04-0E66-4726-AB29-64469D734E0D"),
    // UUID.fromString("9F3D476D-C118-4544-8365-64846904B48E"), UUID.fromString("2AD3F246-FEE1-4E67-B886-69FD380BB150"), UUID.fromString("4a88bc27-9563-4eeb-96d5-fe50917cc24f"),
    // UUID.fromString("fee48d8c-1b51-4c46-9f4b-c58162623a7a") };

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
        ItemRegistry.init();
        CustomEntityAttributes.init();
        CommandInit.init();
        registerAttributeSyncer();
        ResourceManagerHelper.get(PackType.SERVER_DATA).registerReloadListener(Tierify.ATTRIBUTE_DATA_LOADER);

        PayloadTypeRegistry.clientboundPlay().register(ATTRIBUTE_SYNC_PAYLOAD_ID, ATTRIBUTE_SYNC_PAYLOAD_CODEC);
        
        if (FabricLoader.getInstance().getEnvironmentType() == EnvType.CLIENT) {
            // TODO: Add config stuff for the plate in the tooltip
            // setupModifierLabel();
        }

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

    /**
     * Creates an {@link ItemTooltipCallback} listener that adds the modifier name at the top of an Item tooltip.
     * <p>
     * A tool name is only displayed if the item has a modifier.
     */
    private void setupModifierLabel() {
        ItemTooltipCallback.EVENT.register((stack, context, type, lines) -> {
            CustomData customData = stack.get(DataComponents.CUSTOM_DATA);
            CompoundTag root = customData != null ? customData.copyTag() : new CompoundTag();
            // has tier
            if (customData != null && root.contains(NBT_SUBTAG_KEY)) {
                // get tier
                CompoundTag tiered = root.getCompound(NBT_SUBTAG_KEY).orElse(null);
                if (tiered == null) {
                    return;
                }
                Identifier tier = Identifier.parse(tiered.getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse(""));

                // attempt to display attribute if it is valid
                PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

                if (potentialAttribute != null)
                    lines.add(1, Component.translatable(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle()));
            }
        });
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
            CustomData customData = itemStack.get(DataComponents.CUSTOM_DATA);
            CompoundTag root = customData != null ? customData.copyTag() : new CompoundTag();
            if (!itemStack.isEmpty() && customData != null && root.contains(Tierify.NBT_SUBTAG_KEY)) {

                // Check if attribute exists
                List<String> attributeIds = new ArrayList<>();
                Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
                    if (attribute.isValid(BuiltInRegistries.ITEM.getKey(itemStack.getItem()))) {
                        attributeIds.add(attribute.getID());
                    }

                });
                Identifier attributeID = null;
                for (int i = 0; i < attributeIds.size(); i++) {
                    String tieredText = root.getCompound(Tierify.NBT_SUBTAG_KEY).map(CompoundTag::toString).orElse("");
                    if (tieredText.contains(attributeIds.get(i))) {
                        attributeID = Identifier.parse(attributeIds.get(i));
                        break;
                    } else if (i == attributeIds.size() - 1) {
                        ModifierUtils.removeItemStackAttribute(itemStack);
                        attributeID = ModifierUtils.getRandomAttributeIDFor(null, itemStack.getItem(), false);
                    }
                }

                // found an ID
                if (attributeID != null) {

                    HashMap<String, Object> nbtMap = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.parse(attributeID.toString())).getNbtValues();
                    // update durability nbt

                    List<AttributeTemplate> attributeList = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.parse(attributeID.toString())).getAttributes();
                    for (int i = 0; i < attributeList.size(); i++) {
                        if (attributeList.get(i).getAttributeTypeID().equals("tiered:generic.durable")) {
                            if (nbtMap == null) {
                                nbtMap = new HashMap<String, Object>();
                            }
                            nbtMap.put("durable", (double) Math.round(attributeList.get(i).getEntityAttributeModifier().amount() * 100.0) / 100.0);
                            break;
                        }
                    }

                    // add nbtMap
                    if (nbtMap != null) {
                        CompoundTag nbtCompound = customData.copyTag();
                        for (HashMap.Entry<String, Object> entry : nbtMap.entrySet()) {
                            String key = entry.getKey();
                            Object value = entry.getValue();

                            // json list will get read as ArrayList class
                            // json map will get read as linkedtreemap
                            // json integer is read by gson -> always double
                            if (value instanceof String) {
                                nbtCompound.putString(key, (String) value);
                            } else if (value instanceof Boolean) {
                                nbtCompound.putBoolean(key, (boolean) value);
                            } else if (value instanceof Double) {
                                if ((double) Math.abs((double) value) % 1.0 < 0.0001D) {
                                    nbtCompound.putInt(key, (int) Math.round((double) value));
                                } else {
                                    nbtCompound.putDouble(key, Math.round((double) value * 100.0) / 100.0);
                                }
                            }
                        }
                        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbtCompound));
                        customData = itemStack.get(DataComponents.CUSTOM_DATA);
                        root = customData != null ? customData.copyTag() : new CompoundTag();
                    }
                    if (customData == null || !root.contains(Tierify.NBT_SUBTAG_KEY)) {
                        CompoundTag updatedRoot = customData != null ? customData.copyTag() : new CompoundTag();
                        CompoundTag tiered = new CompoundTag();
                        tiered.putString(Tierify.NBT_SUBTAG_DATA_KEY, attributeID.toString());
                        updatedRoot.put(Tierify.NBT_SUBTAG_KEY, tiered);
                        itemStack.set(DataComponents.CUSTOM_DATA, CustomData.of(updatedRoot));
                    }
                    playerInventory.setItem(u, itemStack);
                }
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
