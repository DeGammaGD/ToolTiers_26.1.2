package draylar.tiered.api;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.Attribute;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import net.minecraft.world.item.equipment.Equippable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.jetbrains.annotations.Nullable;

import elocindev.tierify.Tierify;
import elocindev.tierify.compat.ItemBordersCompat;

public class ModifierUtils {

    private static boolean isSpearDebugItem(Item item) {
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        String itemIdString = itemId == null ? "" : itemId.toString();
        return "minecraft:trident".equals(itemIdString) || itemIdString.endsWith(":spear") || itemIdString.contains("_spear");
    }

    private static <T> void sortByWeight(List<Integer> weights, List<T> values) {
        List<Integer> order = new ArrayList<>(weights.size());
        for (int i = 0; i < weights.size(); i++) {
            order.add(i);
        }

        order.sort(Comparator.comparingInt(weights::get));

        List<Integer> sortedWeights = new ArrayList<>(weights.size());
        List<T> sortedValues = new ArrayList<>(values.size());
        for (int index : order) {
            sortedWeights.add(weights.get(index));
            sortedValues.add(values.get(index));
        }

        weights.clear();
        weights.addAll(sortedWeights);
        values.clear();
        values.addAll(sortedValues);
    }

    private static CompoundTag getCustomData(ItemStack stack) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        CompoundTag result = component != null ? component.copyTag() : new CompoundTag();
        if (component != null && result.contains(Tierify.NBT_SUBTAG_KEY)) {
            String tierValue = result.getCompound(Tierify.NBT_SUBTAG_KEY).flatMap(t -> t.getString(Tierify.NBT_SUBTAG_DATA_KEY)).orElse("");
            Tierify.LOGGER.info("Tier read from {} -> {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), tierValue);
        }
        return result;
    }

    private static void setCustomData(ItemStack stack, CompoundTag compound) {
        if (compound.contains(Tierify.NBT_SUBTAG_KEY)) {
            String tierValue = compound.getCompound(Tierify.NBT_SUBTAG_KEY).flatMap(t -> t.getString(Tierify.NBT_SUBTAG_DATA_KEY)).orElse("");
            Tierify.LOGGER.info("Tier write to {} -> {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), tierValue);
        } else {
            Tierify.LOGGER.info("Tier write cleared for {}", BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(compound));
    }

    public static boolean hasTier(ItemStack stack) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        boolean hasTier = component != null && component.copyTag().contains(Tierify.NBT_SUBTAG_KEY);
        Tierify.LOGGER.info("Tier presence check for {} -> {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), hasTier);
        return hasTier;
    }

    public static boolean hasTierMarker(ItemStack stack) {
        CustomData component = stack.get(DataComponents.CUSTOM_DATA);
        boolean hasMarker = component != null && component.copyTag().contains(Tierify.NBT_SUBTAG_MARKER_KEY)
                && component.copyTag().getBoolean(Tierify.NBT_SUBTAG_MARKER_KEY).orElse(false);
        Tierify.LOGGER.info("Tier marker check for {} -> {}", BuiltInRegistries.ITEM.getKey(stack.getItem()), hasMarker);
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
        Tierify.LOGGER.info("[TIER DEBUG] source: {} item: {} assigned tier: {}", source, BuiltInRegistries.ITEM.getKey(stack.getItem()), assignedTier);
    }

    private static boolean templateAppliesToSlot(AttributeTemplate template, EquipmentSlot slot) {
        EquipmentSlot[] requiredSlots = template.getRequiredEquipmentSlots();
        if (requiredSlots != null) {
            for (EquipmentSlot requiredSlot : requiredSlots) {
                if (requiredSlot == slot) {
                    return true;
                }
            }
        }

        EquipmentSlot[] optionalSlots = template.getOptionalEquipmentSlots();
        if (optionalSlots != null) {
            for (EquipmentSlot optionalSlot : optionalSlots) {
                if (optionalSlot == slot) {
                    return true;
                }
            }
        }

        return false;
    }

    private static boolean isValidAttributeTypeId(String attributeTypeId) {
        if (attributeTypeId == null || attributeTypeId.isBlank()) {
            return false;
        }

        try {
            return BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(attributeTypeId)).isPresent();
        } catch (Exception ignored) {
            return false;
        }
    }

    private static int countUsableModifierTemplates(Item item, PotentialAttribute attribute) {
        if (attribute == null || attribute.getAttributes() == null || attribute.getAttributes().isEmpty()) {
            return 0;
        }

        ItemStack probeStack = new ItemStack(item.builtInRegistryHolder());
        int usable = 0;

        for (AttributeTemplate template : attribute.getAttributes()) {
            if (template == null || template.getEntityAttributeModifier() == null || !isValidAttributeTypeId(template.getAttributeTypeID())) {
                continue;
            }

            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!Tierify.isPreferredEquipmentSlot(probeStack, slot)) {
                    continue;
                }

                if (templateAppliesToSlot(template, slot)) {
                    usable++;
                    break;
                }
            }
        }

