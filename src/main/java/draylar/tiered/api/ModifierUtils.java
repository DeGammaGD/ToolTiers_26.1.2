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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.jetbrains.annotations.Nullable;

import elocindev.tierify.Tierify;
import elocindev.tierify.compat.ItemBordersCompat;

@SuppressWarnings({"null", "deprecation"})
public class ModifierUtils {

    private static final String GENERATED_ATTRIBUTES_KEY = "TieredGeneratedAttributes";
    private static final String GENERATED_COUNT_KEY = "count";
    private static final String ENTRY_PREFIX = "entry_";
    private static final String MOVEMENT_SPEED_ATTRIBUTE_ID = "minecraft:movement_speed";

    private static final class GeneratedAttributeRoll {
        private final String attributeTypeId;
        private final String modifierId;
        private final AttributeModifier.Operation operation;
        private final double amount;
        private final EquipmentSlot[] requiredSlots;
        private final EquipmentSlot[] optionalSlots;

        private GeneratedAttributeRoll(String attributeTypeId,
                                       String modifierId,
                                       AttributeModifier.Operation operation,
                                       double amount,
                                       EquipmentSlot[] requiredSlots,
                                       EquipmentSlot[] optionalSlots) {
            this.attributeTypeId = attributeTypeId;
            this.modifierId = modifierId;
            this.operation = operation;
            this.amount = amount;
            this.requiredSlots = requiredSlots;
            this.optionalSlots = optionalSlots;
        }
    }

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
        applyTierIfNeeded(stack);
    }

    public static void applyTierIfNeeded(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return;
        }

        Identifier existingTier = getAttributeID(stack);
        if (existingTier != null && Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().containsKey(existingTier)) {
            return;
        }

        Identifier generatedTier = getRandomAttributeIDFor(null, stack.getItem(), false);
        if (generatedTier == null) {
            return;
        }

        setItemStackAttribute(generatedTier, stack);
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

    private static String resolveTierQuality(Identifier tierId) {
        String path = tierId.getPath().toLowerCase(Locale.ROOT);
        if (path.contains("uncommon")) {
            return "uncommon";
        }
        if (path.contains("legendary")) {
            return "legendary";
        }
        if (path.contains("mythic")) {
            return "mythic";
        }
        if (path.contains("common")) {
            return "common";
        }
        if (path.contains("rare")) {
            return "rare";
        }
        if (path.contains("epic")) {
            return "epic";
        }
        return "common";
    }

    private static int[] getTierAttributeBounds(Identifier tierId) {
        return switch (resolveTierQuality(tierId)) {
            case "uncommon" -> new int[] {1, 2};
            case "rare" -> new int[] {2, 3};
            case "epic" -> new int[] {3, 3};
            case "legendary" -> new int[] {3, 4};
            case "mythic" -> new int[] {4, 4};
            default -> new int[] {1, 1};
        };
    }

    private static double getTierQualityCenter(Identifier tierId) {
        return switch (resolveTierQuality(tierId)) {
            case "uncommon" -> 0.45D;
            case "rare" -> 0.60D;
            case "epic" -> 0.72D;
            case "legendary" -> 0.84D;
            case "mythic" -> 0.92D;
            default -> 0.30D;
        };
    }

    private static double clamp01(double value) {
        if (value < 0.0D) {
            return 0.0D;
        }
        if (value > 1.0D) {
            return 1.0D;
        }
        return value;
    }

    private static double rollAmountForTier(double minAmount, double maxAmount, Identifier tierId) {
        if (Math.abs(maxAmount - minAmount) < 0.0000001D) {
            return minAmount;
        }

        double center = getTierQualityCenter(tierId);
        double base = ThreadLocalRandom.current().nextDouble();
        double jitter = (ThreadLocalRandom.current().nextDouble() - 0.5D) * 0.2D;
        double quality = clamp01((base * 0.65D) + (center * 0.35D) + jitter);
        return minAmount + (maxAmount - minAmount) * quality;
    }

    private static String getModifierBaseId(AttributeTemplate template, Identifier tierId) {
        if (template != null && template.getEntityAttributeModifier() != null && template.getEntityAttributeModifier().id() != null) {
            return template.getEntityAttributeModifier().id().toString();
        }
        return tierId + "_" + System.nanoTime();
    }

    private static EquipmentSlot[] copySlots(EquipmentSlot[] slots) {
        if (slots == null) {
            return null;
        }
        EquipmentSlot[] copy = new EquipmentSlot[slots.length];
        System.arraycopy(slots, 0, copy, 0, slots.length);
        return copy;
    }

    private static String resolveGroupedAttributeType(String attributeTypeId) {
        if (!CustomEntityAttributes.isProtectionFamilyAttributeId(attributeTypeId)) {
            return attributeTypeId;
        }

        String[] family = CustomEntityAttributes.PROTECTION_FAMILY_MEMBERS;
        if (family.length == 0) {
            return attributeTypeId;
        }

        return family[ThreadLocalRandom.current().nextInt(family.length)];
    }

    private static List<GeneratedAttributeRoll> normalizeGroupedRolls(List<GeneratedAttributeRoll> sourceRolls) {
        if (sourceRolls == null || sourceRolls.isEmpty()) {
            return sourceRolls;
        }

        List<GeneratedAttributeRoll> normalized = new ArrayList<>(sourceRolls.size());
        for (GeneratedAttributeRoll roll : sourceRolls) {
            if (roll == null) {
                continue;
            }

            String resolvedTypeId = resolveGroupedAttributeType(roll.attributeTypeId);
            normalized.add(new GeneratedAttributeRoll(
                    resolvedTypeId,
                    roll.modifierId,
                    roll.operation,
                    roll.amount,
                    copySlots(roll.requiredSlots),
                    copySlots(roll.optionalSlots)
            ));
        }

        return normalized;
    }

    private static boolean rollAppliesToSlot(GeneratedAttributeRoll roll, EquipmentSlot slot) {
        if (roll == null) {
            return false;
        }

        if (roll.requiredSlots != null) {
            for (EquipmentSlot requiredSlot : roll.requiredSlots) {
                if (requiredSlot == slot) {
                    return true;
                }
            }
        }

        if (roll.optionalSlots != null) {
            for (EquipmentSlot optionalSlot : roll.optionalSlots) {
                if (optionalSlot == slot) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<GeneratedAttributeRoll> generateTierAttributeRolls(ItemStack stack,
                                                                           Identifier tierId,
                                                                           PotentialAttribute assignedAttribute) {
        List<GeneratedAttributeRoll> generated = new ArrayList<>();
        if (assignedAttribute == null || assignedAttribute.getAttributes() == null || assignedAttribute.getAttributes().isEmpty()) {
            return generated;
        }

        Map<String, List<AttributeTemplate>> templatesByType = new LinkedHashMap<>();
        for (AttributeTemplate template : assignedAttribute.getAttributes()) {
            if (template == null || template.getEntityAttributeModifier() == null || !isValidAttributeTypeId(template.getAttributeTypeID())) {
                continue;
            }

            boolean usableForItem = false;
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (!Tierify.isPreferredEquipmentSlot(stack, slot)) {
                    continue;
                }

                if (templateAppliesToSlot(template, slot)) {
                    usableForItem = true;
                    break;
                }
            }

            if (!usableForItem) {
                continue;
            }

            templatesByType.computeIfAbsent(template.getAttributeTypeID(), k -> new ArrayList<>()).add(template);
        }

        if (templatesByType.isEmpty()) {
            return generated;
        }

        int[] bounds = getTierAttributeBounds(tierId);
        int minCount = Math.max(1, bounds[0]);
        int maxCount = Math.max(minCount, bounds[1]);

        int available = templatesByType.size();
        minCount = Math.min(minCount, available);
        maxCount = Math.min(maxCount, available);
        int targetCount = ThreadLocalRandom.current().nextInt(minCount, maxCount + 1);

        List<String> types = new ArrayList<>(templatesByType.keySet());
        Collections.shuffle(types);

        for (int i = 0; i < targetCount; i++) {
            String type = types.get(i);
            List<AttributeTemplate> group = templatesByType.get(type);
            if (group == null || group.isEmpty()) {
                continue;
            }

            AttributeTemplate representative = group.get(ThreadLocalRandom.current().nextInt(group.size()));
            double minAmount = representative.getEntityAttributeModifier().amount();
            double maxAmount = minAmount;
            for (AttributeTemplate candidate : group) {
                double amount = candidate.getEntityAttributeModifier().amount();
                if (amount < minAmount) {
                    minAmount = amount;
                }
                if (amount > maxAmount) {
                    maxAmount = amount;
                }
            }

            double rolledAmount = rollAmountForTier(minAmount, maxAmount, tierId);
                generated.add(new GeneratedAttributeRoll(
                    resolveGroupedAttributeType(type),
                    getModifierBaseId(representative, tierId),
                    representative.getEntityAttributeModifier().operation(),
                    rolledAmount,
                    copySlots(representative.getRequiredEquipmentSlots()),
                    copySlots(representative.getOptionalEquipmentSlots())
            ));
        }

        return generated;
    }

    private static void writeGeneratedRollsToNbt(CompoundTag root, List<GeneratedAttributeRoll> generatedRolls) {
        root.remove(GENERATED_ATTRIBUTES_KEY);
        if (generatedRolls == null || generatedRolls.isEmpty()) {
            return;
        }

        CompoundTag generatedTag = new CompoundTag();
        generatedTag.putInt(GENERATED_COUNT_KEY, generatedRolls.size());

        for (int i = 0; i < generatedRolls.size(); i++) {
            GeneratedAttributeRoll roll = generatedRolls.get(i);
            CompoundTag entry = new CompoundTag();
            entry.putString("type", roll.attributeTypeId);
            entry.putString("modifier_id", roll.modifierId);
            entry.putString("operation", roll.operation.name());
            entry.putDouble("amount", roll.amount);

            if (roll.requiredSlots != null && roll.requiredSlots.length > 0) {
                StringBuilder requiredBuilder = new StringBuilder();
                for (int s = 0; s < roll.requiredSlots.length; s++) {
                    if (s > 0) {
                        requiredBuilder.append(',');
                    }
                    requiredBuilder.append(roll.requiredSlots[s].name());
                }
                entry.putString("required_slots", requiredBuilder.toString());
            }

            if (roll.optionalSlots != null && roll.optionalSlots.length > 0) {
                StringBuilder optionalBuilder = new StringBuilder();
                for (int s = 0; s < roll.optionalSlots.length; s++) {
                    if (s > 0) {
                        optionalBuilder.append(',');
                    }
                    optionalBuilder.append(roll.optionalSlots[s].name());
                }
                entry.putString("optional_slots", optionalBuilder.toString());
            }

            generatedTag.put(ENTRY_PREFIX + i, entry);
        }

        root.put(GENERATED_ATTRIBUTES_KEY, generatedTag);
    }

    private static EquipmentSlot[] parseSlotsCsv(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }

        String[] raw = value.split(",");
        List<EquipmentSlot> parsed = new ArrayList<>();
        for (String token : raw) {
            if (token == null || token.isBlank()) {
                continue;
            }
            try {
                parsed.add(EquipmentSlot.valueOf(token.trim()));
            } catch (IllegalArgumentException ignored) {
                // ignore invalid slot token
            }
        }

        if (parsed.isEmpty()) {
            return null;
        }

        return parsed.toArray(new EquipmentSlot[0]);
    }

    private static List<GeneratedAttributeRoll> readGeneratedRollsFromNbt(CompoundTag root) {
        List<GeneratedAttributeRoll> generated = new ArrayList<>();
        if (!root.contains(GENERATED_ATTRIBUTES_KEY)) {
            return generated;
        }

        CompoundTag generatedTag = root.getCompound(GENERATED_ATTRIBUTES_KEY).orElse(new CompoundTag());
        int count = generatedTag.getInt(GENERATED_COUNT_KEY).orElse(0);
        for (int i = 0; i < count; i++) {
            CompoundTag entry = generatedTag.getCompound(ENTRY_PREFIX + i).orElse(null);
            if (entry == null) {
                continue;
            }

            String type = entry.getString("type").orElse("");
            String modifierId = entry.getString("modifier_id").orElse("");
            String operationRaw = entry.getString("operation").orElse("");
            double amount = entry.getDouble("amount").orElse(0.0D);

            if (type.isBlank() || modifierId.isBlank() || operationRaw.isBlank()) {
                continue;
            }

            AttributeModifier.Operation operation;
            try {
                operation = AttributeModifier.Operation.valueOf(operationRaw);
            } catch (IllegalArgumentException ignored) {
                continue;
            }

            EquipmentSlot[] required = parseSlotsCsv(entry.getString("required_slots").orElse(""));
            EquipmentSlot[] optional = parseSlotsCsv(entry.getString("optional_slots").orElse(""));
            generated.add(new GeneratedAttributeRoll(type, modifierId, operation, amount, required, optional));
        }

        return generated;
    }

    private static boolean isMasteryAttributeType(String attributeTypeId) {
        return attributeTypeId != null && attributeTypeId.toLowerCase(Locale.ROOT).contains("mastery");
    }

    private static double getMasteryMultiplierForSlot(List<GeneratedAttributeRoll> generatedRolls, EquipmentSlot slot) {
        if (generatedRolls == null || generatedRolls.isEmpty()) {
            return 0.0D;
        }

        double mastery = 0.0D;
        for (GeneratedAttributeRoll roll : generatedRolls) {
            if (roll == null || !isMasteryAttributeType(roll.attributeTypeId)) {
                continue;
            }

            if (!rollAppliesToSlot(roll, slot)) {
                continue;
            }

            mastery += roll.amount;
        }

        return mastery;
    }

    private static double applyMovementSpeedDiminishingReturns(String attributeTypeId,
                                                               double amount,
                                                               Map<String, Integer> diminishingCounters) {
        if (!MOVEMENT_SPEED_ATTRIBUTE_ID.equals(attributeTypeId)) {
            return amount;
        }

        int stackIndex = diminishingCounters.getOrDefault(MOVEMENT_SPEED_ATTRIBUTE_ID, 0) + 1;
        diminishingCounters.put(MOVEMENT_SPEED_ATTRIBUTE_ID, stackIndex);
        double multiplier = Math.pow(0.5D, stackIndex - 1);
        return amount * multiplier;
    }

    private static double resolveDurableAmount(List<GeneratedAttributeRoll> generatedRolls, PotentialAttribute assignedAttribute) {
        if (generatedRolls != null) {
            for (GeneratedAttributeRoll roll : generatedRolls) {
                if (roll != null && CustomEntityAttributes.isDurabilityAttributeId(roll.attributeTypeId)) {
                    return (double) Math.round(roll.amount * 100.0D) / 100.0D;
                }
            }
        }

        if (assignedAttribute != null && assignedAttribute.getAttributes() != null) {
            for (AttributeTemplate template : assignedAttribute.getAttributes()) {
                if (template != null && CustomEntityAttributes.isDurabilityAttributeId(template.getAttributeTypeID()) && template.getEntityAttributeModifier() != null) {
                    return (double) Math.round(template.getEntityAttributeModifier().amount() * 100.0D) / 100.0D;
                }
            }
        }

        return 0.0D;
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
            boolean weightAllowed = attribute.getWeight() > 0;
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
                attributeWeights.add(attribute.getWeight());
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

        // Luck
        if (playerEntity != null) {
            int luckMaxWeight = Collections.max(attributeWeights);
            for (int i = 0; i < attributeWeights.size(); i++) {
                if (attributeWeights.get(i) > luckMaxWeight / 3) {
                    attributeWeights.set(i, (int) (attributeWeights.get(i) * (1.0f - 0.02f * playerEntity.getLuck())));
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

    public static void setItemStackAttribute(Identifier potentialAttributeID, ItemStack stack) {
        if (potentialAttributeID != null) {
            Tierify.LOGGER.info("Assigning tier {} to {}", potentialAttributeID, BuiltInRegistries.ITEM.getKey(stack.getItem()));
            PotentialAttribute assignedAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(Identifier.parse(potentialAttributeID.toString()));
            if (assignedAttribute == null) {
                Tierify.LOGGER.warn("Tier {} has no attribute template while assigning to {}", potentialAttributeID, BuiltInRegistries.ITEM.getKey(stack.getItem()));
                return;
            }

            setTier(stack, potentialAttributeID);
            int modifierPoolSize = countUsableModifierTemplates(stack.getItem(), assignedAttribute);
            int appliedCount = applyTierAttributes(stack);
            Tierify.LOGGER.info("[TierifyDebug][ModifierAssign] item={} tierSelected={} modifierPoolFound={} modifiersAppliedCount={}", BuiltInRegistries.ITEM.getKey(stack.getItem()), potentialAttributeID, modifierPoolSize, appliedCount);
            if (appliedCount == 0) {
                Tierify.LOGGER.warn("No modifiers applied for item={} tier={} despite assignment; reason=pool_empty_or_slot_mismatch_or_invalid_attribute_type", BuiltInRegistries.ITEM.getKey(stack.getItem()), potentialAttributeID);
            }
            Tierify.LOGGER.info("[TierifyDebug][Create] item={} tier={} attributes={}", BuiltInRegistries.ITEM.getKey(stack.getItem()), potentialAttributeID, assignedAttribute.getAttributes());
        } else {
            Tierify.LOGGER.warn("Tier assignment skipped for {} because no valid tier was selected", BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
    }

    private static void clearTierNbtKeys(CompoundTag root, @Nullable Identifier tierId, boolean clearGeneratedRolls) {
        if (clearGeneratedRolls) {
            root.remove(GENERATED_ATTRIBUTES_KEY);
        }
        if (tierId == null) {
            return;
        }

        PotentialAttribute previous = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
        if (previous == null) {
            return;
        }

        HashMap<String, Object> nbtMap = previous.getNbtValues();
        if (nbtMap != null) {
            for (String key : nbtMap.keySet()) {
                if (!"Damage".equals(key)) {
                    root.remove(key);
                }
            }
        }

        for (AttributeTemplate template : previous.getAttributes()) {
            if (template == null) {
                continue;
            }
            if (CustomEntityAttributes.isDurabilityAttributeId(template.getAttributeTypeID())) {
                root.remove("durable");
                break;
            }
        }
    }

    private static void applyTierNbtValues(CompoundTag root,
                                           PotentialAttribute assignedAttribute,
                                           Identifier tierId,
                                           List<GeneratedAttributeRoll> generatedRolls) {
        HashMap<String, Object> nbtMap = assignedAttribute.getNbtValues();
        Tierify.LOGGER.info("Attribute json for {} -> {}", tierId, nbtMap);

        double durableAmount = resolveDurableAmount(generatedRolls, assignedAttribute);
        if (Math.abs(durableAmount) > 0.0000001D) {
            if (nbtMap == null) {
                nbtMap = new HashMap<>();
            }
            nbtMap.put("durable", durableAmount);
        }

        if (nbtMap == null) {
            return;
        }

        for (HashMap.Entry<String, Object> entry : nbtMap.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();

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

    public static void setTier(ItemStack stack, Identifier tierId) {
        if (stack == null || stack.isEmpty() || tierId == null) {
            return;
        }

        CompoundTag root = getCustomData(stack);
        Identifier previousTier = getAttributeID(stack);
        clearTierNbtKeys(root, previousTier, true);

        CompoundTag tiered = new CompoundTag();
        tiered.putString(Tierify.NBT_SUBTAG_DATA_KEY, tierId.toString());
        root.put(Tierify.NBT_SUBTAG_KEY, tiered);
        root.putBoolean(Tierify.NBT_SUBTAG_MARKER_KEY, true);

        CompoundTag colors = new CompoundTag();
        colors.putString("top", ItemBordersCompat.getColorForIdentifier(tierId));
        colors.putString("bottom", ItemBordersCompat.getColorForIdentifier(tierId));
        root.put("itemborders_colors", colors);

        setCustomData(stack, root);
    }

    public static int applyTierAttributes(ItemStack stack) {
        if (stack == null || stack.isEmpty()) {
            return 0;
        }

        Identifier tierId = getAttributeID(stack);
        if (tierId == null) {
            return rebuildAttributeModifiersComponent(stack);
        }

        PotentialAttribute assignedAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tierId);
        if (assignedAttribute == null) {
            Tierify.LOGGER.warn("Tier {} has no attribute template while applying attributes to {}", tierId, BuiltInRegistries.ITEM.getKey(stack.getItem()));
            return rebuildAttributeModifiersComponent(stack);
        }

        CompoundTag root = getCustomData(stack);
        clearTierNbtKeys(root, tierId, false);

        List<GeneratedAttributeRoll> generatedRolls = readGeneratedRollsFromNbt(root);
        if (generatedRolls.isEmpty()) {
            generatedRolls = generateTierAttributeRolls(stack, tierId, assignedAttribute);
        } else {
            generatedRolls = normalizeGroupedRolls(generatedRolls);
        }

        writeGeneratedRollsToNbt(root, generatedRolls);
        applyTierNbtValues(root, assignedAttribute, tierId, generatedRolls);
        setCustomData(stack, root);
        int appliedCount = rebuildAttributeModifiersComponent(stack);
        if (appliedCount == 0) {
            Tierify.LOGGER.warn("Tier {} generated zero modifiers for {}; item will remain tiered but has no generated tier attributes", tierId, BuiltInRegistries.ITEM.getKey(stack.getItem()));
        }
        return appliedCount;
    }


    public static void setItemStackAttribute(@Nullable Player playerEntity, ItemStack stack, boolean reforge) {
        CompoundTag customData = getCustomData(stack);
        boolean alreadyTiered = customData.contains(Tierify.NBT_SUBTAG_KEY);
        boolean hasMarker = customData.contains(Tierify.NBT_SUBTAG_MARKER_KEY) && customData.getBoolean(Tierify.NBT_SUBTAG_MARKER_KEY).orElse(false);

        if (alreadyTiered) {
            Identifier existingTier = getAttributeID(stack);
            int generatedModifierCount = countGeneratedTierModifiers(stack);
            if (existingTier != null && generatedModifierCount > 0) {
                Tierify.LOGGER.info("Skipping tier generation for {} (alreadyTiered=true, marker={}, reforge={}, generatedModifierCount={})", BuiltInRegistries.ITEM.getKey(stack.getItem()), hasMarker, reforge, generatedModifierCount);
                return;
            }

            Tierify.LOGGER.warn("Detected broken tiered item for {} (tier={}, generatedModifierCount={}); attempting repair before regeneration", BuiltInRegistries.ITEM.getKey(stack.getItem()), existingTier, generatedModifierCount);
            int repairedCount = applyTierAttributes(stack);
            if (repairedCount > 0) {
                Tierify.LOGGER.info("Repaired tier attributes for {} (tier={}, generatedModifierCount={})", BuiltInRegistries.ITEM.getKey(stack.getItem()), existingTier, repairedCount);
                return;
            }

            Tierify.LOGGER.warn("Repair failed for {} (tier={}); clearing tier data and regenerating", BuiltInRegistries.ITEM.getKey(stack.getItem()), existingTier);
            removeItemStackAttribute(stack);
        } else if (hasMarker) {
            Tierify.LOGGER.warn("Found tier marker without tier data on {}; clearing marker and regenerating", BuiltInRegistries.ITEM.getKey(stack.getItem()));
            customData.remove(Tierify.NBT_SUBTAG_MARKER_KEY);
            setCustomData(stack, customData);
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
                    if (CustomEntityAttributes.isDurabilityAttributeId(attributeTypeId)) {
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
            root.remove(GENERATED_ATTRIBUTES_KEY);
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

        CompoundTag root = getCustomData(itemStack);
        List<GeneratedAttributeRoll> generatedRolls = readGeneratedRollsFromNbt(root);
        Map<String, Integer> diminishingCounters = new HashMap<>();
        if (!generatedRolls.isEmpty()) {
            int slotMismatchCount = 0;
            int missingAttributeTypeCount = 0;
            int malformedTemplateCount = 0;
            double masteryMultiplier = getMasteryMultiplierForSlot(generatedRolls, slot);

            for (GeneratedAttributeRoll roll : generatedRolls) {
                if (roll == null) {
                    malformedTemplateCount++;
                    continue;
                }

                if (!rollAppliesToSlot(roll, slot)) {
                    slotMismatchCount++;
                    continue;
                }

                if (!isValidAttributeTypeId(roll.attributeTypeId)) {
                    missingAttributeTypeCount++;
                    continue;
                }

                Identifier baseModifierId;
                try {
                    baseModifierId = Identifier.parse(roll.modifierId);
                } catch (Exception ignored) {
                    malformedTemplateCount++;
                    continue;
                }

                Identifier modifierId = Identifier.fromNamespaceAndPath(baseModifierId.getNamespace(), baseModifierId.getPath() + "_" + slot.getName());
                double amount = roll.amount;
                if (masteryMultiplier != 0.0D && !isMasteryAttributeType(roll.attributeTypeId)) {
                    amount = amount + masteryMultiplier;
                }
                amount = applyMovementSpeedDiminishingReturns(roll.attributeTypeId, amount, diminishingCounters);
                AttributeModifier cloneModifier = new AttributeModifier(modifierId, amount, roll.operation);
                var key = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(roll.attributeTypeId));
                if (key.isEmpty()) {
                    missingAttributeTypeCount++;
                    continue;
                }

                modifiers.put(key.get(), cloneModifier);
            }

            Tierify.LOGGER.info("Generated modifiers (rolled) for {} in {} -> {} (appliedCount={}, malformed={}, slotMismatch={}, missingAttributeType={})",
                    BuiltInRegistries.ITEM.getKey(itemStack.getItem()),
                    slot.getName(),
                    modifiers,
                    modifiers.size(),
                    malformedTemplateCount,
                    slotMismatchCount,
                    missingAttributeTypeCount);
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

            AttributeModifier baseModifier = template.getEntityAttributeModifier();
            Identifier baseModifierId = baseModifier.id();
            Identifier modifierId = Identifier.fromNamespaceAndPath(baseModifierId.getNamespace(), baseModifierId.getPath() + "_" + slot.getName());
            double amount = applyMovementSpeedDiminishingReturns(template.getAttributeTypeID(), baseModifier.amount(), diminishingCounters);
            AttributeModifier cloneModifier = new AttributeModifier(modifierId, amount, baseModifier.operation());

            var key = BuiltInRegistries.ATTRIBUTE.get(Identifier.parse(template.getAttributeTypeID()));
            if (key.isPresent()) {
                modifiers.put(key.get(), cloneModifier);
            } else {
                missingAttributeTypeCount++;
            }
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

    private static int countGeneratedTierModifiers(ItemStack itemStack) {
        if (itemStack == null || itemStack.isEmpty()) {
            return 0;
        }

        int generatedModifierCount = 0;
        for (EquipmentSlot slot : EquipmentSlot.values()) {
            if (!Tierify.isPreferredEquipmentSlot(itemStack, slot)) {
                continue;
            }
            generatedModifierCount += buildTierAttributeMap(itemStack, slot).size();
        }
        return generatedModifierCount;
    }

}