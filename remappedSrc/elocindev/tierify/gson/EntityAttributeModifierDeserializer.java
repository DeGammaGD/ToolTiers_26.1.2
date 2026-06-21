package elocindev.tierify.gson;

import com.google.gson.*;
import java.lang.reflect.Type;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

public class EntityAttributeModifierDeserializer implements JsonDeserializer<AttributeModifier> {

    private static final String JSON_NAME_KEY = "name";
    private static final String JSON_AMOUNT_KEY = "amount";
    private static final String JSON_OPERATION_KEY = "operation";

    private static AttributeModifier.Operation parseOperation(String operationName) {
        String normalized = operationName.toUpperCase();
        if ("ADDITION".equals(normalized)) {
            return AttributeModifier.Operation.ADD_VALUE;
        }
        if ("MULTIPLY_BASE".equals(normalized)) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_BASE;
        }
        if ("MULTIPLY_TOTAL".equals(normalized)) {
            return AttributeModifier.Operation.ADD_MULTIPLIED_TOTAL;
        }
        return AttributeModifier.Operation.valueOf(normalized);
    }

    @Override
    public AttributeModifier deserialize(JsonElement json, Type typeOfT, JsonDeserializationContext context) throws JsonParseException {
        JsonObject jsonObject = json.getAsJsonObject();

        JsonElement name = getJsonElement(jsonObject, JSON_NAME_KEY, "Entity Attribute Modifier requires a name!");
        JsonElement amount = getJsonElement(jsonObject, JSON_AMOUNT_KEY, "Entity Attribute Modifier requires an amount!");
        JsonElement operation = getJsonElement(jsonObject, JSON_OPERATION_KEY, "Entity Attribute Modifier requires an operation!");

        return new AttributeModifier(ResourceLocation.parse(name.getAsString()), amount.getAsDouble(), parseOperation(operation.getAsString()));
    }

    private JsonElement getJsonElement(JsonObject jsonObject, String jsonNameKey, String s) {
        JsonElement name;

        if (jsonObject.has(jsonNameKey))
            name = jsonObject.get(jsonNameKey);
        else
            throw new JsonParseException(s);

        return name;
    }
}
