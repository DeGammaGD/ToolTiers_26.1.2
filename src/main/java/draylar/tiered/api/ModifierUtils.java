package draylar.tiered.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.libz.util.SortList;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifierSlot;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import elocindev.tierify.Tierify;
import elocindev.tierify.compat.ItemBordersCompat;

public class ModifierUtils {

    private static NbtCompound getCustomData(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        NbtCompound result = component != null ? component.copyNbt() : new NbtCompound();
        if (component != null && result.contains(Tierify.NBT_SUBTAG_KEY)) {
            Tierify.LOGGER.info("Tier read from {} -> {}", Registries.ITEM.getId(stack.getItem()), result.getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
        }
        return result;
    }

    private static void setCustomData(ItemStack stack, NbtCompound compound) {
        if (compound.contains(Tierify.NBT_SUBTAG_KEY)) {
            Tierify.LOGGER.info("Tier write to {} -> {}", Registries.ITEM.getId(stack.getItem()), compound.getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
        } else {
            Tierify.LOGGER.info("Tier write cleared for {}", Registries.ITEM.getId(stack.getItem()));
        }
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(compound));
    }

    public static boolean hasTier(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        boolean hasTier = component != null && component.copyNbt().contains(Tierify.NBT_SUBTAG_KEY);
        Tierify.LOGGER.info("Tier presence check for {} -> {}", Registries.ITEM.getId(stack.getItem()), hasTier);
        return hasTier;
    }

    public static boolean hasTierMarker(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        boolean hasMarker = component != null && component.copyNbt().contains(Tierify.NBT_SUBTAG_MARKER_KEY) && component.copyNbt().getBoolean(Tierify.NBT_SUBTAG_MARKER_KEY);
        Tierify.LOGGER.info("Tier marker check for {} -> {}", Registries.ITEM.getId(stack.getItem()), hasMarker);
        return hasMarker;
    }

    public static void applyTierToItem(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }
        setItemStackAttribute(null, stack, false);
    }

    public static void logTierDebug(String source, ItemStack stack) {
        Identifier assignedTier = getAttributeID(stack);
        Tierify.LOGGER.info("[TIER DEBUG] source: {} item: {} assigned tier: {}", source, Registries.ITEM.getId(stack.getItem()), assignedTier);
    }

    /**
     * Returns the ID of a random attribute that is valid for the given {@link Item} in {@link Identifier} form.
     * <p>
     * If there is no valid attribute for the given {@link Item}, null is returned.
     *
     * @param item      {@link Item} to generate a random attribute for
     * @return          id of random attribute for item in {@link Identifier} form, or null if there are no valid options
     */
    @Nullable
    public static Identifier getRandomAttributeIDFor(@Nullable PlayerEntity playerEntity, Item item, boolean reforge) {
        List<Identifier> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();
        // collect all valid attributes for the given item and their weights

        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(Registries.ITEM.getId(item)) && (attribute.getWeight() > 0 || reforge)) {
                potentialAttributes.add(Identifier.of(attribute.getID()));
                attributeWeights.add(reforge ? attribute.getWeight() + 1 : attribute.getWeight());
            }
        });
        if (potentialAttributes.size() <= 0) {
            Tierify.LOGGER.info("No tier candidates found for {} (reforge={})", Registries.ITEM.getId(item), reforge);
            return null;
        }

        if (reforge && attributeWeights.size() > 2) {
            SortList.concurrentSort(attributeWeights, attributeWeights, potentialAttributes);
            int maxWeight = attributeWeights.get(attributeWeights.size() - 1);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > maxWeight / 2) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * Tierify.CONFIG.reforgeModifier));
                }
            }
        }
        // Luck
        if (playerEntity != null) {
            int luckMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > luckMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * (1.0f - Tierify.CONFIG.luckReforgeModifier * playerEntity.getLuck())));
                }
            }
        }

        if (potentialAttributes.size() > 0) {
            int totalWeight = 0;
            for (Integer weight : attributeWeights) {
                totalWeight += weight.intValue();
            }
            int randomChoice = new Random().nextInt(totalWeight);
            SortList.concurrentSort(attributeWeights, attributeWeights, potentialAttributes);

            for (int i = 0; i < attributeWeights.size(); i++) {
                if (randomChoice < attributeWeights.get(i)) {
                    Tierify.LOGGER.info("Selected tier {} for {} (reforge={})", potentialAttributes.get(i), Registries.ITEM.getId(item), reforge);
                    return potentialAttributes.get(i);
                }
                randomChoice -= attributeWeights.get(i);
            }
            // If random choice didn't work
            Identifier fallback = potentialAttributes.get(new Random().nextInt(potentialAttributes.size()));
            Tierify.LOGGER.info("Selected fallback tier {} for {} (reforge={})", fallback, Registries.ITEM.getId(item), reforge);
            return fallback;
        } else
            return null;
    }

    /**
     * Returns a list of all attribute IDs that contain the specified quality in their identifier.
     *
     * @param quality       The quality substring to look for in the attribute identifiers (e.g., "mythic").
     * @return              List of attribute IDs that contain the specified quality substring.
     */
    public static List<Identifier> getAttributeIDsForQuality(String quality, Item item) {
        List<Identifier> matchingAttributes = new ArrayList<>();
        
        // iterate over all attributes and add matching ones to the list
        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(Registries.ITEM.getId(item)) && id.toString().contains(quality.toLowerCase())) {
                matchingAttributes.add(id);
            }
        });
        
        return matchingAttributes;
    }

