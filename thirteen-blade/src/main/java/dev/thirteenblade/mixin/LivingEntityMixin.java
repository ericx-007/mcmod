package dev.thirteenblade.mixin;

import dev.thirteenblade.BladeData;
import dev.thirteenblade.BladeEffects;
import dev.thirteenblade.BladeInventory;
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
        if ((Object) this instanceof PlayerEntity player
                && effect.getEffectType() == StatusEffects.HUNGER
                && BladeInventory.activeSword(player).isOf(ThirteenBlade.SWORD)
                && BladeData.has(BladeInventory.activeSword(player), BladeData.HUNGER_WARD)) {
            cir.setReturnValue(false);
        }
    }
}
