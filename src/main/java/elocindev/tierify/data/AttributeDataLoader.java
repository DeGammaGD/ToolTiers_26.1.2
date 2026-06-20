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
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.resource.JsonDataLoader;
import net.minecraft.resource.ResourceManager;
import net.minecraft.text.Style;
import net.minecraft.text.TextColor;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.profiler.Profiler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

public class AttributeDataLoader extends JsonDataLoader implements SimpleSynchronousResourceReloadListener {
    public static final Gson GSON = new GsonBuilder().setPrettyPrinting().disableHtmlEscaping()
            .registerTypeAdapter(EntityAttributeModifier.class, new EntityAttributeModifierDeserializer())
            .registerTypeAdapter(EntityAttributeModifier.class, new EntityAttributeModifierSerializer())
            .registerTypeAdapter(EquipmentSlot.class, new EquipmentSlotSerializer())
            .registerTypeAdapter(EquipmentSlot.class, new EquipmentSlotDeserializer())
            .registerTypeAdapter(Style.class, new JsonDeserializer<Style>() {
                @Override
                public Style deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
                    JsonObject object = json.getAsJsonObject();
                    Style style = Style.EMPTY;
                    if (object.has("color")) {
                        String color = object.get("color").getAsString();
                        if (color.startsWith("#")) {
                            var parsed = TextColor.parse(color).result();
                            if (parsed.isPresent()) {
                                style = style.withColor(parsed.get());
                            }
                        } else {
                            Formatting formatting = Formatting.byName(color.toUpperCase(Locale.ROOT));
                            if (formatting != null) {
                                style = style.withColor(TextColor.fromFormatting(formatting));
                            }
                        }
                    }
                    if (object.has("bold") && object.get("bold").getAsBoolean()) style = style.withBold(true);
                    if (object.has("italic") && object.get("italic").getAsBoolean()) style = style.withItalic(true);
                    if (object.has("underlined") && object.get("underlined").getAsBoolean()) style = style.withUnderline(true);
                    if (object.has("strikethrough") && object.get("strikethrough").getAsBoolean()) style = style.withStrikethrough(true);
                    if (object.has("obfuscated") && object.get("obfuscated").getAsBoolean()) style = style.withObfuscated(true);
                    return style;
                }
            })
            .create();

    private static final String PARSING_ERROR_MESSAGE = "Parsing error loading recipe {}";
    private static final String LOADED_RECIPES_MESSAGE = "Loaded {} recipes";
    private static final Logger LOGGER = LogManager.getLogger();

    private Map<Identifier, PotentialAttribute> itemAttributes = new HashMap<>();

    public AttributeDataLoader() {
        super(GSON, "item_attributes");
    }

    @Override
    protected void apply(Map<Identifier, JsonElement> loader, ResourceManager manager, Profiler profiler) {
        Map<Identifier, PotentialAttribute> readItemAttributes = Maps.newHashMap();

        for (Map.Entry<Identifier, JsonElement> entry : loader.entrySet()) {
            Identifier identifier = entry.getKey();

            try {
                PotentialAttribute itemAttribute = GSON.fromJson(entry.getValue(), PotentialAttribute.class);
                readItemAttributes.put(Identifier.of(itemAttribute.getID()), itemAttribute);
            } catch (IllegalArgumentException | JsonParseException exception) {
                LOGGER.error(PARSING_ERROR_MESSAGE, identifier, exception);
            }
        }

        itemAttributes = readItemAttributes;
        LOGGER.info(LOADED_RECIPES_MESSAGE, readItemAttributes.size());
    }

    public Map<Identifier, PotentialAttribute> getItemAttributes() {
        return itemAttributes;
    }

    @Override
    public Identifier getFabricId() {
        return Identifier.of("tiered", "item_attributes");
    }

    @Override
    public void reload(ResourceManager resourceManager) {
    }

}
