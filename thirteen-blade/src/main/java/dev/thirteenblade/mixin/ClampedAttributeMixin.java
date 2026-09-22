package dev.thirteenblade.mixin;

import net.minecraft.entity.attribute.ClampedEntityAttribute;
import net.minecraft.entity.attribute.EntityAttributes;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Allow the requested 2 toughness per family to remain effective beyond vanilla's 20-point ceiling. */
@Mixin(ClampedEntityAttribute.class)
public abstract class ClampedAttributeMixin {
    @Inject(method = "clamp", at = @At("HEAD"), cancellable = true)
    private void thirteenblade$allowSoulToughness(double value, CallbackInfoReturnable<Double> cir) {
        if ((Object) this == EntityAttributes.GENERIC_ARMOR_TOUGHNESS && Double.isFinite(value))
            cir.setReturnValue(Math.max(0, Math.min(1024, value)));
    }
}
