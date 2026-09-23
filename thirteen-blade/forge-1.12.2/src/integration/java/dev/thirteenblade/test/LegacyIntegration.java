package dev.thirteenblade.test;

import com.mojang.authlib.GameProfile;
import dev.thirteenblade.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import net.minecraft.entity.*;
import net.minecraft.entity.boss.*;
import net.minecraft.entity.monster.*;
import net.minecraft.entity.passive.*;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.*;
import net.minecraft.inventory.*;
import net.minecraft.item.*;
import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.potion.PotionEffect;
import net.minecraft.util.*;
import net.minecraft.world.WorldServer;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.event.entity.living.LivingDeathEvent;
import net.minecraftforge.fml.common.*;
import net.minecraftforge.fml.common.event.*;
import net.minecraftforge.fml.common.registry.ForgeRegistries;

/** Opt-in dedicated-server integration tests, excluded from the distributed mod. */
@Mod(modid = "thirteenblade_tests", name = "Thirteen Blade Integration Tests", version = "1", dependencies = "required-after:thirteenblade")
public final class LegacyIntegration {
    private final List<String> results = new ArrayList<>();
    private WorldServer world;
    @Mod.EventHandler public void started(FMLServerStartedEvent event) {
        net.minecraft.server.MinecraftServer server = FMLCommonHandler.instance().getMinecraftServerInstance(); world = server.getWorld(0);
        try {
            run("growth heals both hands and respects base cap", this::growth);
            run("all twelve available souls and toughness above twenty", this::families);
            run("upgrade preserves NBT without sharing it", this::upgrade);
            run("ordinary potions take priority over sword effects", this::potions);
            run("shield cannot refill through switching or milk", this::shield);
            run("effects and flight expire after five seconds", this::linger);
            run("intrinsic enchants and fireproof entity persist", this::intrinsic);
            run("elite buffs and health survive saving", this::elites);
            run("cancelled deaths and invalid victims grant nothing", this::invalid);
            run("stolen buffs persist, cap levels and expire when configured", this::stolen);
            run("V stays armed beyond thirty seconds and cancels freely", this::armed);
            if (Loader.isModLoaded("firstaid")) run("First Aid limbs heal and rescale on growth", this::firstAid);
        } finally {
            try { Files.write(Paths.get("thirteenblade-integration-results.txt"), results, StandardCharsets.UTF_8); }
            catch (Exception e) { throw new RuntimeException(e); }
            server.initiateShutdown();
        }
    }
    private void run(String name, Runnable test) {
        try { test.run(); results.add("PASS " + name); }
        catch (Throwable failure) { results.add("FAIL " + name + ": " + failure); failure.printStackTrace(); }
        ThirteenBlade.LOGGER.info(results.get(results.size() - 1));
    }
    private void check(boolean value, String message) { if (!value) throw new AssertionError(message); }
    private FakePlayer player(ItemStack sword, EnumHand hand) {
        FakePlayer player = new FakePlayer(world, new GameProfile(UUID.randomUUID(), "BladeTest"));
        player.connection = new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(), new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND), player) {
            @Override public void sendPacket(net.minecraft.network.Packet<?> packet) {}
        };
        player.setHeldItem(hand, sword); return player;
    }
    private void kill(EntityPlayerMP player, EntityLivingBase victim) {
        victim.setPosition(0, 80, 0);
        // Use real damage/death hooks without random pack-specific spawn upgrades or retaliation.
        if (victim instanceof EntityDragon) {
            EntityDragon dragon = (EntityDragon)victim;
            dragon.getPhaseManager().setPhase(net.minecraft.entity.boss.dragon.phase.PhaseList.HOLDING_PATTERN);
            dragon.attackEntityFromPart(dragon.dragonPartHead, DamageSource.causePlayerDamage(player), 10000);
        } else victim.attackEntityFrom(DamageSource.causePlayerDamage(player), 10000);
    }
    private void growth() {
        for (boolean advanced : new boolean[]{false, true}) for (EnumHand hand : EnumHand.values()) {
            ItemStack sword = ThirteenBlade.sword(advanced); BladeData.write(sword).setInteger("Kills", 12);
            FakePlayer p = player(sword, hand); ItemStack carried = ThirteenBlade.sword(true); BladeData.write(carried).setInteger("Kills", 1300); p.inventory.mainInventory.set(10, carried);
            BladeGameplay.refreshAttributes(p); p.setHealth(3); kill(p, new EntityZombie(world));
            check(BladeData.level(sword) == 1 && p.getMaxHealth() == 220 && p.getHealth() == 220, "growth must heal to strongest carried maximum: level=" + BladeData.level(sword) + ", max=" + p.getMaxHealth() + ", current=" + p.getHealth());
            p.setHealth(3); kill(p, new EntityZombie(world)); check(p.getHealth() == 3, "ordinary kill must not heal");
            BladeData.write(sword).setInteger("Kills", 142); kill(p, new EntityZombie(world));
            check(BladeData.level(sword) == (advanced ? 11 : 10), "base cap / advanced growth");
            p.setHealth(3); BladeData.write(sword).setInteger("Kills", 155); kill(p, new EntityZombie(world));
            check(p.getHealth() == (advanced ? 220 : 3), "capped growth must not heal"); BladeEffects.release(p);
        }
    }
    private void families() {
        EntityLivingBase[] mobs = {new EntityZombie(world), new EntitySkeleton(world), new EntitySpider(world), new EntityCreeper(world), new EntityBlaze(world), new EntityWither(world), new EntityDragon(world), new EntityWitch(world), new EntitySlime(world), new EntityVindicator(world), new EntityGuardian(world), new EntityShulker(world)};
        ItemStack all = ThirteenBlade.sword(true);
        for (EntityLivingBase victim : mobs) {
            ItemStack sword = ThirteenBlade.sword(false); FakePlayer p = player(sword, EnumHand.OFF_HAND);
            BladeGameplay.toggleAbsorption(p); check(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) == 0, "arming must be free");
            kill(p, victim); SoulPower power = SoulPower.of(victim);
            check(power.known(sword) && BladeData.soulCount(sword) == 1, "missing soul " + power);
            check(BladeData.cooldownRemaining(sword, System.currentTimeMillis()) > 0, "trigger starts cooldown");
            check(p.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue() == 2, "two toughness per family");
            if (power.effect != null) check(p.isPotionActive(power.effect), "missing potion " + power);
            BladeData.unlock(all, power.flag); BladeEffects.release(p);
        }
        FakePlayer p = player(all, EnumHand.MAIN_HAND); BladeGameplay.refreshAttributes(p);
        check(p.getEntityAttribute(SharedMonsterAttributes.ARMOR_TOUGHNESS).getAttributeValue() == 24, "vanilla twenty cap must be raised");
        check(!BladeData.discover(all, new EntityHusk(world)), "zombie family deduplicates");
        check(BladeData.discover(all, new EntityGhast(world)), "other hostile species grant discovery");
        check(!BladeData.discover(all, new EntityCow(world)), "passive mobs do not grant toughness"); BladeEffects.release(p);
    }
    private void upgrade() {
        ItemStack sword = ThirteenBlade.sword(false); BladeData.write(sword).setInteger("Kills", 1300); BladeData.unlock(sword, "DragonFlight"); sword.setStackDisplayName("Ancient blade");
        InventoryCrafting grid = new InventoryCrafting(new Container() { public boolean canInteractWith(EntityPlayer player) { return true; } }, 2, 2);
        grid.setInventorySlotContents(0, sword); grid.setInventorySlotContents(3, new ItemStack(Blocks.DRAGON_EGG));
        net.minecraft.item.crafting.IRecipe recipe = new DragonUpgradeRecipe(); ItemStack result = recipe.getCraftingResult(grid);
        check(result.getItem() == ThirteenBlade.DRAGON_SWORD && result.getTagCompound().equals(sword.getTagCompound()) && BladeData.level(result) == 100, "upgrade copies all data");
        BladeData.addKill(result); check(BladeData.kills(sword) == 1300, "no shared NBT");
        grid.setInventorySlotContents(1, new ItemStack(Blocks.DIRT)); check(!recipe.matches(grid, world), "reject extra ingredients");
        check(ForgeRegistries.RECIPES.containsKey(new ResourceLocation("thirteenblade", "thirteen_blade")), "base recipe loaded");
    }
    private void potions() {
        ItemStack sword = ThirteenBlade.sword(false); BladeData.unlock(sword, "NightSight"); FakePlayer p = player(sword, EnumHand.MAIN_HAND); BladeEffects.refresh(p);
        PotionEffect ordinary = new PotionEffect(MobEffects.NIGHT_VISION, 600, 1); p.addPotionEffect(ordinary);
        check(p.getActivePotionEffect(MobEffects.NIGHT_VISION) == ordinary, "real potion must replace sword duration");
        BladeEffects.refresh(p); BladeEffects.release(p); check(p.getActivePotionEffect(MobEffects.NIGHT_VISION) == ordinary, "release must keep real potions");
        BladeData.unlock(sword, "HungerWard"); p.addPotionEffect(new PotionEffect(MobEffects.HUNGER, 200)); check(!p.isPotionActive(MobEffects.HUNGER), "hunger blocked");
    }
    private void shield() {
        ItemStack sword = ThirteenBlade.sword(false); BladeData.unlock(sword, "CreeperShield"); FakePlayer p = player(sword, EnumHand.MAIN_HAND); BladeEffects.refresh(p);
        check(p.getAbsorptionAmount() == 4, "initial shield"); p.setAbsorptionAmount(1); p.removePotionEffect(MobEffects.ABSORPTION); BladeEffects.refresh(p);
        check(p.getAbsorptionAmount() == 1, "milk cannot refill");
        BladeEffects.release(p); BladeEffects.refresh(p); check(p.getAbsorptionAmount() == 1, "re-equip cannot refill");
        BladeEffects.replenishShield(p, sword); check(p.getAbsorptionAmount() == 4, "new absorption reward refills"); BladeEffects.release(p);
    }
    private void linger() {
        long originalTime = world.getTotalWorldTime();
        try {
            ItemStack sword = ThirteenBlade.sword(false); BladeData.unlock(sword, "NightSight"); BladeData.unlock(sword, "DragonFlight");
            FakePlayer p = player(sword, EnumHand.MAIN_HAND); BladeEffects.refresh(p); check(p.capabilities.allowFlying, "flight granted");
            p.setHeldItem(EnumHand.MAIN_HAND, ItemStack.EMPTY); BladeEffects.refresh(p);
            world.getWorldInfo().setWorldTotalTime(originalTime + 99); BladeEffects.refresh(p); check(p.isPotionActive(MobEffects.NIGHT_VISION) && p.capabilities.allowFlying, "five-second grace");
            world.getWorldInfo().setWorldTotalTime(originalTime + 100); BladeEffects.refresh(p); check(!p.isPotionActive(MobEffects.NIGHT_VISION) && !p.capabilities.allowFlying, "grace expires");
            BladeEffects.release(p);
        } finally { world.getWorldInfo().setWorldTotalTime(originalTime); }
    }
    private void intrinsic() {
        for (boolean advanced : new boolean[]{false, true}) {
            ItemStack sword = ThirteenBlade.sword(advanced); sword.addEnchantment(Enchantments.SHARPNESS, 4); Map<net.minecraft.enchantment.Enchantment,Integer> stronger = net.minecraft.enchantment.EnchantmentHelper.getEnchantments(sword); stronger.put(Enchantments.LOOTING, 5); net.minecraft.enchantment.EnchantmentHelper.setEnchantments(stronger, sword); BladeEnchantments.ensure(sword);
            check(net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantments.LOOTING, sword) == 5 && net.minecraft.enchantment.EnchantmentHelper.getEnchantmentLevel(Enchantments.BINDING_CURSE, sword) == 1, "intrinsic enchants preserve stronger looting");
            check(!sword.isItemStackDamageable(), "unbreakable");
            Entity item = new ThirteenBladeItem.FireproofItem(world, 0, 80, 0, sword);
            check(!item.attackEntityFrom(DamageSource.LAVA, 100), "fire immune");
            NBTTagCompound saved = new NBTTagCompound(); check(item.writeToNBTAtomically(saved), "custom dropped entity has registry identity");
            Entity restored = EntityList.createEntityFromNBT(saved, world); check(restored instanceof ThirteenBladeItem.FireproofItem, "dropped sword reloads");
        }
    }
    private void elites() {
        EntityCreeper mob = new EntityCreeper(world); EliteMobs.empower(mob); check(mob.getMaxHealth() == 30 && !mob.getActivePotionEffects().isEmpty(), "elite stronger with buffs");
        NBTTagCompound saved = new NBTTagCompound(); mob.writeToNBT(saved); EntityCreeper restored = new EntityCreeper(world); restored.readFromNBT(saved);
        check(restored.getMaxHealth() == 30 && !EliteMobs.eligible(restored), "elite persists without reapplying");
        check(!EliteMobs.eligible(new EntityWither(world)), "boss excluded");
    }
    private void invalid() {
        check(!BladeGameplay.validVictim(new EntityVillager(world)), "villagers excluded"); EntityZombie baby = new EntityZombie(world); baby.setChild(true); check(!BladeGameplay.validVictim(baby), "babies excluded");
        EntityWolf pet = new EntityWolf(world); pet.setTamed(true); check(!BladeGameplay.validVictim(pet), "tamed pets excluded");
        ItemStack sword = ThirteenBlade.sword(false); FakePlayer p = player(sword, EnumHand.MAIN_HAND);
        LivingDeathEvent event = new LivingDeathEvent(new EntityZombie(world), DamageSource.causePlayerDamage(p)); event.setCanceled(true); MinecraftForge.EVENT_BUS.post(event);
        check(BladeData.kills(sword) == 0, "cancelled event grants nothing");
    }
    private void firstAid() {
        try {
            ItemStack sword = ThirteenBlade.sword(true); BladeData.write(sword).setInteger("Kills", 12);
            EntityPlayerMP p = new EntityPlayerMP(world.getMinecraftServer(), world, new GameProfile(UUID.randomUUID(), "BladeFirstAid"), new net.minecraft.server.management.PlayerInteractionManager(world));
            p.connection = new net.minecraft.network.NetHandlerPlayServer(world.getMinecraftServer(), new net.minecraft.network.NetworkManager(net.minecraft.network.EnumPacketDirection.SERVERBOUND), p) {
                @Override public void sendPacket(net.minecraft.network.Packet<?> packet) {}
            };
            p.setHeldItem(EnumHand.OFF_HAND, sword);
            net.minecraftforge.common.capabilities.Capability<?> cap = (net.minecraftforge.common.capabilities.Capability<?>)Class.forName("ichttt.mods.firstaid.api.CapabilityExtendedHealthSystem").getField("INSTANCE").get(null);
            Object model = p.getCapability(cap, null); check(model != null, "First Aid capability attached");
            for (Object part : (Iterable<?>)model) part.getClass().getField("currentHealth").setFloat(part, 1);
            kill(p, new EntityZombie(world));
            for (Object part : (Iterable<?>)model) check(part.getClass().getField("currentHealth").getFloat(part) == ((Number)part.getClass().getMethod("getMaxHealth").invoke(part)).floatValue(), "every limb is full");
            for (Object part : (Iterable<?>)model) part.getClass().getField("currentHealth").setFloat(part, 1);
            kill(p, new EntityZombie(world));
            for (Object part : (Iterable<?>)model) check(part.getClass().getField("currentHealth").getFloat(part) == 1, "ordinary kill must not heal limbs");
            BladeData.write(sword).setInteger("Kills", 1300); BladeGameplay.refreshAttributes(p);
            for (Object part : (Iterable<?>)model) check(((Number)part.getClass().getMethod("getMaxHealth").invoke(part)).floatValue() > 6, "carried vitality rescales limb maximum");
            BladeEffects.release(p);
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private void stolen() {
        ItemStack sword = ThirteenBlade.sword(false); FakePlayer p = player(sword, EnumHand.MAIN_HAND);
        EntityCreeper victim = new EntityCreeper(world); victim.addPotionEffect(new PotionEffect(MobEffects.SPEED, 400, 4)); victim.addPotionEffect(new PotionEffect(MobEffects.POISON, 400));
        BladeGameplay.toggleAbsorption(p); kill(p, victim);
        BladeData.StoredEffect speed = BladeData.stolenEffects(sword, System.currentTimeMillis()).get(MobEffects.SPEED);
        check(speed != null && speed.amplifier == 2 && speed.expiresAt == -1, "capture beneficial effect capped at III, infinite by default");
        check(!BladeData.stolenEffects(sword, System.currentTimeMillis()).containsKey(MobEffects.POISON), "debuff not stolen");
        ItemStack restored = new ItemStack(sword.writeToNBT(new NBTTagCompound()));
        check(BladeData.stolenEffects(restored, System.currentTimeMillis()).containsKey(MobEffects.SPEED), "captured effect survives item save");
        int previous = ThirteenBlade.balance.stolenEffectDurationSeconds;
        try {
            ThirteenBlade.balance.stolenEffectDurationSeconds = 2; ItemStack finite = ThirteenBlade.sword(false);
            BladeData.capture(finite, new PotionEffect(MobEffects.SPEED, 200), 10000);
            check(BladeData.stolenEffects(finite, 11999).size() == 1 && BladeData.stolenEffects(finite, 12000).isEmpty(), "configured expiry uses wall-clock time");
        } finally { ThirteenBlade.balance.stolenEffectDurationSeconds = previous; BladeEffects.release(p); }
    }
    private void armed() {
        long original = world.getTotalWorldTime();
        try {
            ItemStack sword = ThirteenBlade.sword(false); FakePlayer p = player(sword, EnumHand.MAIN_HAND);
            BladeGameplay.toggleAbsorption(p); world.getWorldInfo().setWorldTotalTime(original + 640);
            kill(p, new EntitySkeleton(world)); check(SoulPower.SKELETON.known(sword), "armed state has no thirty-second expiry"); BladeEffects.release(p);
            ItemStack cancelled = ThirteenBlade.sword(false); FakePlayer q = player(cancelled, EnumHand.OFF_HAND);
            BladeGameplay.toggleAbsorption(q); world.getWorldInfo().setWorldTotalTime(original + 646); BladeGameplay.toggleAbsorption(q);
            kill(q, new EntitySkeleton(world)); check(!SoulPower.SKELETON.known(cancelled) && BladeData.cooldownRemaining(cancelled, System.currentTimeMillis()) == 0, "cancel is free and does not absorb"); BladeEffects.release(q);
        } finally { world.getWorldInfo().setWorldTotalTime(original); }
    }
}
