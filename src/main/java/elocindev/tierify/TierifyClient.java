package elocindev.tierify;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.PackType;
import net.minecraft.world.item.Item;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import draylar.tiered.api.BorderTemplate;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.data.AttributeDataLoader;
import elocindev.tierify.data.TooltipBorderLoader;
import elocindev.tierify.network.TieredClientPacket;
import elocindev.tierify.screen.ReforgeScreenHandler;
import elocindev.tierify.screen.client.ReforgeScreen;

@Environment(EnvType.CLIENT)
public class TierifyClient implements ClientModInitializer {

    // map for storing attributes before logging into a server
    public static final Map<ResourceLocation, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    public static final List<BorderTemplate> BORDER_TEMPLATES = new ArrayList<BorderTemplate>();
    
    @Override
    public void onInitializeClient() {
        registerAttributeSyncHandler();
        registerReforgeItemSyncHandler();
        MenuScreens.<ReforgeScreenHandler, ReforgeScreen>register(Tierify.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredClientPacket.init();
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new TooltipBorderLoader());
    }

    public static void registerAttributeSyncHandler() {
        ClientPlayNetworking.registerGlobalReceiver(Tierify.ATTRIBUTE_SYNC_PAYLOAD_ID, (payload, context) -> {
            context.client().execute(() -> {
                CACHED_ATTRIBUTES.clear();
                payload.attributes().forEach((id, attributeJson) -> {
                    CACHED_ATTRIBUTES.put(id, AttributeDataLoader.GSON.fromJson(attributeJson, PotentialAttribute.class));
                });
            });
        });
    }

    public static void registerReforgeItemSyncHandler() {
        ClientPlayNetworking.registerGlobalReceiver(Tierify.REFORGE_ITEM_SYNC_PAYLOAD_ID, (payload, context) -> {
            context.client().execute(() -> {
                Tierify.REFORGE_DATA_LOADER.clearReforgeBaseItems();
                payload.reforgeItems().forEach((targetItem, baseItemIds) -> {
                    List<Item> items = new ArrayList<>();
                    for (ResourceLocation id : baseItemIds) {
                        items.add(BuiltInRegistries.ITEM.get(id));
                    }
                    Tierify.REFORGE_DATA_LOADER.putReforgeBaseItems(targetItem, items);
                });
            });
        });
    }
}
