package elocindev.tierify;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import draylar.tiered.api.BorderTemplate;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.data.AttributeDataLoader;
import elocindev.tierify.data.TooltipBorderLoader;

@Environment(EnvType.CLIENT)
@SuppressWarnings({"null", "deprecation"})
public class TierifyClient implements ClientModInitializer {

    // map for storing attributes before logging into a server
    public static final Map<Identifier, PotentialAttribute> CACHED_ATTRIBUTES = new HashMap<>();

    public static final List<BorderTemplate> BORDER_TEMPLATES = new ArrayList<BorderTemplate>();
    
    @Override
    public void onInitializeClient() {
        registerAttributeSyncHandler();
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

}
