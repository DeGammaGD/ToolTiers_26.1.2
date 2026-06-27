package elocindev.tierify.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import com.google.gson.JsonArray;

import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.gson.EntityAttributeModifierDeserializer;
import elocindev.tierify.gson.EntityAttributeModifierSerializer;
import elocindev.tierify.gson.EquipmentSlotDeserializer;
import elocindev.tierify.gson.EquipmentSlotSerializer;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@SuppressWarnings({"null", "deprecation"})
public class AttributeDataLoader implements SimpleSynchronousResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeAdapter(AttributeModifier.class, new EntityAttributeModifierDeserializer())
            .registerTypeAdapter(AttributeModifier.class, new EntityAttributeModifierSerializer())
            .registerTypeAdapter(EquipmentSlot.class, new EquipmentSlotSerializer())
            .registerTypeAdapter(EquipmentSlot.class, new EquipmentSlotDeserializer())
            .registerTypeAdapter(Style.class, new JsonDeserializer<Style>() {
                @Override
                public Style deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    JsonObject object = json.getAsJsonObject();
                    Style style = Style.EMPTY;
                    if (object.has("color")) {
                        JsonElement colorElement = object.get("color");
                        String color = null;
                        if (colorElement.isJsonPrimitive()) {
                            color = colorElement.getAsString();
                        } else if (colorElement.isJsonObject()) {
                            JsonObject colorObject = colorElement.getAsJsonObject();
                            if (colorObject.has("string")) {
                                color = colorObject.get("string").getAsString();
                            } else if (colorObject.has("name")) {
                                color = colorObject.get("name").getAsString();
                            } else if (colorObject.has("rgb")) {
                                style = style.withColor(TextColor.fromRgb(colorObject.get("rgb").getAsInt()));
                            }
                        }
                        if (color != null) {
                            if (color.startsWith("#")) {
                                var parsed = TextColor.parseColor(color).result();
                                if (parsed.isPresent()) {
                                    style = style.withColor(parsed.get());
                                }
                            } else {
                                ChatFormatting formatting = ChatFormatting.getByName(color.toUpperCase(Locale.ROOT));
                                if (formatting != null) {
                                    style = style.withColor(TextColor.fromLegacyFormat(formatting));
                                }
                            }
                        }
                    }
                    if (object.has("bold") && object.get("bold").getAsBoolean()) style = style.withBold(true);
                    if (object.has("italic") && object.get("italic").getAsBoolean()) style = style.withItalic(true);
                    if (object.has("underlined") && object.get("underlined").getAsBoolean()) style = style.withUnderlined(true);
                    if (object.has("strikethrough") && object.get("strikethrough").getAsBoolean()) style = style.withStrikethrough(true);
                    if (object.has("obfuscated") && object.get("obfuscated").getAsBoolean()) style = style.withObfuscated(true);
                    return style;
                }
            })
            .create();

    private static final String PARSING_ERROR_MESSAGE = "Parsing error loading recipe {}";
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String ITEM_ATTRIBUTES_ROOT = "item_attributes";
    private static final String MODIFIER_POOLS_ROOT = "modifier_pools";
    private static final Identifier LEGACY_ALIAS_RESOURCE = Identifier.fromNamespaceAndPath("tiered", "item_attribute_aliases.json");

    private Map<Identifier, PotentialAttribute> itemAttributes = new HashMap<>();

    public AttributeDataLoader() {
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
        Map<Identifier, JsonElement> loader = Maps.newHashMap();
        resourceManager.listResources(ITEM_ATTRIBUTES_ROOT, id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try (InputStream stream = resourceRef.open(); InputStreamReader reader = new InputStreamReader(stream)) {
                JsonElement element = JsonParser.parseReader(reader);
                String path = id.getPath();
                String trimmed = path.substring((ITEM_ATTRIBUTES_ROOT + "/").length(), path.length() - ".json".length());
                loader.put(Identifier.fromNamespaceAndPath(id.getNamespace(), trimmed), element);
            } catch (Exception exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, id, exception);
            }
        });

        Map<Identifier, JsonArray> modifierPools = loadModifierPools(resourceManager);

        LOGGER.info("Loading {} attribute definitions", loader.size());
        Map<Identifier, PotentialAttribute> readItemAttributes = Maps.newHashMap();

        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            Identifier identifier = entry.getKey();

            try {
                JsonObject resolved = resolveModifierPool(entry.getValue(), identifier, modifierPools);
                PotentialAttribute itemAttribute = GSON.fromJson(resolved, PotentialAttribute.class);
                readItemAttributes.put(Identifier.parse(itemAttribute.getID()), itemAttribute);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, identifier, exception);
            }
        }

        applyLegacyAliases(resourceManager, readItemAttributes);

        itemAttributes = readItemAttributes;
        LOGGER.info("Loaded {} attribute definitions", readItemAttributes.size());
    }

    public Map<Identifier, PotentialAttribute> getItemAttributes() {
        return itemAttributes;
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.fromNamespaceAndPath("tiered", "item_attributes");
    }

    private static Map<Identifier, JsonArray> loadModifierPools(ResourceManager resourceManager) {
        Map<Identifier, JsonArray> pools = Maps.newHashMap();

        resourceManager.listResources(MODIFIER_POOLS_ROOT, id -> id.getPath().endsWith(".json")).forEach((id, resourceRef) -> {
            try (InputStream stream = resourceRef.open(); InputStreamReader reader = new InputStreamReader(stream)) {
                JsonElement element = JsonParser.parseReader(reader);
                String path = id.getPath();
                String trimmed = path.substring((MODIFIER_POOLS_ROOT + "/").length(), path.length() - ".json".length());
                Identifier poolId = Identifier.fromNamespaceAndPath(id.getNamespace(), trimmed);

                JsonArray poolAttributes;
                if (element.isJsonArray()) {
                    poolAttributes = element.getAsJsonArray();
                } else if (element.isJsonObject() && element.getAsJsonObject().has("attributes")
                        && element.getAsJsonObject().get("attributes").isJsonArray()) {
                    poolAttributes = element.getAsJsonObject().getAsJsonArray("attributes");
                } else {
                    LOGGER.error("Invalid modifier pool format for {}", poolId);
                    return;
                }

                pools.put(poolId, poolAttributes);
            } catch (Exception exception) {
                LOGGER.error("Failed loading modifier pool {}", id, exception);
            }
        });

        LOGGER.info("Loaded {} modifier pools", pools.size());
        return pools;
    }

    private static JsonObject resolveModifierPool(JsonElement source,
                                                  Identifier attributeId,
                                                  Map<Identifier, JsonArray> modifierPools) {
        JsonObject object = source.getAsJsonObject().deepCopy();
        if (!object.has("modifier_pool")) {
            return object;
        }

        String rawPoolId = object.get("modifier_pool").getAsString();
        Identifier poolId = parsePoolIdentifier(rawPoolId, attributeId);
        JsonArray poolAttributes = modifierPools.get(poolId);

        if (poolAttributes == null) {
            throw new JsonParseException("Unknown modifier_pool '" + rawPoolId + "' in attribute " + attributeId);
        }

        object.add("attributes", poolAttributes.deepCopy());
        object.remove("modifier_pool");
        return object;
    }

    private static Identifier parsePoolIdentifier(String rawPoolId, Identifier attributeId) {
        if (rawPoolId.contains(":")) {
            return Identifier.parse(rawPoolId);
        }
        return Identifier.fromNamespaceAndPath(attributeId.getNamespace(), rawPoolId);
    }

    private static void applyLegacyAliases(ResourceManager resourceManager,
                                           Map<Identifier, PotentialAttribute> readItemAttributes) {
        Optional<Resource> aliasResource = resourceManager.getResource(LEGACY_ALIAS_RESOURCE);
        if (aliasResource.isEmpty()) {
            return;
        }

        int aliasCount = 0;
        try (InputStream stream = aliasResource.get().open(); InputStreamReader reader = new InputStreamReader(stream)) {
            JsonElement root = JsonParser.parseReader(reader);
            if (!root.isJsonObject()) {
                LOGGER.error("Legacy alias file {} is not a JSON object", LEGACY_ALIAS_RESOURCE);
                return;
            }

            JsonObject object = root.getAsJsonObject();
            if (!object.has("aliases") || !object.get("aliases").isJsonObject()) {
                LOGGER.error("Legacy alias file {} is missing 'aliases' object", LEGACY_ALIAS_RESOURCE);
                return;
            }

            JsonObject aliases = object.getAsJsonObject("aliases");
            for (Map.Entry<String, JsonElement> entry : aliases.entrySet()) {
                JsonElement value = entry.getValue();
                if (!(value instanceof JsonPrimitive primitive) || !primitive.isString()) {
                    LOGGER.warn("Skipping alias {} in {} because target is not a string", entry.getKey(), LEGACY_ALIAS_RESOURCE);
                    continue;
                }

                Identifier aliasId = Identifier.parse(entry.getKey());
                Identifier canonicalId = Identifier.parse(primitive.getAsString());
                PotentialAttribute canonical = readItemAttributes.get(canonicalId);
                if (canonical == null) {
                    LOGGER.warn("Skipping alias {} -> {} because canonical id is missing", aliasId, canonicalId);
                    continue;
                }

                PotentialAttribute aliasAttribute = new PotentialAttribute(
                        aliasId.toString(),
                        canonical.getVerifiers(),
                        0,
                        canonical.getStyle(),
                        canonical.getAttributes(),
                        canonical.getNbtValues()
                );
                readItemAttributes.put(aliasId, aliasAttribute);
                aliasCount++;
            }
        } catch (Exception exception) {
            LOGGER.error("Failed loading legacy aliases from {}", LEGACY_ALIAS_RESOURCE, exception);
            return;
        }

        LOGGER.info("Loaded {} legacy item attribute aliases", aliasCount);
    }

}
