package elocindev.tierify.mixin.client;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import draylar.tiered.api.CustomEntityAttributes;
import draylar.tiered.api.PotentialAttribute;
import elocindev.tierify.Tierify;
import elocindev.tierify.util.TieredTooltip;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Environment(EnvType.CLIENT)
@Mixin(ItemStack.class)
@SuppressWarnings({"null"})
public abstract class ItemStackClientMixin {

    private static final Pattern PERCENT_NUMBER_PATTERN = Pattern.compile("([+-]?\\d+(?:\\.\\d+)?)%");
    private static final Pattern PLAIN_NUMBER_PATTERN = Pattern.compile("(\\d+(?:\\.\\d+)?)");

    /**
     * Converts the vanilla Critical Chance attribute line (rendered as a raw decimal, e.g. "+0.65")
     * into a percentage ("+65%"). Display-only: the underlying modifier value is untouched. Lines that
     * already contain a '%' (modifiers stored as a multiplied operation) are left for the percent rounder.
     */
    private static Component formatCriticalChanceComponent(Component component) {
        String critName = Component.translatable("generic.critical_chance").getString();
        String raw = component.getString();
        if (critName.isEmpty() || !raw.contains(critName) || raw.indexOf('%') >= 0) {
            return component;
        }

        Matcher matcher = PLAIN_NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return component;
        }

        String replacement;
        try {
            double value = Double.parseDouble(matcher.group(1)) * 100.0;
            BigDecimal rounded = new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
            replacement = rounded.toPlainString() + "%";
        } catch (NumberFormatException ignored) {
            return component;
        }

