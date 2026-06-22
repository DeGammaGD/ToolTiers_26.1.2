package elocindev.tierify.mixin;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.DataSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Mixin(AnvilMenu.class)
public abstract class AnvilScreenHandlerMixin {

    @Shadow
    private DataSlot cost;

    @Inject(method = "createResult", at = @At("TAIL"))
    private void tierify$allowTierUpgradeValidationPreview(CallbackInfo info) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();
        ItemStack vanillaResult = menu.getSlot(2).getItem();
        if (!isTierUpgradeOperationValid(left, right)) {
            return;
        }

        Identifier leftTier = ModifierUtils.getAttributeID(left);
        Identifier rightTier = ModifierUtils.getAttributeID(right);
        if (leftTier == null || rightTier == null) {
            return;
        }

        Identifier targetTier = pickResultTier(left.getItem(), leftTier, rightTier);
        if (targetTier == null) {
            return;
        }

        ItemStack preview = vanillaResult.isEmpty() ? left.copy() : vanillaResult.copy();
        ModifierUtils.removeItemStackAttribute(preview);
        ModifierUtils.setTier(preview, targetTier);
        menu.getSlot(2).set(preview);

        if (vanillaResult.isEmpty()) {
            this.cost.set(1);
        }
        menu.broadcastChanges();
    }


    @Inject(method = "onTake", at = @At("HEAD"))
    private void tierify$applyAnvilTierUpgradeOnTake(Player player, ItemStack resultStack, CallbackInfo info) {
        AnvilMenu menu = (AnvilMenu) (Object) this;
        ItemStack left = menu.getSlot(0).getItem();
        ItemStack right = menu.getSlot(1).getItem();

        if (left.isEmpty() || right.isEmpty() || resultStack.isEmpty()) {
            return;
        }
        if (left.getItem() != right.getItem() || resultStack.getItem() != left.getItem()) {
            return;
        }

        Identifier leftTier = ModifierUtils.getAttributeID(left);
        Identifier rightTier = ModifierUtils.getAttributeID(right);
        if (leftTier == null || rightTier == null) {
            return;
        }
        if (!isTierUpgradeOperationValid(left, right)) {
            return;
        }

        Identifier targetTier = pickResultTier(left.getItem(), leftTier, rightTier);
        if (targetTier == null) {
            return;
        }

        ModifierUtils.setTier(resultStack, targetTier);
        ModifierUtils.applyTierAttributes(resultStack);
    }

    @Unique
    private static Identifier pickResultTier(Item item, Identifier leftTier, Identifier rightTier) {
        TierRank leftRank = rankForTierId(leftTier);
        TierRank rightRank = rankForTierId(rightTier);
        if (leftRank == null || rightRank == null) {
            return null;
        }

        Map<TierRank, List<Identifier>> tiersByRank = collectTiersByRank(item);

        if (tiersByRank.isEmpty()) {
            return null;
        }

        TierRank targetRank;
        if (leftRank == rightRank) {
            targetRank = nextRank(leftRank);
        } else {
            targetRank = higherRank(leftRank, rightRank);
        }

        List<Identifier> candidates = tiersByRank.get(targetRank);
        if (candidates == null || candidates.isEmpty()) {
            return null;
        }

        if (leftRank != rightRank) {
            return leftRank.ordinal() >= rightRank.ordinal() ? leftTier : rightTier;
        }

        if (targetRank == leftRank) {
            return leftTier;
        }

        return chooseDeterministicTier(candidates, leftTier);
    }

    @Unique
    private static boolean isTierUpgradeOperationValid(ItemStack left, ItemStack right) {
        if (left.isEmpty() || right.isEmpty()) {
            return false;
        }
        if (left.getItem() != right.getItem()) {
            return false;
        }

        Identifier leftTier = ModifierUtils.getAttributeID(left);
        Identifier rightTier = ModifierUtils.getAttributeID(right);
        if (leftTier == null || rightTier == null) {
            return false;
        }

        return hasUpgradeableTarget(left.getItem(), leftTier, rightTier);
    }

    @Unique
    private static boolean hasUpgradeableTarget(Item item, Identifier leftTier, Identifier rightTier) {
        TierRank leftRank = rankForTierId(leftTier);
        TierRank rightRank = rankForTierId(rightTier);
        if (leftRank == null || rightRank == null) {
            return false;
        }

        Map<TierRank, List<Identifier>> tiersByRank = collectTiersByRank(item);
        if (tiersByRank.isEmpty()) {
            return false;
        }

        if (leftRank == rightRank) {
            TierRank targetRank = nextRank(leftRank);
            List<Identifier> candidates = tiersByRank.get(targetRank);
            return candidates != null && !candidates.isEmpty();
        }

        TierRank targetRank = higherRank(leftRank, rightRank);
        List<Identifier> candidates = tiersByRank.get(targetRank);
        return candidates != null && !candidates.isEmpty();
    }

    @Unique
    private static Map<TierRank, List<Identifier>> collectTiersByRank(Item item) {
        Map<TierRank, List<Identifier>> tiersByRank = new EnumMap<>(TierRank.class);
        Identifier itemId = BuiltInRegistries.ITEM.getKey(item);

        Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().forEach((id, attribute) -> {
            if (!attribute.isValid(itemId)) {
                return;
            }
            TierRank rank = rankForTierId(id);
            if (rank != null) {
                tiersByRank.computeIfAbsent(rank, ignored -> new ArrayList<>()).add(id);
            }
        });

        return tiersByRank;
    }

    @Unique
    private static Identifier chooseDeterministicTier(List<Identifier> candidates, Identifier preferredSourceTier) {
        Identifier fallback = null;
        Identifier preferred = null;
        String preferredKey = tierInvariantKey(preferredSourceTier);

        for (Identifier candidate : candidates) {
            if (fallback == null || compareIdentifier(candidate, fallback) < 0) {
                fallback = candidate;
            }

            if (preferredKey.equals(tierInvariantKey(candidate))) {
                if (preferred == null || compareIdentifier(candidate, preferred) < 0) {
                    preferred = candidate;
                }
            }
        }

        return preferred != null ? preferred : fallback;
    }

    @Unique
    private static TierRank higherRank(TierRank first, TierRank second) {
        return first.ordinal() >= second.ordinal() ? first : second;
    }

    @Unique
    private static int compareIdentifier(Identifier first, Identifier second) {
        int namespaceCompare = first.getNamespace().compareTo(second.getNamespace());
        if (namespaceCompare != 0) {
            return namespaceCompare;
        }
        return first.getPath().compareTo(second.getPath());
    }

    @Unique
    private static String tierInvariantKey(Identifier id) {
        return id.getNamespace() + ":" + id.getPath().toLowerCase()
                .replace("uncommon", "")
                .replace("legendary", "")
                .replace("common", "")
                .replace("mythic", "")
                .replace("epic", "")
                .replace("rare", "");
    }

    @Unique
    private static TierRank rankForTierId(Identifier id) {
        String path = id.getPath().toLowerCase();
        if (path.contains("uncommon")) {
            return TierRank.UNCOMMON;
        }
        if (path.contains("legendary")) {
            return TierRank.LEGENDARY;
        }
        if (path.contains("common")) {
            return TierRank.COMMON;
        }
        if (path.contains("mythic")) {
            return TierRank.MYTHIC;
        }
        if (path.contains("epic")) {
            return TierRank.EPIC;
        }
        if (path.contains("rare")) {
            return TierRank.RARE;
        }
        return null;
    }

    @Unique
    private static TierRank nextRank(TierRank rank) {
        if (rank == TierRank.MYTHIC) {
            return TierRank.MYTHIC;
        }
        return TierRank.values()[rank.ordinal() + 1];
    }

    @Unique
    private enum TierRank {
        COMMON,
        UNCOMMON,
        RARE,
        EPIC,
        LEGENDARY,
        MYTHIC
    }

}
