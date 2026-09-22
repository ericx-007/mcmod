package dev.thirteenblade.mixin;

import dev.thirteenblade.BladeOwnedEffect;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.nbt.NbtCompound;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(StatusEffectInstance.class)
public abstract class StatusEffectInstanceMixin implements BladeOwnedEffect {
    @Unique private boolean thirteenblade$owned;
    public boolean thirteenblade$isOwned() { return thirteenblade$owned; }
    public void thirteenblade$setOwned(boolean owned) { thirteenblade$owned = owned; }

    @Inject(method = "writeNbt", at = @At("RETURN"))
    private void thirteenblade$writeOwner(NbtCompound nbt, CallbackInfoReturnable<NbtCompound> cir) {
        if (thirteenblade$owned) cir.getReturnValue().putBoolean("ThirteenBladeOwned", true);
    }

    @Inject(method = "fromNbt(Lnet/minecraft/nbt/NbtCompound;)Lnet/minecraft/entity/effect/StatusEffectInstance;", at = @At("RETURN"))
    private static void thirteenblade$readOwner(NbtCompound nbt, CallbackInfoReturnable<StatusEffectInstance> cir) {
        if (cir.getReturnValue() != null)
            ((BladeOwnedEffect) cir.getReturnValue()).thirteenblade$setOwned(nbt.getBoolean("ThirteenBladeOwned"));
    }
}
