package draylar.tiered.api;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import elocindev.tierify.Tierify;

public class ItemVerifier {

    private final String id;
    private final String tag;

    public ItemVerifier(String id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    /**
     * Returns whether the given {@link Identifier} is valid for this ItemVerifier, which may check direct against either a {@link Identifier} or {@link Tag<Item>}.
     * <p>
     * The given {@link Identifier} should be the ID of an {@link Item} in {@link Registry#ITEM}.
     *
     * @param itemID item registry ID to check against this verifier
     * @return whether the check succeeded
     */
    public boolean isValid(Identifier itemID) {
        return isValid(itemID.toString());
    }

    /**
     * Returns whether the given {@link String} is valid for this ItemVerifier, which may check direct against either a {@link Identifier} or {@link Tag<Item>}.
     * <p>
     * The given {@link String} should be the ID of an {@link Item} in {@link Registry#ITEM}.
     *
     * @param itemID item registry ID to check against this verifier
     * @return whether the check succeeded
     */
    public boolean isValid(String itemID) {
        if (id != null) {
            boolean result = itemID.equals(id);
            logVerifierResult(itemID, "id=" + id, result);
            return result;
        } else if (tag != null) {
            List<TagKey<Item>> candidates = resolveCandidateTags(tag);
            Holder<Item> itemEntry = BuiltInRegistries.ITEM.get(Identifier.parse(itemID)).orElse(null);
            if (itemEntry == null) {
                logVerifierResult(itemID, "tag=" + tag, false);
                return false;
            }

            for (TagKey<Item> candidate : candidates) {
                if (itemEntry.is(candidate)) {
                    logVerifierResult(itemID, "tag=" + tag, true);
                    return true;
                }
            }

            boolean fallbackResult = matchesKnownItemFallback(tag, itemID);
            logVerifierResult(itemID, "tag=" + tag + " (fallback)", fallbackResult);
            return fallbackResult;
        }

        return false;
    }

    private static void logVerifierResult(String itemID, String verifier, boolean result) {
        if (!isSpearRelatedItem(itemID) && !isSpearRelatedVerifier(verifier)) {
            return;
        }

        Tierify.LOGGER.info("[TierifyDebug][Verifier] item={} verifier={} result={}", itemID, verifier, result);
    }

    private static boolean isSpearRelatedItem(String itemID) {
        return "minecraft:trident".equals(itemID) || itemID.endsWith(":spear") || itemID.contains("_spear");
    }

    private static boolean isSpearRelatedVerifier(String verifier) {
        return verifier.contains("spears") || verifier.contains("trident") || verifier.contains("spear");
    }

    private static boolean matchesKnownItemFallback(String rawTag, String itemID) {
        return switch (rawTag) {
            case "c:bows", "c:tools/bows", "minecraft:bows" -> "minecraft:bow".equals(itemID);
            case "c:crossbows", "c:tools/crossbows", "minecraft:crossbows" -> "minecraft:crossbow".equals(itemID);
            case "c:shields", "c:tools/shields", "minecraft:shields" -> "minecraft:shield".equals(itemID);
            case "c:shears", "c:tools/shears", "minecraft:shears" -> "minecraft:shears".equals(itemID);
            case "c:flint_and_steel", "c:tools/flint_and_steel", "minecraft:flint_and_steel" -> "minecraft:flint_and_steel".equals(itemID);
            case "c:maces", "c:tools/maces", "minecraft:maces" -> "minecraft:mace".equals(itemID);
            case "c:spears", "c:tools/spears", "minecraft:spears" -> "minecraft:trident".equals(itemID) || itemID.endsWith(":spear") || itemID.contains("_spear");
            default -> false;
        };
    }

    private static List<TagKey<Item>> resolveCandidateTags(String rawTag) {
        List<TagKey<Item>> candidates = new ArrayList<>();
        candidates.add(TagKey.create(Registries.ITEM, Identifier.parse(rawTag)));

        if (!rawTag.startsWith("c:")) {
            return candidates;
        }

        String path = rawTag.substring(2);
        switch (path) {
            case "axes" -> {
                addCandidate(candidates, "c:tools/axes");
                addCandidate(candidates, "minecraft:axes");
            }
            case "pickaxes" -> {
                addCandidate(candidates, "c:tools/pickaxes");
                addCandidate(candidates, "minecraft:pickaxes");
            }
            case "shovels" -> {
                addCandidate(candidates, "c:tools/shovels");
                addCandidate(candidates, "minecraft:shovels");
            }
            case "hoes" -> {
                addCandidate(candidates, "c:tools/hoes");
                addCandidate(candidates, "minecraft:hoes");
            }
            case "swords" -> {
                addCandidate(candidates, "c:tools/swords");
                addCandidate(candidates, "minecraft:swords");
            }
            case "bows", "tools/bows" -> {
                addCandidate(candidates, "c:tools/bows");
                addCandidate(candidates, "minecraft:bows");
            }
            case "crossbows", "tools/crossbows" -> {
                addCandidate(candidates, "c:tools/crossbows");
                addCandidate(candidates, "minecraft:crossbows");
            }
            case "shields", "tools/shields" -> {
                addCandidate(candidates, "c:tools/shields");
                addCandidate(candidates, "minecraft:shields");
            }
            case "shears", "tools/shears" -> {
                addCandidate(candidates, "c:tools/shears");
                addCandidate(candidates, "minecraft:shears");
            }
            case "flint_and_steel", "tools/flint_and_steel" -> {
                addCandidate(candidates, "c:tools/flint_and_steel");
                addCandidate(candidates, "minecraft:flint_and_steel");
            }
            case "spears", "tools/spears" -> {
                addCandidate(candidates, "c:tools/spears");
                addCandidate(candidates, "minecraft:spears");
            }
            case "maces", "tools/maces" -> {
                addCandidate(candidates, "c:tools/maces");
                addCandidate(candidates, "minecraft:maces");
            }
            case "helmets" -> {
                addCandidate(candidates, "c:armors/helmets");
                addCandidate(candidates, "minecraft:head_armor");
            }
            case "chestplates" -> {
                addCandidate(candidates, "c:armors/chestplates");
                addCandidate(candidates, "minecraft:chest_armor");
            }
            case "leggings" -> {
                addCandidate(candidates, "c:armors/leggings");
                addCandidate(candidates, "minecraft:leg_armor");
            }
            case "boots" -> {
                addCandidate(candidates, "c:armors/boots");
                addCandidate(candidates, "minecraft:foot_armor");
            }
            default -> {
            }
        }

        return candidates;
    }

    private static void addCandidate(List<TagKey<Item>> candidates, String rawTag) {
        candidates.add(TagKey.create(Registries.ITEM, Identifier.parse(rawTag)));
    }

    public String getId() {
        return id;
    }

    public TagKey<Item> getTagKey() {
        return TagKey.create(Registries.ITEM, Identifier.parse(tag));
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode() * 17 + (tag == null ? 0 : tag.hashCode());
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ItemVerifier other)) {
            return false;
        }
        if (this != other) {
            return false;
        }
        String thisId = this.id == null ? "" : this.id;
        String thisTag = this.tag == null ? "" : this.tag;
        String otherId = other.id == null ? "" : other.id;
        String otherTag = other.tag == null ? "" : other.tag;
        return thisId.equals(otherId) && thisTag.equals(otherTag);
    }
}
