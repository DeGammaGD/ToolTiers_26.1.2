package elocindev.tierify.mixin.client;

import java.util.List;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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
            return component;
        }

        String formatted = formatPercentNumbers(raw);
        if (formatted.equals(raw)) {
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
                Tierify.LOGGER.info("Tooltip name read tier {} for {}", tier, BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()));
                Tierify.LOGGER.info("Tooltip name attributes for {} -> {}", tier, potentialAttribute.getAttributes());
                Tierify.LOGGER.info("[TierifyDebug][Tooltip] item={} tier={} attributes={}", BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()), tier, potentialAttribute.getAttributes());
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
        for (int i = 0; i < tooltip.size(); i++) {
            Component line = formatCriticalChanceComponent(tooltip.get(i));
            tooltip.set(i, formatTooltipComponent(line));
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
                Tierify.LOGGER.info("Tooltip body read tier {} for {}", tier, BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()));
                Tierify.LOGGER.info("Tooltip body attributes for {} -> {}", tier, potentialAttribute.getAttributes());
                Tierify.LOGGER.info("[TierifyDebug][Tooltip] item={} tier={} attributes={}", BuiltInRegistries.ITEM.getKey(((ItemStack) (Object) this).getItem()), tier, potentialAttribute.getAttributes());
                tooltip.add(Component.literal("Tier: ").append(Component.translatable(potentialAttribute.getID() + ".label").setStyle(potentialAttribute.getStyle())));
            }
        }

        info.setReturnValue(tooltip);
    }
}
