package committee.nova.mods.magneticraft.integration.jade;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.content.machine.observation.MachineObservation;
import committee.nova.mods.magneticraft.content.machine.observation.MachineObservationCodec;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IBlockComponentProvider;
import snownee.jade.api.ITooltip;
import snownee.jade.api.config.IPluginConfig;

import java.util.Locale;

enum JadeMachineComponentProvider implements IBlockComponentProvider {
    INSTANCE;

    private static final ResourceLocation UID = Magneticraft.id("machine_status");

    @Override
    public void appendTooltip(ITooltip tooltip, BlockAccessor accessor, IPluginConfig config) {
        MachineObservationCodec.read(accessor.getServerData()).ifPresent(observation -> append(tooltip, observation));
        NuclearJadeData.read(accessor.getServerData()).ifPresent(reading -> appendNuclear(tooltip, reading));
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }

    private static void append(ITooltip tooltip, MachineObservation observation) {
        observation.process().ifPresent(process -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.process",
                process.progress(),
                process.total(),
                Component.translatable(process.working()
                        ? "tooltip.magneticraft.jade.active"
                        : "tooltip.magneticraft.jade.idle")
        )));
        observation.energy().ifPresent(energy -> tooltip.add(Component.translatable(
                energy.unit().tooltipTranslationKey(), energy.stored(), energy.capacity()
        )));
        observation.electrical().ifPresent(electrical -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.electrical_terminal",
                tierName(electrical.tierId()),
                electrical.terminalId()
        )));
        observation.electrical().ifPresent(electrical -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.electrical_flow",
                decimal(electrical.voltageVolts()),
                decimal(electrical.chargeCoulombsPerTick()),
                decimal(electrical.currentAmps()),
                decimal(electrical.joulesPerTick()),
                decimal(electrical.powerWatts())
        )));
        observation.electrical().ifPresent(electrical -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.electrical_state",
                percent(electrical.loadRatio()),
                percent(electrical.thermalStress()),
                Component.translatable(electrical.flowDirection().translationKey()),
                Component.translatable(electrical.faultKind().translationKey())
        )));
        observation.thermal().ifPresent(thermal -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.temperature",
                decimal(thermal.temperatureKelvin())
        )));
        observation.pressure().ifPresent(pressure -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.pressure",
                pressure.gasId().map(ResourceLocation::toString).orElseGet(() ->
                        Component.translatable("tooltip.magneticraft.jade.empty").getString()),
                decimal(pressure.pressureKpa()),
                decimal(pressure.gasKpaLiters()),
                decimal(pressure.capacityKpaLiters())
        )));
        observation.pressure().filter(MachineObservation.PressureStatus::warning)
                .ifPresent(pressure -> tooltip.add(Component.translatable(
                        "tooltip.magneticraft.jade.pressure_warning",
                        percent(pressure.fillRatio())
                )));
        for (MachineObservation.TankStatus tank : observation.tanks()) {
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft.jade.tank",
                    tank.fluidId().map(ResourceLocation::toString).orElseGet(() ->
                            Component.translatable("tooltip.magneticraft.jade.empty").getString()),
                    tank.amount(),
                    tank.capacity()
            ));
        }
        observation.structure().ifPresent(structure -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.structure",
                Component.translatable(structure.formed()
                        ? "tooltip.magneticraft.jade.formed"
                        : "tooltip.magneticraft.jade.unformed"),
                Component.translatable(structure.operational()
                        ? "tooltip.magneticraft.jade.operational"
                        : "tooltip.magneticraft.jade.paused")
        )));
    }

    private static void appendNuclear(ITooltip tooltip, NuclearJadeData.Reading reading) {
        tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.nuclear_dose_rate", decimal(reading.doseRate())));
        tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.nuclear_contamination",
                Component.translatable(reading.contaminationSource()
                        ? "message.magneticraft.contamination.present"
                        : "message.magneticraft.contamination.absent")));
        if (reading.formed() != null) {
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft.jade.nuclear_structure",
                    Component.translatable(reading.formed()
                            ? "tooltip.magneticraft.jade.formed"
                            : "tooltip.magneticraft.jade.unformed")));
        }
        if (reading.accidentStage() != null) {
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft.jade.nuclear_accident",
                    Component.translatable("gui.magneticraft.reactor.accident."
                            + reading.accidentStage().name().toLowerCase(Locale.ROOT))));
            tooltip.add(Component.translatable(
                    "tooltip.magneticraft.jade.nuclear_barriers",
                    decimal(reading.pressureMegapascals()),
                    percent(reading.vesselIntegrity()),
                    percent(reading.containmentIntegrity())));
        }
    }

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }

    private static String percent(double ratio) {
        return String.format(Locale.ROOT, "%.1f%%", ratio * 100.0D);
    }

    private static Component tierName(ResourceLocation tierId) {
        return ClientVoltageTierRegistry.current().tier(tierId)
                .<Component>map(tier -> Component.translatable(tier.translationKey()))
                .orElseGet(() -> Component.literal(tierId.toString()));
    }
}
