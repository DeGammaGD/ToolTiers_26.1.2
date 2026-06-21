package elocindev.tierify;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.libz.registry.TabRegistry;
import net.minecraft.client.gui.screen.ingame.AnvilScreen;
import net.minecraft.client.gui.screen.ingame.HandledScreens;
import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;


import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import draylar.tiered.api.BorderTemplate;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.data.AttributeDataLoader;
import elocindev.tierify.network.TieredClientPacket;
import elocindev.tierify.screen.ReforgeScreenHandler;
import elocindev.tierify.screen.client.ReforgeScreen;
import elocindev.tierify.screen.client.widget.ReforgeTab;

@Environment(EnvType.CLIENT)
public class TierifyClient implements ClientModInitializer {

    // map for storing attributes before logging into a server
    public static final Map<Identifier, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    public static final List<BorderTemplate> BORDER_TEMPLATES = new ArrayList<BorderTemplate>();

    private static final Identifier REFORGE_TAB_ICON = Identifier.of("tiered:textures/gui/reforge_tab_icon.png");
    
    @Override
    public void onInitializeClient() {
        registerAttributeSyncHandler();
        registerReforgeItemSyncHandler();
        HandledScreens.<ReforgeScreenHandler, ReforgeScreen>register(Tierify.REFORGE_SCREEN_HANDLER_TYPE, ReforgeScreen::new);
        TieredClientPacket.init();
        // TODO(1.21.1-stabilization): Restore optional tooltip border addon once core tooltip text flow is stable.
        // ResourceManagerHelper.get(ResourceType.CLIENT_RESOURCES).registerReloadListener(new TooltipBorderLoader());

        // TODO(1.21.1-stabilization): Restore custom Anvil tab navigation after reforge flow is fully validated.
        // TabRegistry.registerOtherTab(new AnvilTab(Text.translatable("container.repair"), ANVIL_TAB_ICON, 0, AnvilScreen.class), AnvilScreen.class);
        // TODO(1.21.1-stabilization): Keep custom Reforge/Anvil tab UI disabled; do not inject extra tabs into vanilla anvil screen.
        // TabRegistry.registerOtherTab(new ReforgeTab(Text.translatable("screen.tiered.reforging_screen"), REFORGE_TAB_ICON, 1, ReforgeScreen.class), AnvilScreen.class);
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
                    for (Identifier id : baseItemIds) {
                        items.add(Registries.ITEM.get(id));
                    }
                    Tierify.REFORGE_DATA_LOADER.putReforgeBaseItems(targetItem, items);
                });
            });
        });
    }
}
