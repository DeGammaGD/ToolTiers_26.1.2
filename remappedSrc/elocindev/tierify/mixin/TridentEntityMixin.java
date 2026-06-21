package elocindev.tierify.mixin;

import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import elocindev.tierify.util.AttributeHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.AbstractArrow;
import net.minecraft.world.entity.projectile.ThrownTrident;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.EntityHitResult;

@Mixin(ThrownTrident.class)
public abstract class TridentEntityMixin extends AbstractArrow {

    public TridentEntityMixin(EntityType<? extends AbstractArrow> entityType, Level world) {
        super(entityType, world);
    }

    @Inject(method = "onEntityHit", at = @At("HEAD"))
    private void onEntityHitMixin(EntityHitResult hitResult, CallbackInfo info) {
        if (this.getOwner() instanceof ServerPlayer) {
            this.setBaseDamage(AttributeHelper.getExtraRangeDamage((Player) this.getOwner(), (float) this.getBaseDamage()));
        }
    }
}
