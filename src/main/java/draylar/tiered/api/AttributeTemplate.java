package draylar.tiered.api;

import com.google.common.collect.Multimap;
import com.google.gson.annotations.SerializedName;

import elocindev.tierify.Tierify;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;

/**
 * Stores information on an AttributeModifier template applied to an ItemStack.
 *
 * The ID of the AttributeTemplate is the logical ID used to determine what "type" of attribute of is. An EntityAttributeModifier has: - a UUID, which is a mythic identifier to separate different
 * attributes of the same type - a name, which is used for generating a non-specified UUID and displaying in tooltips in some context - an amount, which is used in combination with the operation to
 * modify the final relevant value - a modifier, which can be something such as addition or subtraction
 *
 * The EquipmentSlot is used to only apply this template to certain items.
 */
@SuppressWarnings({"null"})
public class AttributeTemplate {

    @SerializedName("type")
    private final String attributeTypeID;

    @SerializedName("modifier")
    private final AttributeModifier entityAttributeModifier;

    @SerializedName("required_equipment_slots")
    private final EquipmentSlot[] requiredEquipmentSlots;

    @SerializedName("optional_equipment_slots")
    private final EquipmentSlot[] optionalEquipmentSlots;

    public AttributeTemplate(String attributeTypeID, AttributeModifier entityAttributeModifier, EquipmentSlot[] requiredEquipmentSlots, EquipmentSlot[] optionalEquipmentSlots) {
        this.attributeTypeID = attributeTypeID;
        this.entityAttributeModifier = entityAttributeModifier;
        this.requiredEquipmentSlots = requiredEquipmentSlots;
        this.optionalEquipmentSlots = optionalEquipmentSlots;
    }

    public EquipmentSlot[] getRequiredEquipmentSlots() {
        return requiredEquipmentSlots;
    }

    public EquipmentSlot[] getOptionalEquipmentSlots() {
        return optionalEquipmentSlots;
    }

    public AttributeModifier getEntityAttributeModifier() {
        return entityAttributeModifier;
    }

    public String getAttributeTypeID() {
        return attributeTypeID;
    }

    /**
     * Uses this {@link AttributeTemplate} to create an {@link AttributeModifier}, which is placed into the given {@link Multimap}.
     * <p>
     * Note that this method assumes the given {@link Multimap} is mutable.
     *
     * @param multimap map to add {@link AttributeTemplate}
     * @param slot
     */
    public void realize(Multimap<Holder<Attribute>, AttributeModifier> multimap, EquipmentSlot slot) {
        Identifier modifierId = Identifier.fromNamespaceAndPath(entityAttributeModifier.id().getNamespace(), entityAttributeModifier.id().getPath() + "_" + slot.getName());
        AttributeModifier cloneModifier = new AttributeModifier(modifierId, entityAttributeModifier.amount(), entityAttributeModifier.operation());

        var key = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(attributeTypeID));
        if (key.isEmpty()) {
            Tierify.LOGGER.warn(String.format("%s was referenced as an attribute type, but it does not exist! A data file in /tiered/item_attributes/ has an invalid type property.", attributeTypeID));
        } else {
            multimap.put(key.get(), cloneModifier);
        }
    }
}
