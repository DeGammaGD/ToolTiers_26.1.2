package elocindev.tierify.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;

@Mixin(Mob.class)
public class MobEntityMixin {

    @Inject(method = "initialize", at = @At("TAIL"))
    private void initializeMixin(ServerLevelAccessor world, DifficultyInstance difficulty, MobSpawnType spawnReason, @Nullable SpawnGroupData entityData,
            CallbackInfoReturnable<SpawnGroupData> info) {
        if (Tierify.CONFIG.entityItemModifier) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = this.getEquippedStack(equipmentSlot);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ModifierUtils.setItemStackAttribute(null, itemStack, false);
            }
        }
    }

    @Shadow
    public ItemStack getEquippedStack(EquipmentSlot slot) {
        return null;
    }

}
