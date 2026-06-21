package draylar.tiered.api;

import net.minecraft.item.Item;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.registry.tag.TagKey;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.List;

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
            return itemID.equals(id);
        } else if (tag != null) {
            List<TagKey<Item>> candidates = resolveCandidateTags(tag);
            RegistryEntry<Item> itemEntry = Registries.ITEM.getEntry(Identifier.of(itemID)).orElse(null);
            if (itemEntry == null) {
                return false;
            }

            for (TagKey<Item> candidate : candidates) {
                if (itemEntry.isIn(candidate)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static List<TagKey<Item>> resolveCandidateTags(String rawTag) {
        List<TagKey<Item>> candidates = new ArrayList<>();
        candidates.add(TagKey.of(RegistryKeys.ITEM, Identifier.of(rawTag)));

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
            case "bows" -> {
                addCandidate(candidates, "c:tools/bows");
                addCandidate(candidates, "minecraft:bows");
            }
            case "crossbows" -> {
                addCandidate(candidates, "c:tools/crossbows");
                addCandidate(candidates, "minecraft:crossbows");
            }
            case "shields" -> {
                addCandidate(candidates, "c:tools/shields");
                addCandidate(candidates, "minecraft:shields");
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
        candidates.add(TagKey.of(RegistryKeys.ITEM, Identifier.of(rawTag)));
    }

    public String getId() {
        return id;
    }

    public TagKey<Item> getTagKey() {
        return TagKey.of(RegistryKeys.ITEM, Identifier.of(tag));
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
