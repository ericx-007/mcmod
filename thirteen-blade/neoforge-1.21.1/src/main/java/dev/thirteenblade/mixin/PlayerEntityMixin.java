package dev.thirteenblade.mixin;

import dev.thirteenblade.BladeGameplay;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Player.class)
public abstract class PlayerEntityMixin {
    @Inject(method = "attack", at = @At("HEAD"))
    private void thirteenblade$refreshBeforeAttack(Entity target, CallbackInfo ci) {
        if ((Object) this instanceof ServerPlayer player) BladeGameplay.refreshAttributes(player);
    }
}
