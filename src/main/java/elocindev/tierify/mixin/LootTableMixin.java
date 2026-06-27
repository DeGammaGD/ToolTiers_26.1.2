package elocindev.tierify.mixin;

import java.util.function.Consumer;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.LootTable;

@Mixin(LootTable.class)
public class LootTableMixin {

    @ModifyVariable(method = "getRandomItemsRaw(Lnet/minecraft/world/level/storage/loot/LootContext;Ljava/util/function/Consumer;)V", at = @At("HEAD"), argsOnly = true)
    private Consumer<ItemStack> wrapLootOutputConsumer(Consumer<ItemStack> originalConsumer, LootContext context) {
        if (!Tierify.CONFIG.lootContainerModifier) {
            return originalConsumer;
        }

        return stack -> {
            if (!stack.isEmpty() && !context.getLevel().isClientSide()) {
                ModifierUtils.applyTierIfNeeded(stack);
                ModifierUtils.logTierDebug("loot_generation", stack);
            }
            originalConsumer.accept(stack);
        };
    }
}
