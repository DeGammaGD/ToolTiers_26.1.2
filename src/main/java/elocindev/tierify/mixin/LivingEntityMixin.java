package elocindev.tierify.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import java.util.Map;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.At.Shift;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import elocindev.tierify.network.TieredServerPacket;

@SuppressWarnings("rawtypes")
@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Mutable
    @Shadow
    @Final
    private static TrackedData<Float> HEALTH;

    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    /**
     * Item attributes aren't applied until the player first ticks, which means any attributes such as bonus health are reset. This is annoying with health boosting armor.
     */
    @Redirect(method = "readCustomDataFromNbt", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/LivingEntity;setHealth(F)V"))
    private void readCustomDataFromNbtMixin(LivingEntity livingEntity, float health) {
        this.dataTracker.set(HEALTH, health);
    }

    @Inject(method = "getEquipmentChanges", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/attribute/AttributeContainer;removeModifiers(Lcom/google/common/collect/Multimap;)V", shift = Shift.AFTER), locals = LocalCapture.CAPTURE_FAILHARD)
    private void getEquipmentChangesMixin(CallbackInfoReturnable<Map<EquipmentSlot, ItemStack>> info, Map map, EquipmentSlot var2[], int var3, int var4, EquipmentSlot equipmentSlot,
            ItemStack itemStack) {
        final boolean[] hasTieredModifier = new boolean[] { false };
        itemStack.applyAttributeModifiers(equipmentSlot, (attribute, modifier) -> {
            if (modifier.id().toString().contains("tiered:")) {
                hasTieredModifier[0] = true;
            }
        });

        if (hasTieredModifier[0] && (Object) this instanceof ServerPlayerEntity) {
            boolean syncHealth = getEquippedStack(equipmentSlot).isEmpty();
            if (!syncHealth) {
                ItemStack newItemStack = getEquippedStack(equipmentSlot);
                if (!itemStack.isOf(newItemStack.getItem())) {
                    syncHealth = true;
                }
                if (!syncHealth) {
                    NbtComponent oldComponent = itemStack.get(DataComponentTypes.CUSTOM_DATA);
                    NbtCompound oldNbt = oldComponent != null ? oldComponent.copyNbt() : new NbtCompound();
                    oldNbt.remove("Damage");
                    oldNbt.remove("iced");

                    NbtComponent newComponent = newItemStack.get(DataComponentTypes.CUSTOM_DATA);
                    NbtCompound newNbt = newComponent != null ? newComponent.copyNbt() : new NbtCompound();
                    newNbt.remove("Damage");
                    newNbt.remove("iced");
                    if (!oldNbt.equals(newNbt)) {
                        syncHealth = true;
                    }
                }
            }
            if (syncHealth) {
                this.setHealth(this.getHealth() > this.getMaxHealth() ? this.getMaxHealth() : this.getHealth());
                TieredServerPacket.writeS2CHealthPacket((ServerPlayerEntity) (Object) this);
            }
        }
    }

    @Shadow
    public float getHealth() {
        return 0f;
    }

    @Shadow
    public final float getMaxHealth() {
        return 0;
    }

    @Shadow
    public void setHealth(float health) {
    }

    @Shadow
    public abstract ItemStack getEquippedStack(EquipmentSlot var1);

}
