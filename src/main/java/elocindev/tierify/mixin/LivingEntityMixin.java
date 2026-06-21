package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Mutable;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import elocindev.tierify.network.TieredServerPacket;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {

    @Mutable
    @Shadow
    @Final
    private static EntityDataAccessor<Float> DATA_HEALTH_ID;

    public LivingEntityMixin(EntityType<?> type, Level world) {
        super(type, world);
    }

    /**
     * Item attributes aren't applied until the player first ticks, which means any attributes such as bonus health are reset. This is annoying with health boosting armor.
     */
    @Redirect(method = "readAdditionalSaveData", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/entity/LivingEntity;setHealth(F)V"))
    private void readCustomDataFromNbtMixin(LivingEntity livingEntity, float health) {
        this.entityData.set(DATA_HEALTH_ID, health);
    }

    @Inject(method = { "onEquipItem", "onEquipStack" }, at = @At("TAIL"), require = 0)
    private void onEquipStackMixin(EquipmentSlot slot, ItemStack oldStack, ItemStack newStack, CallbackInfo info) {
        if (!((Object) this instanceof ServerPlayer serverPlayerEntity)) {
            return;
        }

        if (!hasTieredModifier(slot, oldStack) && !hasTieredModifier(slot, newStack)) {
            return;
        }

        boolean syncHealth = newStack.isEmpty() || !oldStack.is(newStack.getItem());
        if (!syncHealth) {
            CustomData oldComponent = oldStack.get(DataComponents.CUSTOM_DATA);
            CompoundTag oldNbt = oldComponent != null ? oldComponent.copyTag() : new CompoundTag();
            oldNbt.remove("Damage");
            oldNbt.remove("iced");

            CustomData newComponent = newStack.get(DataComponents.CUSTOM_DATA);
            CompoundTag newNbt = newComponent != null ? newComponent.copyTag() : new CompoundTag();
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
        stack.forEachModifier(slot, (attribute, modifier) -> {
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

}
