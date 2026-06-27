package elocindev.tierify.mixin;

import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import draylar.tiered.api.ModifierUtils;
import elocindev.tierify.Tierify;
import net.minecraft.world.DifficultyInstance;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.SpawnGroupData;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.phys.AABB;

@Mixin(Mob.class)
public class MobEntityMixin {

    @Inject(method = "finalizeSpawn", at = @At("TAIL"))
    private void initializeMixin(ServerLevelAccessor world, DifficultyInstance difficulty, EntitySpawnReason spawnReason, @Nullable SpawnGroupData entityData,
            CallbackInfoReturnable<SpawnGroupData> info) {
        if (Tierify.CONFIG.entityItemModifier) {
            for (EquipmentSlot equipmentSlot : EquipmentSlot.values()) {
                ItemStack itemStack = ((Mob) (Object) this).getItemBySlot(equipmentSlot);
                if (itemStack.isEmpty()) {
                    continue;
                }
                ModifierUtils.applyTierIfNeeded(itemStack);
            }
        }
    }

    @Inject(method = "dropFromLootTable", at = @At("TAIL"))
    private void tierifyLootTableDrops(ServerLevel level, DamageSource damageSource, boolean hasLastDamagePlayer, CallbackInfo info) {
        applyTierToNearbyDrops(level);
    }

    @Inject(method = "dropCustomDeathLoot", at = @At("TAIL"))
    private void tierifyCustomDrops(ServerLevel level, DamageSource damageSource, boolean hasLastDamagePlayer, CallbackInfo info) {
        applyTierToNearbyDrops(level);
    }

    private void applyTierToNearbyDrops(ServerLevel level) {
        if (!Tierify.CONFIG.entityItemModifier) {
            return;
        }

        Mob mob = (Mob) (Object) this;
        AABB searchBox = mob.getBoundingBox().inflate(2.0D);
        for (ItemEntity itemEntity : level.getEntitiesOfClass(ItemEntity.class, searchBox, dropped -> !dropped.getItem().isEmpty())) {
            ModifierUtils.applyTierIfNeeded(itemEntity.getItem());
            ModifierUtils.logTierDebug("mob_drops", itemEntity.getItem());
        }
    }

}