        String formatted = raw.substring(0, matcher.start(1)) + replacement + raw.substring(matcher.end(1));
        return Component.literal(formatted).setStyle(component.getStyle());
    }

    private static Component formatMiningEfficiencyComponent(Component component) {
        String miningName = Component.translatable("generic.mining_efficiency").getString();
        if (miningName.isEmpty()) {
            miningName = Component.translatable("attribute.name.minecraft.mining_efficiency").getString();
        }
        String raw = component.getString();
        if (miningName.isEmpty() || !raw.contains(miningName) || raw.indexOf('%') >= 0) {
            return component;
        }

        Matcher matcher = PLAIN_NUMBER_PATTERN.matcher(raw);
        if (!matcher.find()) {
            return component;
        }

        String replacement;
        try {
            double value = Double.parseDouble(matcher.group(1)) * 100.0;
            BigDecimal rounded = new BigDecimal(value).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
            replacement = rounded.toPlainString() + "%";
        } catch (NumberFormatException ignored) {
            return component;
        }

        String formatted = raw.substring(0, matcher.start(1)) + replacement + raw.substring(matcher.end(1));
        return Component.literal(formatted).setStyle(component.getStyle());
    }

    private static boolean isMiningEfficiencyLine(Component component) {
        String raw = component.getString();
        String lower = raw.toLowerCase();
        String customMiningName = Component.translatable("generic.mining_efficiency").getString();
        if (!customMiningName.isEmpty() && raw.contains(customMiningName)) {
            return true;
        }

        String miningName = Component.translatable("attribute.name.minecraft.mining_efficiency").getString();
        if (!miningName.isEmpty() && raw.contains(miningName)) {
            return true;
        }

        // Some vanilla/enchantment contexts render a separate total mining-speed line.
        // Collapse it with mining-efficiency so only one user-facing line remains.
        String[] miningSpeedKeys = new String[] {
                "attribute.name.minecraft.block_break_speed",
                "attribute.name.player.block_break_speed",
                "attribute.name.generic.block_break_speed",
                "attribute.name.minecraft.mining_speed",
                "attribute.name.player.mining_speed",
                "attribute.name.generic.mining_speed"
        };

        for (String key : miningSpeedKeys) {
            String value = Component.translatable(key).getString();
            if (!value.isEmpty() && raw.contains(value)) {
                return true;
            }
        }

        if (lower.contains("mining speed") || lower.contains("block break speed") || lower.contains("block breaking speed")) {
            return true;
        }

        return lower.contains("mining") && lower.contains("speed");
    }

    private static boolean hasPercentValue(Component component) {
        return component.getString().indexOf('%') >= 0;
    }

    private static boolean hasCustomMiningEfficiencyAttribute(ItemStack stack) {
        var component = stack.get(DataComponents.ATTRIBUTE_MODIFIERS);
        if (component == null) {
            return false;
        }

        for (var entry : component.modifiers()) {
            Identifier attributeId = BuiltInRegistries.ATTRIBUTE.getKey(entry.attribute().value());
            if (attributeId != null && CustomEntityAttributes.MINING_EFFICIENCY_ID.equals(attributeId.toString())) {
                return true;
            }
        }

        return false;
    }

    private static boolean isEfficiencyEnchantmentLine(Component component) {
        String efficiencyName = Component.translatable("enchantment.minecraft.efficiency").getString();
        return !efficiencyName.isEmpty() && component.getString().contains(efficiencyName);
    }

    private static String formatPercentNumbers(String text) {
        Matcher matcher = PERCENT_NUMBER_PATTERN.matcher(text);
        StringBuffer output = new StringBuffer();

        while (matcher.find()) {
            String rawNumber = matcher.group(1);
            String replacement = rawNumber;

            try {
                BigDecimal rounded = new BigDecimal(rawNumber).setScale(1, RoundingMode.HALF_UP).stripTrailingZeros();
                replacement = rounded.toPlainString();
            } catch (NumberFormatException ignored) {
            }

            matcher.appendReplacement(output, Matcher.quoteReplacement(replacement + "%"));
        }

        matcher.appendTail(output);
        return output.toString();
    }

    private static Component formatTooltipComponent(Component component) {
        String raw = component.getString();

        if (raw.indexOf('%') < 0) {
            if (!raw.equals(component.getString())) {
                return Component.literal(raw).setStyle(component.getStyle());
            }
            return component;
        }

        String formatted = formatPercentNumbers(raw);
        if (formatted.equals(raw)) {
            if (!raw.equals(component.getString())) {
                return Component.literal(raw).setStyle(component.getStyle());
            }
            return component;
        }

        return Component.literal(formatted).setStyle(component.getStyle());
    }

    @Inject(method = "getHoverName", at = @At("RETURN"), cancellable = true, require = 0)
    private void getNameMixin(CallbackInfoReturnable<Component> info) {
        CustomData component = ((ItemStack) (Object) this).get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();
        if (component != null && !root.contains("display") && root.contains(Tierify.NBT_SUBTAG_KEY)) {
            CompoundTag tiered = root.getCompound(Tierify.NBT_SUBTAG_KEY).orElse(null);
            if (tiered == null) {
                return;
            }
            Identifier tier = Identifier.parse(tiered.getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse(""));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                MutableComponent text = Component.translatable(potentialAttribute.getID() + ".label");

                if (Tierify.CLIENT_CONFIG.showPlatesOnName) {
                    text = Component.literal(TieredTooltip.getPlateForModifier(text.getString()));
                }

                info.setReturnValue(text.append(" ").append(info.getReturnValue()).setStyle(potentialAttribute.getStyle()));
            }
        }
    }

    @Inject(method = "getTooltipLines", at = @At("RETURN"), cancellable = true, require = 0)
    private void getTooltipMixin(Item.TooltipContext context, Player player, TooltipFlag type, CallbackInfoReturnable<List<Component>> info) {
        List<Component> tooltip = info.getReturnValue();
        ItemStack self = (ItemStack) (Object) this;
        boolean hasCustomMining = hasCustomMiningEfficiencyAttribute(self);
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = formatCriticalChanceComponent(tooltip.get(i));
            line = formatMiningEfficiencyComponent(line);
            line = formatTooltipComponent(line);
            tooltip.set(i, line);
        }

        int keepMiningLineIndex = -1;
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = tooltip.get(i);
            if (!isMiningEfficiencyLine(line)) {
                continue;
            }

            if (keepMiningLineIndex == -1) {
                keepMiningLineIndex = i;
                continue;
            }

            // Prefer the explicit percent-based line when both vanilla total speed and tier line exist.
            if (hasPercentValue(line) && !hasPercentValue(tooltip.get(keepMiningLineIndex))) {
                keepMiningLineIndex = i;
            }
        }

        if (keepMiningLineIndex != -1) {
            for (int i = tooltip.size() - 1; i >= 0; i--) {
                if (i == keepMiningLineIndex) {
                    continue;
                }
                if (isMiningEfficiencyLine(tooltip.get(i))) {
                    tooltip.remove(i);
                }
            }
        }

        if (hasCustomMining) {
            for (int i = tooltip.size() - 1; i >= 0; i--) {
                if (keepMiningLineIndex != -1 && i == keepMiningLineIndex) {
                    continue;
                }
                if (isEfficiencyEnchantmentLine(tooltip.get(i))) {
                    tooltip.remove(i);
                }
            }
        }

        CustomData component = ((ItemStack) (Object) this).get(DataComponents.CUSTOM_DATA);
        CompoundTag root = component != null ? component.copyTag() : new CompoundTag();

        if (component != null && root.contains(Tierify.NBT_SUBTAG_KEY)) {
            CompoundTag tiered = root.getCompound(Tierify.NBT_SUBTAG_KEY).orElse(null);
            if (tiered == null) {
                return;
            }
            Identifier tier = Identifier.parse(tiered.getString(Tierify.NBT_SUBTAG_DATA_KEY).orElse(""));
            PotentialAttribute potentialAttribute = Tierify.ATTRIBUTE_DATA_LOADER.getItemAttributes().get(tier);

            if (potentialAttribute != null) {
                tooltip.add(Component.literal("Tier: ").append(Component.translatable(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle())));
            }
        }

        info.setReturnValue(tooltip);
    }
}
