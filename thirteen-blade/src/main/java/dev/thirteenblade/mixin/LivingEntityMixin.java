package dev.thirteenblade.mixin;

import dev.thirteenblade.BladeData;
import dev.thirteenblade.BladeEffects;
import dev.thirteenblade.BladeInventory;
import dev.thirteenblade.SoulPower;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.damage.DamageTypes;
import dev.thirteenblade.ThirteenBlade;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.Entity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.player.PlayerEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "addStatusEffect(Lnet/minecraft/entity/effect/StatusEffectInstance;Lnet/minecraft/entity/Entity;)Z", at = @At("HEAD"))
    private void thirteenblade$prioritizeExternalPotion(StatusEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayerEntity player && !BladeEffects.owned(effect)
                && BladeEffects.owned(player.getStatusEffect(effect.getEffectType())))
            player.removeStatusEffect(effect.getEffectType());
    }

    @Inject(method = "onStatusEffectRemoved", at = @At("HEAD"))
    private void thirteenblade$rememberShield(StatusEffectInstance effect, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayerEntity player) BladeEffects.beforeRemoval(player, effect);
    }

    @Inject(method = "canHaveStatusEffect", at = @At("HEAD"), cancellable = true)
    private void thirteenblade$wardHunger(StatusEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof PlayerEntity player && ThirteenBlade.isSword(BladeInventory.activeSword(player))
                && ((effect.getEffectType() == StatusEffects.HUNGER && SoulPower.ZOMBIE.known(BladeInventory.activeSword(player)))
                || ((effect.getEffectType() == StatusEffects.BLINDNESS || effect.getEffectType() == StatusEffects.DARKNESS)
                    && SoulPower.WARDEN.known(BladeInventory.activeSword(player))))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "damage", at = @At("RETURN"))
    private void thirteenblade$weakeningHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && amount > 0 && source.isOf(DamageTypes.PLAYER_ATTACK)
                && source.getAttacker() instanceof ServerPlayerEntity player
                && SoulPower.VINDICATOR.known(BladeInventory.activeSword(player)))
            ((LivingEntity) (Object) this).addStatusEffect(new StatusEffectInstance(StatusEffects.WEAKNESS, 100, 0), player);
    }
}