/**
     * Returns a random attribute ID from the attributes that contain any of the specified quality substrings in their identifier,
     * considering the weights of the attributes.
     *
     * @param qualities A list of quality substrings to look for in the attribute identifiers (e.g., "mythic", "legendary").
     * @param item      The item for which the attribute is being searched.
     * 
     * @return A random attribute ID that contains one of the specified quality substrings, considering attribute weights, or null if none are found.
     */
    public static Identifier getRandomAttributeForQuality(List<String> qualities, Item item, boolean reforge) {
        List<Identifier> matchingAttributes = new ArrayList<>();
        List<Integer> matchingAttributeWeights = new ArrayList<>();

        // Collect all matching attributes for the given qualities and their weights
        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (attribute.isValid(Registries.ITEM.getId(item)) && qualities.stream().anyMatch(quality -> id.toString().contains(quality.toLowerCase())) && (attribute.getWeight() > 0 || reforge)) {
                matchingAttributes.add(id);
                matchingAttributeWeights.add(reforge ? attribute.getWeight() + 1 : attribute.getWeight());
            }
        });

        // Return null if no matching attributes are found
        if (matchingAttributes.isEmpty()) {
            return null;
        }

        // Calculate the total weight
        int totalWeight = matchingAttributeWeights.stream().mapToInt(Integer::intValue).sum();
        int randomIndex = new Random().nextInt(totalWeight);
        
        // Choose a random attribute based on weight
        for (int i = 0; i < matchingAttributes.size(); i++) {
            randomIndex -= matchingAttributeWeights.get(i);
            if (randomIndex < 0) {
                return matchingAttributes.get(i);
            }
        }

        // Fallback, should not be reached due to the weight calculation
        return null;
    }

    public static void setItemStackAttribute(Identifier potentialAttributeID, ItemStack stack) {
        if (potentialAttributeID != null) {
            Tierify.LOGGER.info("Assigning tier {} to {}", potentialAttributeID, Registries.ITEM.getId(stack.getItem()));
            NbtCompound root = getCustomData(stack);
            PotentialAttribute assignedAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.of(potentialAttributeID.toString()));
            if (assignedAttribute == null) {
                Tierify.LOGGER.warn("Tier {} has no attribute template while assigning to {}", potentialAttributeID, Registries.ITEM.getId(stack.getItem()));
                return;
            }

            NbtCompound tiered = new NbtCompound();
            tiered.putString(Tierify.NBT_SUBTAG_DATA_KEY, potentialAttributeID.toString());
            root.put(Tierify.NBT_SUBTAG_KEY, tiered);
            root.putBoolean(Tierify.NBT_SUBTAG_MARKER_KEY, true);

            if (!Tierify.CORE_STABILIZATION_MODE) {
                // TODO(1.21.1-stabilization): Restore ItemBorders compatibility writes after core tier flow is stable.
                NbtCompound colors = new NbtCompound();
                colors.putString("top", ItemBordersCompat.getColorForIdentifier(potentialAttributeID));
                colors.putString("bottom", ItemBordersCompat.getColorForIdentifier(potentialAttributeID));
                root.put("itemborders_colors", colors);
            }

            HashMap<String, Object> nbtMap = assignedAttribute.getNbtValues();
            Tierify.LOGGER.info("Attribute json for {} -> {}", potentialAttributeID, nbtMap);

            // add durability nbt
            List<AttributeTemplate> attributeList = assignedAttribute.getAttributes();
            for (int i = 0; i < attributeList.size(); i++) {
                if (attributeList.get(i).getAttributeTypeID().equals("tiered:generic.durable")) {
                    if (nbtMap == null) {
                        nbtMap = new HashMap<>();
                    }
                    nbtMap.put("durable", (double) Math.round(attributeList.get(i).getEntityAttributeModifier().value() * 100.0) / 100.0);
                    break;
                }
            }

            // add nbtMap
            if (nbtMap != null) {
                for (HashMap.Entry<String, Object> entry : nbtMap.entrySet()) {
                    String key = entry.getKey();
                    Object value = entry.getValue();

                    // json list will get read as ArrayList class
                    // json map will get read as linkedtreemap
                    // json integer is read by gson -> always double
                    if (value instanceof String) {
                        root.putString(key, (String) value);
                    } else if (value instanceof Boolean) {
                        root.putBoolean(key, (boolean) value);
                    } else if (value instanceof Double) {
                        if (Math.abs((double) value) % 1.0 < 0.0001D) {
                            root.putInt(key, (int) Math.round((double) value));
                        } else {
                            root.putDouble(key, Math.round((double) value * 100.0) / 100.0);
                        }
                    }
                }
            }

            setCustomData(stack, root);
            rebuildAttributeModifiersComponent(stack);
            Tierify.LOGGER.info("[TierifyDebug][Create] item={} tier={} attributes={}", Registries.ITEM.getId(stack.getItem()), potentialAttributeID, attributeList);
        }
    }


    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge, ItemStack reforgeMaterial) {
        if (reforge && reforgeMaterial != null) {
            List<String> qualities = null;

            if (reforgeMaterial.isIn(TieredItemTags.TIER_1_ITEM)) {
                qualities = Tierify.CONFIG.tier_1_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_2_ITEM)) {
                qualities = Tierify.CONFIG.tier_2_qualities;
            } else if (reforgeMaterial.isIn(TieredItemTags.TIER_3_ITEM)) {
                qualities = Tierify.CONFIG.tier_3_qualities;
            }

            if (qualities != null) {
                Identifier possibleAttribute = getRandomAttributeForQuality(qualities, stack.getItem(), reforge);
                if (possibleAttribute != null) {
                    Tierify.LOGGER.info("Reforge material {} produced tier {} for {}", Registries.ITEM.getId(reforgeMaterial.getItem()), possibleAttribute, Registries.ITEM.getId(stack.getItem()));
                    setItemStackAttribute(possibleAttribute, stack);
                    return;
                }
            }
        }

        setItemStackAttribute(playerEntity, stack, reforge);
    }

    public static void setItemStackAttribute(@Nullable PlayerEntity playerEntity, ItemStack stack, boolean reforge) {
        NbtCompound customData = getCustomData(stack);
        boolean alreadyTiered = customData.contains(Tierify.NBT_SUBTAG_KEY);
        boolean hasMarker = customData.contains(Tierify.NBT_SUBTAG_MARKER_KEY) && customData.getBoolean(Tierify.NBT_SUBTAG_MARKER_KEY);
        if (alreadyTiered || hasMarker) {
            Tierify.LOGGER.info("Skipping tier generation for {} (alreadyTiered={}, marker={}, reforge={})", Registries.ITEM.getId(stack.getItem()), alreadyTiered, hasMarker, reforge);
            return;
        }

        Tierify.LOGGER.info("Generating tier for {} (reforge={})", Registries.ITEM.getId(stack.getItem()), reforge);
        setItemStackAttribute(ModifierUtils.getRandomAttributeIDFor(playerEntity, stack.getItem(), reforge), stack);
    }

    public static void removeItemStackAttribute(ItemStack itemStack) {
        NbtCompound root = getCustomData(itemStack);
        if (root.contains(Tierify.NBT_SUBTAG_KEY)) {
            Tierify.LOGGER.info("Removing tier from {}", Registries.ITEM.getId(itemStack.getItem()));
            NbtCompound tiered = root.getCompound(Tierify.NBT_SUBTAG_KEY);
            Identifier tier = Identifier.of(tiered.getString(Tierify.NBT_SUBTAG_DATA_KEY));
            if (Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier) != null) {
                HashMap<String, Object> nbtMap = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier).getNbtValues();
                List<String> nbtKeys = new ArrayList<>();
                if (nbtMap != null) {
                    nbtKeys.addAll(nbtMap.keySet());
                }

                List<AttributeTemplate> attributeList = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier).getAttributes();
                for (int i = 0; i < attributeList.size(); i++) {
                    if (attributeList.get(i).getAttributeTypeID().equals("tiered:generic.durable")) {
                        nbtKeys.add("durable");
                        break;
                    }
                }

                if (!nbtKeys.isEmpty()) {
                    for (int i = 0; i < nbtKeys.size(); i++) {
                        if (!nbtKeys.get(i).equals("Damage")) {
                            root.remove(nbtKeys.get(i));
                        }
                    }
                }
            }
            root.remove(Tierify.NBT_SUBTAG_KEY);
            root.remove(Tierify.NBT_SUBTAG_MARKER_KEY);
            setCustomData(itemStack, root);
            rebuildAttributeModifiersComponent(itemStack);
        }
    }

    @Nullable
    public static Identifier getAttributeID(ItemStack itemStack) {
        NbtCompound root = getCustomData(itemStack);
        if (root.contains(Tierify.NBT_SUBTAG_KEY)) {
            Identifier tierId = Identifier.of(root.getCompound(Tierify.NBT_SUBTAG_KEY).getString(Tierify.NBT_SUBTAG_DATA_KEY));
            Tierify.LOGGER.info("Resolved tier id for {} -> {}", Registries.ITEM.getId(itemStack.getItem()), tierId);
            return tierId;
        }
        Tierify.LOGGER.info("Resolved tier id for {} -> none", Registries.ITEM.getId(itemStack.getItem()));
        return null;
    }

    public static Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> buildTierAttributeMap(ItemStack itemStack, EquipmentSlot slot) {
        Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> modifiers = HashMultimap.create();
        Identifier tierId = getAttributeID(itemStack);
        if (tierId == null) {
            Tierify.LOGGER.info("No tier data found while building modifiers for {} in {}", Registries.ITEM.getId(itemStack.getItem()), slot.getName());
            return modifiers;
        }

        PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
        if (potentialAttribute == null) {
            Tierify.LOGGER.info("Tier {} missing attribute definition for {}", tierId, Registries.ITEM.getId(itemStack.getItem()));
            return modifiers;
        }

        Tierify.LOGGER.info("Generating modifiers for {} using tier {} and attributes {}", Registries.ITEM.getId(itemStack.getItem()), tierId, potentialAttribute.getAttributes());
        for (AttributeTemplate template : potentialAttribute.getAttributes()) {
            EquipmentSlot[] requiredSlots = template.getRequiredEquipmentSlots();
            EquipmentSlot[] optionalSlots = template.getOptionalEquipmentSlots();

            boolean applies = false;
            if (requiredSlots != null) {
                for (EquipmentSlot requiredSlot : requiredSlots) {
                    if (requiredSlot == slot) {
                        applies = true;
                        break;
                    }
                }
            }
            if (!applies && optionalSlots != null) {
                for (EquipmentSlot optionalSlot : optionalSlots) {
                    if (optionalSlot == slot) {
                        applies = true;
                        break;
                    }
                }
            }

            if (applies) {
                template.realize(modifiers, slot);
            }
        }

        Tierify.LOGGER.info("Generated modifiers for {} in {} -> {}", Registries.ITEM.getId(itemStack.getItem()), slot.getName(), modifiers);
        return modifiers;
    }

    public static void rebuildAttributeModifiersComponent(ItemStack itemStack) {
        AttributeModifiersComponent baseComponent = new ItemStack(itemStack.getRegistryEntry(), itemStack.getCount())
                .getOrDefault(DataComponentTypes.ATTRIBUTE_MODIFIERS, AttributeModifiersComponent.DEFAULT);

        AttributeModifiersComponent.Builder builder = AttributeModifiersComponent.builder();
        for (AttributeModifiersComponent.Entry entry : baseComponent.modifiers()) {
            builder.add(entry.attribute(), entry.modifier(), entry.slot());
        }

        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!Tierify.isPreferredEquipmentSlot(itemStack, slot)) {
                continue;
            }
            Multimap<RegistryEntry<EntityAttribute>, EntityAttributeModifier> generated = buildTierAttributeMap(itemStack, slot);
            for (java.util.Map.Entry<RegistryEntry<EntityAttribute>, EntityAttributeModifier> entry : generated.entries()) {
                builder.add(entry.getKey(), entry.getValue(), AttributeModifierSlot.forEquipmentSlot(slot));
            }
        }

        AttributeModifiersComponent rebuilt = builder.build().withShowInTooltip(baseComponent.showInTooltip());
        itemStack.set(DataComponentTypes.ATTRIBUTE_MODIFIERS, rebuilt);
        Tierify.LOGGER.info("Rebuilt attribute modifiers for {} -> {}", Registries.ITEM.getId(itemStack.getItem()), rebuilt.modifiers());
    }

}