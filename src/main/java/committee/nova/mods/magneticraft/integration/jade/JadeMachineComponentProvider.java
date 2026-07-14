package committee.nova.mods.magneticraft.integration.jade;

import committee.nova.mods.magneticraft.Magneticraft;
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
                "tooltip.magneticraft.energy", energy.stored(), energy.capacity()
        )));
        observation.electrical().ifPresent(electrical -> tooltip.add(Component.translatable(
                "message.magneticraft.voltmeter",
                decimal(electrical.voltageVolts()),
                decimal(electrical.currentAmps()),
                decimal(electrical.powerWatts())
        )));
        observation.thermal().ifPresent(thermal -> tooltip.add(Component.translatable(
                "tooltip.magneticraft.jade.temperature",
                decimal(thermal.temperatureKelvin())
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

    private static String decimal(double value) {
        return String.format(Locale.ROOT, "%.2f", value);
    }
}
