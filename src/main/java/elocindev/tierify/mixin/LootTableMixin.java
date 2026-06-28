package elocindev.tierify.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemInstance;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

@Mixin(LootTable.class)
public class LootTableMixin {

    @ModifyVariable(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V", at = @At("HEAD"), argsOnly = true)
    private Consumer<ItemStack> wrapLootOutputConsumer(Consumer<ItemStack> originalConsumer, LootContext context) {
        boolean applyTier = Tierify.CONFIG.lootContainerModifier;
        double dropMultiplier = tierify$dropMultiplier(context);

        if (!applyTier && dropMultiplier <= 0.0D) {
            return originalConsumer;
        }

        return stack -> {
            if (!stack.isEmpty() && !context.getLevel().isClientSide() && applyTier) {
                ModifierUtils.applyTierIfNeeded(stack);
                ModifierUtils.logTierDebug("loot_generation", stack);
            }

            if (dropMultiplier > 0.0D && !stack.isEmpty() && stack.isStackable()) {
                tierify$emitMultiplied(stack, dropMultiplier, context.getRandom(), originalConsumer);
            } else {
                originalConsumer.accept(stack);
            }
        };
    }

    /**
     * Resolves the ToolTiers drop multiplier for the current loot context: {@code fortune} when a block is being
     * broken, {@code looting} when a (non-player) entity is killed. Returns {@code 0} when there is no tool or the
     * context does not match either case. Vanilla Looting/Fortune enchantments are untouched and stack on top.
     */
    private static double tierify$dropMultiplier(LootContext context) {
        ItemInstance tool = context.getOptionalParameter(LootContextParams.TOOL);
        if (tool == null) {
            return 0.0D;
        }

        if (context.hasParameter(LootContextParams.BLOCK_STATE)) {
            return AttributeHelper.getItemAttributeAmount(tool, CustomEntityAttributes.FORTUNE);
        }

        Entity thisEntity = context.getOptionalParameter(LootContextParams.THIS_ENTITY);
        if (thisEntity instanceof LivingEntity && !(thisEntity instanceof Player)) {
            return AttributeHelper.getItemAttributeAmount(tool, CustomEntityAttributes.LOOTING);
        }

        return 0.0D;
    }

    /**
     * Emits a drop scaled by {@code base * (1 + multiplier)}. The fractional remainder is resolved probabilistically
     * so the expected drop count matches the multiplier exactly (e.g. +50% on a single item yields a 50% chance of a
     * second one). Output is split into stacks of at most 64.
     */
    private static void tierify$emitMultiplied(ItemStack stack, double multiplier, RandomSource random, Consumer<ItemStack> consumer) {
        int base = stack.getCount();
        double exact = base * (1.0D + multiplier);
        int total = (int) Math.floor(exact);
        double frac = exact - total;
        if (frac > 0.0D && random.nextDouble() < frac) {
            total++;
        }

        if (total <= base) {
            consumer.accept(stack);
            return;
        }

        int remaining = total;
        while (remaining > 0) {
            int n = Math.min(remaining, 64);
            ItemStack copy = stack.copy();
            copy.setCount(n);
            consumer.accept(copy);
            remaining -= n;
        }
    }
}
