package dev.thirteenblade.mixin;

import dev.thirteenblade.BladeData;
import dev.thirteenblade.BladeEffects;
import dev.thirteenblade.BladeInventory;
import dev.thirteenblade.SoulPower;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import dev.thirteenblade.ThirteenBlade;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Entity;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "addEffect(Lnet/minecraft/world/effect/MobEffectInstance;Lnet/minecraft/world/entity/Entity;)Z", at = @At("HEAD"))
    private void thirteenblade$prioritizeExternalPotion(MobEffectInstance effect, Entity source, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof ServerPlayer player && !BladeEffects.owned(effect)
                && BladeEffects.owned(player.getEffect(effect.getEffect())))
            player.removeEffect(effect.getEffect());
    }

    @Inject(method = "onEffectRemoved", at = @At("HEAD"))
    private void thirteenblade$rememberShield(MobEffectInstance effect, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) BladeEffects.beforeRemoval(player, effect);
    }

    @Inject(method = "canBeAffected", at = @At("HEAD"), cancellable = true)
    private void thirteenblade$wardHunger(MobEffectInstance effect, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this instanceof Player player && ThirteenBlade.isSword(BladeInventory.activeSword(player))
                && ((effect.getEffect() == MobEffects.HUNGER && SoulPower.ZOMBIE.known(BladeInventory.activeSword(player)))
                || ((effect.getEffect() == MobEffects.BLINDNESS || effect.getEffect() == MobEffects.DARKNESS)
                    && SoulPower.WARDEN.known(BladeInventory.activeSword(player))))) {
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "hurt", at = @At("RETURN"))
    private void thirteenblade$weakeningHit(DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue() && amount > 0 && source.is(DamageTypes.PLAYER_ATTACK)
                && source.getEntity() instanceof ServerPlayer player
                && SoulPower.VINDICATOR.known(BladeInventory.activeSword(player)))
            ((LivingEntity) (Object) this).addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 0), player);
    }
}
