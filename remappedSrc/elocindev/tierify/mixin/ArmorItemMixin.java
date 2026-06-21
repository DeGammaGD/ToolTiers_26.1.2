package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.EquipmentSlotGroup;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ArmorItem;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.ArmorMaterials;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ArmorItem.class)
public class ArmorItemMixin {

    @Shadow
    @Final
    protected Holder<ArmorMaterial> material;

    @Shadow
    @Final
    protected ArmorItem.Type type;

    private static final ResourceLocation KNOCKBACK_RESISTANCE_ID = ResourceLocation.fromNamespaceAndPath("tiered", "armor_knockback_resistance");

    @Inject(method = "getAttributeModifiers", at = @At("RETURN"), cancellable = true)
    private void getAttributeModifiersMixin(CallbackInfoReturnable<ItemAttributeModifiers> info) {
        if (this.material != ArmorMaterials.NETHERITE && this.material.value().knockbackResistance() > 0.0001f) {
            ItemAttributeModifiers modifiers = info.getReturnValue();
            boolean hasTieredKnockback = modifiers.modifiers().stream()
                    .anyMatch(entry -> entry.modifier().id().equals(KNOCKBACK_RESISTANCE_ID));

            if (!hasTieredKnockback) {
                info.setReturnValue(modifiers.withModifierAdded(
                        Attributes.KNOCKBACK_RESISTANCE,
                        new AttributeModifier(KNOCKBACK_RESISTANCE_ID, this.material.value().knockbackResistance(), AttributeModifier.Operation.ADD_VALUE),
                        EquipmentSlotGroup.bySlot(this.type.getSlot())
                ));
            }
        }
    }
}
