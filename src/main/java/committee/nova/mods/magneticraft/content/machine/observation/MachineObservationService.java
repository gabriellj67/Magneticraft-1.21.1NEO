package committee.nova.mods.magneticraft.content.machine.observation;

import committee.nova.mods.magneticraft.content.computer.MiningRobotBlockEntity;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceProcessModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.EnergyStorageModule;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Collects the explicit public subset of machine state used by read-only overlays. */
public final class MachineObservationService {
    private MachineObservationService() {
    }

    public static MachineObservation observe(BlockEntity blockEntity, @Nullable Direction side) {
        Optional<MachineObservation.ProcessStatus> process = Optional.empty();
        Optional<MachineObservation.EnergyStatus> energy = Optional.empty();
        Optional<MachineObservation.StructureStatus> structure = Optional.empty();
        List<MachineObservation.TankStatus> tanks = new ArrayList<>();

        if (blockEntity instanceof ElectricFurnaceBlockEntity furnace) {
            process = Optional.of(new MachineObservation.ProcessStatus(
                    furnace.process().progressUnits(),
                    ElectricFurnaceProcessModule.TOTAL_PROGRESS_UNITS,
                    furnace.process().working()
            ));
            energy = energy(furnace.energy());
        } else if (blockEntity instanceof SingleBlockMachineBlockEntity machine) {
            if (machine.totalProgress() > 0 || machine.working()) {
                process = Optional.of(new MachineObservation.ProcessStatus(
                        machine.progress(), machine.totalProgress(), machine.working()
                ));
            }
            energy = energy(machine.energy());
            addTank(tanks, machine.primaryTank());
            addTank(tanks, machine.secondaryTank());
        } else if (blockEntity instanceof AdvancedMultiblockBlockEntity controller) {
            if (controller.totalProgress() > 0 || controller.working()) {
                process = Optional.of(new MachineObservation.ProcessStatus(
                        controller.progress(), controller.totalProgress(), controller.working()
                ));
            }
            energy = energy(controller.energy());
            for (int index = 0; index < Math.min(controller.definition().tankCount(), MachineObservation.MAX_TANKS); index++) {
                addTank(tanks, controller.tank(index));
            }
            structure = Optional.of(new MachineObservation.StructureStatus(
                    controller.formed(), controller.operational()
            ));
        } else if (blockEntity instanceof BatteryBlockEntity battery) {
            energy = energy(battery.energy());
        } else if (blockEntity instanceof WindTurbineBlockEntity turbine) {
            energy = energy(turbine.energy());
        } else if (blockEntity instanceof MiningRobotBlockEntity robot) {
            energy = energy(robot.energy());
        }

        Optional<MachineObservation.ElectricalStatus> electrical = Optional.empty();
        Optional<MachineObservation.ThermalStatus> thermal = Optional.empty();
        if (blockEntity instanceof DiagnosticHost diagnostics) {
            Direction observedSide = side == null ? Direction.UP : side;
            electrical = diagnostics.electricalReading(observedSide).map(reading ->
                    new MachineObservation.ElectricalStatus(
                            reading.tierId(),
                            reading.terminalId(),
                            reading.voltageVolts(),
                            reading.chargeCoulombsPerTick(),
                            reading.currentAmps(),
                            reading.joulesPerTick(),
                            reading.powerWatts(),
                            reading.storedJoules(),
                            reading.capacityJoules(),
                            reading.loadRatio(),
                            reading.thermalStress(),
                            reading.flowDirection(),
                            reading.faultKind()
                    )
            );
            thermal = diagnostics.thermalReading(observedSide).map(reading ->
                    new MachineObservation.ThermalStatus(reading.temperatureKelvin())
            );
        }

        return new MachineObservation(process, energy, electrical, thermal, tanks, structure);
    }

    private static Optional<MachineObservation.EnergyStatus> energy(@Nullable EnergyStorageModule energy) {
        return energy == null
                ? Optional.empty()
                : Optional.of(new MachineObservation.EnergyStatus(
                        energy.getEnergyStored(), energy.getMaxEnergyStored()
                ));
    }

    private static void addTank(List<MachineObservation.TankStatus> tanks, @Nullable FluidTankModule module) {
        if (module == null || tanks.size() >= MachineObservation.MAX_TANKS) {
            return;
        }
        FluidStack fluid = module.tank().getFluid();
        Optional<ResourceLocation> fluidId = fluid.isEmpty()
                ? Optional.empty()
                : Optional.ofNullable(ForgeRegistries.FLUIDS.getKey(fluid.getFluid()));
        tanks.add(new MachineObservation.TankStatus(
                fluidId,
                module.tank().getFluidAmount(),
                module.tank().getCapacity()
        ));
    }
}
