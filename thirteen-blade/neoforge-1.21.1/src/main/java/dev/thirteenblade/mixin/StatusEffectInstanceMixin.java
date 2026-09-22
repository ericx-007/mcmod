package dev.thirteenblade.mixin;

import dev.thirteenblade.BladeOwnedEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.nbt.CompoundTag;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MobEffectInstance.class)
public abstract class StatusEffectInstanceMixin implements BladeOwnedEffect {
    @Unique private boolean thirteenblade$owned;
    public boolean thirteenblade$isOwned() { return thirteenblade$owned; }
    public void thirteenblade$setOwned(boolean owned) { thirteenblade$owned = owned; }

    @Inject(method = "save", at = @At("RETURN"))
    private void thirteenblade$writeOwner(CallbackInfoReturnable<net.minecraft.nbt.Tag> cir) {
        if (thirteenblade$owned && cir.getReturnValue() instanceof CompoundTag tag) tag.putBoolean("ThirteenBladeOwned", true);
    }

    @Inject(method = "load(Lnet/minecraft/nbt/CompoundTag;)Lnet/minecraft/world/effect/MobEffectInstance;", at = @At("RETURN"))
    private static void thirteenblade$readOwner(CompoundTag nbt, CallbackInfoReturnable<MobEffectInstance> cir) {
        if (cir.getReturnValue() != null)
            ((BladeOwnedEffect) cir.getReturnValue()).thirteenblade$setOwned(nbt.getBoolean("ThirteenBladeOwned"));
    }
}
