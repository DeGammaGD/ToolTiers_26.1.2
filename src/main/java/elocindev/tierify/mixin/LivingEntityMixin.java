package elocindev.tierify.mixin;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.LivingEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.entity.data.TrackedData;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.world.World;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.network.TieredServerPacket;

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

    @Inject(method = "onEquipStack", at = @At("TAIL"))
    private void onEquipStackMixin(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo info) {
        if (!((Object) this instanceof ServerPlayerEntity serverPlayerEntity)) {
            return;
        }

        if (!hasTieredModifier(slot, oldStack) && !hasTieredModifier(slot, newStack)) {
            return;
        }

        boolean syncHealth = newStack.isEmpty() || !oldStack.isOf(newStack.getItem());
        if (!syncHealth) {
            NbtComponent oldComponent = oldStack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound oldNbt = oldComponent != null ? oldComponent.copyNbt() : new NbtCompound();
            oldNbt.remove("Damage");
            oldNbt.remove("iced");

            NbtComponent newComponent = newStack.get(DataComponentTypes.CUSTOM_DATA);
            NbtCompound newNbt = newComponent != null ? newComponent.copyNbt() : new NbtCompound();
            newNbt.remove("Damage");
            newNbt.remove("iced");
            syncHealth = !oldNbt.equals(newNbt);
        }

        if (syncHealth) {
            this.setHealth(Math.min(this.getHealth(), this.getMaxHealth()));
            TieredServerPacket.writeS2CHealthPacket(serverPlayerEntity);
        }
    }

    private static boolean hasTieredModifier(EquipmentSlot slot, ItemStack stack) {
        if (stack.isEmpty()) {
            return false;
        }

        final boolean[] hasTieredModifier = new boolean[] { false };
        stack.applyAttributeModifiers(slot, (attribute, modifier) -> {
            if (modifier.id().toString().contains("tiered:")) {
                hasTieredModifier[0] = true;
            }
        });
        return hasTieredModifier[0];
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
