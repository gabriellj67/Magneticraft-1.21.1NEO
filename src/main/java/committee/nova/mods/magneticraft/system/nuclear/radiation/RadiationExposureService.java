package committee.nova.mods.magneticraft.system.nuclear.radiation;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.nuclear.fuel.FuelAssemblyItem;
import committee.nova.mods.magneticraft.content.nuclear.radiation.RadiationProtectionItem;
import committee.nova.mods.magneticraft.content.nuclear.radiation.SealedSpentFuelCaskItem;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.entity.player.PlayerEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Server-authoritative exposure integration over visible loaded sources and carried material. */
@Mod.EventBusSubscriber(modid = Magneticraft.MOD_ID, bus = Mod.EventBusSubscriber.Bus.FORGE)
public final class RadiationExposureService {
    private static final String DATA_KEY = "magneticraft_radiation";
    private static final double HOURS_PER_SAMPLE = 1.0D / 3_600.0D;

    private RadiationExposureService() {
    }

    @SubscribeEvent
    public static void onPlayerTick(TickEvent.PlayerTickEvent event) {
        if (event.phase != TickEvent.Phase.END || !(event.player instanceof ServerPlayer player)
                || player.tickCount % 20 != 0) {
            return;
        }
        RadiationReading reading = reading(player);
        RadiationExposure next = exposure(player).accumulate(reading, HOURS_PER_SAMPLE, protection(player));
        write(player, next);
        applyHealthEffects(player, next);
    }

    @SubscribeEvent
    public static void onClone(PlayerEvent.Clone event) {
        CompoundTag old = event.getOriginal().getPersistentData();
        if (old.contains(DATA_KEY)) {
            event.getEntity().getPersistentData().put(DATA_KEY, old.getCompound(DATA_KEY).copy());
        }
    }

    public static RadiationExposure exposure(Player player) {
        return RadiationExposure.load(player.getPersistentData().getCompound(DATA_KEY))
                .orElse(RadiationExposure.ZERO);
    }

    public static boolean decontaminate(Player player) {
        RadiationExposure current = exposure(player);
        if (current.contaminationMillisieverts() <= 0.0D) {
            return false;
        }
        write(player, current.decontaminated());
        return true;
    }

    public static RadiationReading reading(ServerPlayer player) {
        return carriedReading(player).add(worldReading(player));
    }

    private static RadiationReading worldReading(ServerPlayer player) {
        ServerLevel level = player.serverLevel();
        Vec3 target = player.getEyePosition();
        RadiationReading total = RadiationReading.ZERO;
        for (RadiationSource source : RadiationSourceRegistry.nearby(level, player.blockPosition(), 32.0D)) {
            Vec3 origin = Vec3.atCenterOf(source.radiationOrigin());
            double distance = Math.max(1.0D, origin.distanceTo(target));
            if (distance > source.maximumRangeBlocks()) {
                continue;
            }
            double dose = source.doseRateMillisievertsPerHour() / (distance * distance)
                    * RadiationShielding.attenuation(level, origin, target);
            total = total.add(new RadiationReading(dose, source.contaminationSource() ? dose : 0.0D));
        }
        return total;
    }

    private static RadiationReading carriedReading(ServerPlayer player) {
        double dose = 0.0D;
        double contamination = 0.0D;
        for (ItemStack stack : player.getInventory().items) {
            RadiationReading item = itemReading(stack);
            dose += item.doseRateMillisievertsPerHour();
            contamination += item.contaminationRateMillisievertsPerHour();
        }
        for (ItemStack stack : player.getInventory().offhand) {
            RadiationReading item = itemReading(stack);
            dose += item.doseRateMillisievertsPerHour();
            contamination += item.contaminationRateMillisievertsPerHour();
        }
        return new RadiationReading(dose, contamination);
    }

    public static RadiationReading itemReading(ItemStack stack) {
        var parameters = ReactorParameterRegistry.INSTANCE.current().parameters();
        if (stack.getItem() instanceof FuelAssemblyItem fuel) {
            return fuel.state(stack).map(state -> {
                double dose = parameters.freshFuelDoseRateMillisievertsPerHour()
                        + parameters.spentFuelDoseRateMillisievertsPerHour() * state.burnupFraction()
                        * (1.0D + state.decayHeatJoules() / 10.0D);
                double contamination = state.claddingIntegrity() < parameters.claddingAccidentThreshold()
                        ? dose * (1.0D - state.claddingIntegrity()) : 0.0D;
                return new RadiationReading(dose, contamination);
            }).orElse(RadiationReading.ZERO);
        }
        if (stack.getItem() instanceof SealedSpentFuelCaskItem cask) {
            return cask.fuel(stack).map(state -> new RadiationReading(
                    parameters.freshFuelDoseRateMillisievertsPerHour()
                            + parameters.spentFuelDoseRateMillisievertsPerHour()
                            * state.burnupFraction() * 0.01D, 0.0D)).orElse(RadiationReading.ZERO);
        }
        if (stack.is(ModFluids.get(FluidDefinition.HOT_REACTOR_COOLANT).bucket().get())) {
            return new RadiationReading(parameters.hotCoolantDoseRateMillisievertsPerHour(), 0.0D);
        }
        return RadiationReading.ZERO;
    }

    private static double protection(Player player) {
        double remaining = 1.0D;
        for (ItemStack armor : player.getArmorSlots()) {
            if (armor.getItem() instanceof RadiationProtectionItem protection) {
                remaining *= 1.0D - protection.protectionFraction();
            }
        }
        return 1.0D - remaining;
    }

    private static void applyHealthEffects(ServerPlayer player, RadiationExposure exposure) {
        double dose = exposure.cumulativeDoseMillisieverts();
        if (dose >= 100.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 60, dose >= 500.0D ? 1 : 0, true, true));
        }
        if (dose >= 250.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.CONFUSION, 80, 0, true, true));
        }
        if (dose >= 500.0D) {
            player.addEffect(new MobEffectInstance(MobEffects.POISON, 60, 0, true, true));
        }
        if (dose >= 1_000.0D && player.tickCount % 100 == 0) {
            player.hurt(player.damageSources().magic(), 1.0F);
        }
    }

    private static void write(Player player, RadiationExposure exposure) {
        player.getPersistentData().put(DATA_KEY, exposure.save());
    }
}