        return usable;
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
    public static Identifier getRandomAttributeIDFor(@Nullable Player playerEntity, Item item, boolean reforge) {
        List<Identifier> potentialAttributes = new ArrayList<>();
        List<Integer> attributeWeights = new ArrayList<>();
        Map<Identifier, Integer> modifierPoolByTier = new HashMap<>();
        boolean spearDebug = isSpearDebugItem(item);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);
        // collect all valid attributes for the given item and their weights

        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            boolean verifierResult = attribute.isValid(itemId);
            boolean weightAllowed = attribute.getWeight() > 0 || reforge;
            Identifier candidateId = Identifier.parse(attribute.getID());

            if (!verifierResult || !weightAllowed) {
                if (spearDebug) {
                    Tierify.LOGGER.info("[TierifyDebug][TierDecision] item={} tier={} verifierResult={} weightAllowed={} decision=skip", itemId, candidateId, verifierResult, weightAllowed);
                }
                return;
            }

            int modifierPool = countUsableModifierTemplates(item, attribute);
            if (modifierPool > 0) {
                potentialAttributes.add(candidateId);
                attributeWeights.add(reforge ? attribute.getWeight() + 1 : attribute.getWeight());
                modifierPoolByTier.put(candidateId, modifierPool);
                if (spearDebug) {
                    Tierify.LOGGER.info("[TierifyDebug][TierDecision] item={} tier={} verifierResult={} weightAllowed={} modifierPool={} decision=include", itemId, candidateId, verifierResult, weightAllowed, modifierPool);
                }
            } else {
                Tierify.LOGGER.info("Skipping tier {} for {} because modifier pool is empty", candidateId, BuiltInRegistries.ITEM.getKey(item));
                if (spearDebug) {
                    Tierify.LOGGER.info("[TierifyDebug][TierDecision] item={} tier={} verifierResult={} weightAllowed={} modifierPool={} decision=skip_pool_empty", itemId, candidateId, verifierResult, weightAllowed, modifierPool);
                }
            }
        });
        if (potentialAttributes.size() <= 0) {
            Tierify.LOGGER.info("No tier candidates found for {} (reforge={})", BuiltInRegistries.ITEM.getKey(item), reforge);
            return null;
        }

        Tierify.LOGGER.info("Tier candidate pool for {} (reforge={}) -> {}", BuiltInRegistries.ITEM.getKey(item), reforge, potentialAttributes.stream().map(id -> id + "[pool=" + modifierPoolByTier.getOrDefault(id, 0) + "]").toList());

        if (reforge && attributeWeights.size() > 2) {
            sortByWeight(attributeWeights, potentialAttributes);
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
            sortByWeight(attributeWeights, potentialAttributes);

            for (int i = 0; i < attributeWeights.size(); i++) {
                if (randomChoice < attributeWeights.get(i)) {
                    Tierify.LOGGER.info("Selected tier {} for {} (reforge={}) modifierPool={}", potentialAttributes.get(i), BuiltInRegistries.ITEM.getKey(item), reforge, modifierPoolByTier.getOrDefault(potentialAttributes.get(i), 0));
                    return potentialAttributes.get(i);
                }
                randomChoice -= attributeWeights.get(i);
            }
            // If random choice didn't work
            Identifier fallback = potentialAttributes.get(new Random().nextInt(potentialAttributes.size()));
            Tierify.LOGGER.info("Selected fallback tier {} for {} (reforge={}) modifierPool={}", fallback, BuiltInRegistries.ITEM.getKey(item), reforge, modifierPoolByTier.getOrDefault(fallback, 0));
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
            if (attribute.isValid(BuiltInRegistries.ITEM.getKey(item)) && id.toString().contains(quality.toLowerCase())) {
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
            if (attribute.isValid(BuiltInRegistries.ITEM.getKey(item)) && qualities.stream().anyMatch(quality -> id.toString().contains(quality.toLowerCase())) && (attribute.getWeight() > 0 || reforge)) {
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
            Tierify.LOGGER.info("Assigning tier {} to {}", potentialAttributeID, BuiltInRegistries.ITEM.getKey(stack.getItem()));
            CompoundTag root = getCustomData(stack);
            PotentialAttribute assignedAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.parse(potentialAttributeID.toString()));
            if (assignedAttribute == null) {
                Tierify.LOGGER.warn("Tier {} has no attribute template while assigning to {}", potentialAttributeID, BuiltInRegistries.ITEM.getKey(stack.getItem()));
                return;
            }

            CompoundTag tiered = new CompoundTag();
            tiered.putString(Tierify.NBT_SUBTAG_DATA_KEY, potentialAttributeID.toString());
            root.put(Tierify.NBT_SUBTAG_KEY, tiered);
            root.putBoolean(Tierify.NBT_SUBTAG_MARKER_KEY, true);

            CompoundTag colors = new CompoundTag();
            colors.putString("top", ItemBordersCompat.getColorForIdentifier(potentialAttributeID));
            colors.putString("bottom", ItemBordersCompat.getColorForIdentifier(potentialAttributeID));
            root.put("itemborders_colors", colors);

            HashMap<String, Object> nbtMap = assignedAttribute.getNbtValues();
            Tierify.LOGGER.info("Attribute json for {} -> {}", potentialAttributeID, nbtMap);

            // add durability nbt
            List<AttributeTemplate> attributeList = assignedAttribute.getAttributes();
            for (int i = 0; i < attributeList.size(); i++) {
                String attributeTypeId = attributeList.get(i).getAttributeTypeID();
                if ("tiered:generic.durable".equals(attributeTypeId)) {
                    if (nbtMap == null) {
                        nbtMap = new HashMap<>();
                    }
                    nbtMap.put("durable", (double) Math.round(attributeList.get(i).getEntityAttributeModifier().amount() * 100.0) / 100.0);
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
            int modifierPoolSize = countUsableModifierTemplates(stack.getItem(), assignedAttribute);
            int appliedCount = rebuildAttributeModifiersComponent(stack);
            Tierify.LOGGER.info("[TierifyDebug][ModifierAssign] item={} tierSelected={} modifierPoolFound={} modifiersAppliedCount={}", BuiltInRegistries.ITEM.getKey(stack.getItem()), potentialAttributeID, modifierPoolSize, appliedCount);
            if (appliedCount == 0) {
                Tierify.LOGGER.warn("No modifiers applied for item={} tier={} despite assignment; reason=pool_empty_or_slot_mismatch_or_invalid_attribute_type", BuiltInRegistries.ITEM.getKey(stack.getItem()), potentialAttributeID);
            }
            Tierify.LOGGER.info("[TierifyDebug][Create] item={} tier={} attributes={}", BuiltInRegistries.ITEM.getKey(stack.getItem()), potentialAttributeID, attributeList);
        } else {
            Tierify.LOGGER.warn("Tier assignment skipped for {} because no valid tier was selected", BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }


    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge, ItemStack reforgeMaterial) {
        if (reforge && reforgeMaterial != null) {
            List<String> qualities = null;

            if (reforgeMaterial.is(TieredItemTags.TIER_1_ITEM)) {
                qualities = Tierify.CONFIG.tier_1_qualities;
            } else if (reforgeMaterial.is(TieredItemTags.TIER_2_ITEM)) {
                qualities = Tierify.CONFIG.tier_2_qualities;
            } else if (reforgeMaterial.is(TieredItemTags.TIER_3_ITEM)) {
                qualities = Tierify.CONFIG.tier_3_qualities;
            }

            if (qualities != null) {
                Identifier possibleAttribute = getRandomAttributeForQuality(qualities, stack.getItem(), reforge);
                if (possibleAttribute != null) {
                    Tierify.LOGGER.info("Reforge material {} produced tier {} for {}", BuiltInRegistries.ITEM.getKey(reforgeMaterial.getItem()), possibleAttribute, BuiltInRegistries.ITEM.getKey(stack.getItem()));
                    setItemStackAttribute(possibleAttribute, stack);
                    return;
                }
            }
        }

        setItemStackAttribute(playerEntity, stack, reforge);
    }

    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge) {
        CompoundTag customData = getCustomData(stack);
        boolean alreadyTiered = customData.contains(Tierify.NBT_SUBTAG_KEY);
        boolean hasMarker = customData.contains(Tierify.NBT_SUBTAG_MARKER_KEY) && customData.getBoolean(Tierify.NBT_SUBTAG_MARKER_KEY).orElse(false);
        if (alreadyTiered || hasMarker) {
            Tierify.LOGGER.info("Skipping tier generation for {} (alreadyTiered={}, marker={}, reforge={})", BuiltInRegistries.ITEM.getKey(stack.getItem()), alreadyTiered, hasMarker, reforge);
            return;
        }

        Tierify.LOGGER.info("Generating tier for {} (reforge={})", BuiltInRegistries.ITEM.getKey(stack.getItem()), reforge);
        setItemStackAttribute(ModifierUtils.getRandomAttributeIDFor(playerEntity, stack.getItem(), reforge), stack);
    }

    public static void removeItemStackAttribute(ItemStack itemStack) {
        CompoundTag root = getCustomData(itemStack);
        if (root.contains(Tierify.NBT_SUBTAG_KEY)) {
            Tierify.LOGGER.info("Removing tier from {}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
            CompoundTag tiered = root.getCompound(Tierify.NBT_SUBTAG_KEY).orElse(new CompoundTag());
            Identifier tier = Identifier.parse(tiered.getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse(""));
            if (Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier) != null) {
                HashMap<String, Object> nbtMap = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier).getNbtValues();
                List<String> nbtKeys = new ArrayList<>();
                if (nbtMap != null) {
                    nbtKeys.addAll(nbtMap.keySet());
                }

                List<AttributeTemplate> attributeList = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier).getAttributes();
                for (int i = 0; i < attributeList.size(); i++) {
                    String attributeTypeId = attributeList.get(i).getAttributeTypeID();
                    if ("tiered:generic.durable".equals(attributeTypeId)) {
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
        CompoundTag root = getCustomData(itemStack);
        if (root.contains(Tierify.NBT_SUBTAG_KEY)) {
            CompoundTag tiered = root.getCompound(Tierify.NBT_SUBTAG_KEY).orElse(new CompoundTag());
            Identifier tierId = Identifier.parse(tiered.getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse(""));
            Tierify.LOGGER.info("Resolved tier id for {} -> {}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), tierId);
            return tierId;
        }
        Tierify.LOGGER.info("Resolved tier id for {} -> none", BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
        return null;
    }

    public static Multimap<Holder<Attribute>, AttributeModifier> buildTierAttributeMap(ItemStack itemStack, EquipmentSlot slot) {
        Multimap<Holder<Attribute>, AttributeModifier> modifiers = HashMultimap.create();
        Identifier tierId = getAttributeID(itemStack);
        if (tierId == null) {
            Tierify.LOGGER.info("No tier data found while building modifiers for {} in {}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), slot.getName());
            return modifiers;
        }

        PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
        if (potentialAttribute == null) {
            Tierify.LOGGER.info("Tier {} missing attribute definition for {}", tierId, BuiltInRegistries.ITEM.getKey(itemStack.getItem()));
            return modifiers;
        }

        int malformedTemplateCount = 0;
        int slotMismatchCount = 0;
        int missingAttributeTypeCount = 0;
        Tierify.LOGGER.info("Generating modifiers for {} using tier {} and attributes {}", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), tierId, potentialAttribute.getAttributes());
        for (AttributeTemplate template : potentialAttribute.getAttributes()) {
            if (template == null || template.getEntityAttributeModifier() == null) {
                malformedTemplateCount++;
                continue;
            }

            boolean applies = templateAppliesToSlot(template, slot);
            if (!applies) {
                slotMismatchCount++;
                continue;
            }

            if (!isValidAttributeTypeId(template.getAttributeTypeID())) {
                missingAttributeTypeCount++;
                continue;
            }

            template.realize(modifiers, slot);
        }

        Tierify.LOGGER.info("Generated modifiers for {} in {} -> {} (appliedCount={}, malformed={}, slotMismatch={}, missingAttributeType={})",
                BuiltInRegistries.ITEM.getKey(itemStack.getItem()),
                slot.getName(),
                modifiers,
                modifiers.size(),
                malformedTemplateCount,
                slotMismatchCount,
                missingAttributeTypeCount);
        return modifiers;
    }

    public static int rebuildAttributeModifiersComponent(ItemStack itemStack) {
        ItemAttributeModifiers baseComponent = new ItemStack(itemStack.getItem().builtInRegistryHolder(), itemStack.getCount())
                .getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY);

        ItemAttributeModifiers.Builder builder = ItemAttributeModifiers.builder();

        Equippable equippable = itemStack.get(DataComponents.EQUIPPABLE);
        EquipmentSlot armorSlot = null;
        if (equippable != null) {
            EquipmentSlot slot = equippable.slot();
            if (slot == EquipmentSlot.HEAD || slot == EquipmentSlot.CHEST || slot == EquipmentSlot.LEGS || slot == EquipmentSlot.FEET) {
                armorSlot = slot;
            }
        }

        for (ItemAttributeModifiers.Entry entry : baseComponent.modifiers()) {
            EquipmentSlotGroup slotGroup = entry.slot();
            // Armor items should show their concrete slot header instead of generic "worn".
            if (armorSlot != null && (slotGroup == EquipmentSlotGroup.ARMOR || slotGroup == EquipmentSlotGroup.BODY)) {
                slotGroup = EquipmentSlotGroup.bySlot(armorSlot);
            }
            builder.add(entry.attribute(), entry.modifier(), slotGroup);
        }

        int appliedModifierCount = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!Tierify.isPreferredEquipmentSlot(itemStack, slot)) {
                continue;
            }
            Multimap<Holder<Attribute>, AttributeModifier> generated = buildTierAttributeMap(itemStack, slot);
            for (java.util.Map.Entry<Holder<Attribute>, AttributeModifier> entry : generated.entries()) {
                builder.add(entry.getKey(), entry.getValue(), EquipmentSlotGroup.bySlot(slot));
                appliedModifierCount++;
            }
        }

        ItemAttributeModifiers rebuilt = builder.build();
        itemStack.set(DataComponents.ATTRIBUTE_MODIFIERS, rebuilt);
        Tierify.LOGGER.info("Rebuilt attribute modifiers for {} -> {} (appliedModifierCount={})", BuiltInRegistries.ITEM.getKey(itemStack.getItem()), rebuilt.modifiers(), appliedModifierCount);
        return appliedModifierCount;
    }

}