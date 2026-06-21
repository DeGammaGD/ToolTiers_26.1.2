package elocindev.tierify.data;

import com.google.common.collect.Maps;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonDeserializationContext;
import com.google.gson.JsonDeserializer;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;

import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.gson.EntityAttributeModifierDeserializer;
import elocindev.tierify.gson.EntityAttributeModifierSerializer;
import elocindev.tierify.gson.EquipmentSlotDeserializer;
import elocindev.tierify.gson.EquipmentSlotSerializer;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Style;
import net.minecraft.network.chat.TextColor;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.server.packs.resources.SimpleJsonResourceReloadListener;
import net.minecraft.util.profiling.ProfilerFiller;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

public class AttributeDataLoader extends SimpleJsonResourceReloadListener implements SimpleSynchronousResourceReloadListener {
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

    private Map<ResourceLocation, PotentialAttribute> itemAttributes = new HashMap<>();

    public AttributeDataLoader() {
        super(GSON, "item_attributes");
    }

    @Override
    protected void apply(Map<ResourceLocation, JsonElement> loader, ResourceManager manager, ProfilerFiller profiler) {
        LOGGER.info("Loading {} attribute definitions", loader.size());
        Map<ResourceLocation, PotentialAttribute> readItemAttributes = Maps.newHashMap();

        for (Map.Entry<ResourceLocation, JsonElement> entry : loader.entrySet()) {
            ResourceLocation identifier = entry.getKey();

            try {
                PotentialAttribute itemAttribute = GSON.fromJson(entry.getValue(), PotentialAttribute.class);
                readItemAttributes.put(ResourceLocation.parse(itemAttribute.getID()), itemAttribute);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, identifier, exception);
            }
        }

        itemAttributes = readItemAttributes;
        LOGGER.info("Loaded {} attribute definitions", readItemAttributes.size());
    }

    public Map<ResourceLocation, PotentialAttribute> getItemAttributes() {
        return itemAttributes;
    }

    @Override
    public ResourceLocation getFabricId() {
        return ResourceLocation.fromNamespaceAndPath("tiered", "item_attributes");
    }

    @Override
    public void onResourceManagerReload(ResourceManager resourceManager) {
    }

}
