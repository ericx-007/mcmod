package dev.thirteenblade;

import java.lang.reflect.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.fml.common.Loader;

/** Optional First Aid 1.6.x API bridge. No First Aid classes are linked on an ordinary Forge server. */
public final class FirstAidBridge {
    private static Capability<?> capability;
    private static boolean failed;
    private FirstAidBridge() {}
    public static void initialize() {
        if (!Loader.isModLoaded("firstaid")) return;
        // First Aid registers its capability later in preInit; resolve lazily on the server thread.
    }
    private static Object model(EntityPlayer player) throws ReflectiveOperationException {
        if (!Loader.isModLoaded("firstaid") || failed) return null;
        if (capability == null) capability = (Capability<?>)Class.forName("ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem").getField("INSTANCE").get(null);
        return capability == null ? null : player.getCapability(capability, null);
    }
    public static void rescale(EntityPlayer player) {
        try {
            Object model = model(player);
            if (model != null) { model.getClass().getMethod("runScaleLogic", EntityPlayer.class).invoke(model, player); model.getClass().getMethod("scheduleResync").invoke(model); }
        } catch (ReflectiveOperationException | RuntimeException e) { failure(e); }
    }
    public static void healFully(EntityPlayer player) {
        try {
            Object model = model(player); if (model == null) return;
            model.getClass().getMethod("runScaleLogic", EntityPlayer.class).invoke(model, player);
            for (Object part : (Iterable<?>)model) {
                float maximum = ((Number)part.getClass().getMethod("getMaxHealth").invoke(part)).floatValue();
                part.getClass().getField("currentHealth").setFloat(part, maximum);
            }
            model.getClass().getMethod("scheduleResync").invoke(model);
        } catch (ReflectiveOperationException | RuntimeException e) { failure(e); }
    }
    private static void failure(Exception error) { if (!failed) ThirteenBlade.LOGGER.error("First Aid compatibility could not initialize; vanilla health remains available", error); failed = true; }
}
