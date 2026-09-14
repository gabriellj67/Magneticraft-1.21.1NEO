package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlockEntity;
import committee.nova.mods.magneticraft.content.network.kinetic.HandCrankBlockEntity;
import committee.nova.mods.magneticraft.content.network.kinetic.WoodenShaftBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.MachineCapabilities;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockGapBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPortBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorPortBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalPortBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.thermal.MainCoolantPumpBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.radiation.RadioactiveSourceBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolPortBlockEntity;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.BoxTransformerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricConnectorBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalControlBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.WirelessEnergyReceiverBlockEntity;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlockEntity;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import committee.nova.mods.magneticraft.content.network.pressure.BrassPressurePipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.pressure.PressureTankBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Machine block entity type registrations.
 */
public final class ModBlockEntities {
    private static final Map<SingleBlockMachineDefinition, DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleBlockMachineBlockEntity>>>
            SINGLE_BLOCK_MACHINES = new EnumMap<>(SingleBlockMachineDefinition.class);
    private static final Map<MultiblockDefinition, DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedMultiblockBlockEntity>>>
            ADVANCED_MULTIBLOCKS = new EnumMap<>(MultiblockDefinition.class);
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearFacilityControllerBlockEntity>>
            NUCLEAR_FACILITY_CONTROLLER = ModRegistries.BLOCK_ENTITY_TYPES.register(
            "nuclear_facility_controller",
            () -> BlockEntityType.Builder.of(
                    NuclearFacilityControllerBlockEntity::new,
                    ModNuclearBlocks.controllers().values().stream()
                            .map(DeferredHolder::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            ).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearFacilityPortBlockEntity>>
            NUCLEAR_FACILITY_PORT = ModRegistries.BLOCK_ENTITY_TYPES.register(
            "nuclear_facility_port",
            () -> BlockEntityType.Builder.of(
                    NuclearFacilityPortBlockEntity::new,
                    ModNuclearBlocks.ITEM_INPUT_PORT.get(),
                    ModNuclearBlocks.ITEM_OUTPUT_PORT.get(),
                    ModNuclearBlocks.ELECTRICAL_PORT.get()
            ).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearReactorControllerBlockEntity>>
            NUCLEAR_REACTOR_CONTROLLER = ModRegistries.BLOCK_ENTITY_TYPES.register(
            "pressurized_water_reactor_controller",
            () -> BlockEntityType.Builder.of(
                    NuclearReactorControllerBlockEntity::new,
                    ModNuclearBlocks.REACTOR_CONTROLLER.get()
            ).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearReactorPortBlockEntity>>
            NUCLEAR_REACTOR_PORT = ModRegistries.BLOCK_ENTITY_TYPES.register(
            "nuclear_reactor_port",
            () -> BlockEntityType.Builder.of(
                    NuclearReactorPortBlockEntity::new,
                    ModNuclearBlocks.REACTOR_MAIN_COOLANT_PORT.get(),
                    ModNuclearBlocks.REACTOR_ELECTRICAL_PORT.get(),
                    ModNuclearBlocks.REACTOR_INSTRUMENTATION_PORT.get()
            ).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearThermalControllerBlockEntity>>
            NUCLEAR_THERMAL_CONTROLLER = ModRegistries.BLOCK_ENTITY_TYPES.register(
            "nuclear_thermal_controller",
            () -> BlockEntityType.Builder.of(
                    NuclearThermalControllerBlockEntity::new,
                    ModNuclearBlocks.thermalControllers().values().stream()
                            .map(DeferredHolder::get)
                            .toArray(net.minecraft.world.level.block.Block[]::new)
            ).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<NuclearThermalPortBlockEntity>>
            NUCLEAR_THERMAL_PORT = ModRegistries.BLOCK_ENTITY_TYPES.register(
            "nuclear_thermal_port",
            () -> BlockEntityType.Builder.of(
                    NuclearThermalPortBlockEntity::new,
                    ModNuclearBlocks.NUCLEAR_THERMAL_PORT.get()
            ).build(null)
    );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MainCoolantPumpBlockEntity>> MAIN_COOLANT_PUMP =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "main_coolant_pump",
                    () -> BlockEntityType.Builder.of(
                            MainCoolantPumpBlockEntity::new,
                            ModNuclearBlocks.MAIN_COOLANT_PUMP.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<RadioactiveSourceBlockEntity>> RADIOACTIVE_SOURCE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "radioactive_source",
                    () -> BlockEntityType.Builder.of(
                            RadioactiveSourceBlockEntity::new,
                            ModNuclearBlocks.RADIOACTIVE_DEBRIS.get(),
                            ModNuclearBlocks.CORIUM.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpentFuelPoolControllerBlockEntity>> SPENT_FUEL_POOL_CONTROLLER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "spent_fuel_pool_controller",
                    () -> BlockEntityType.Builder.of(
                            SpentFuelPoolControllerBlockEntity::new,
                            ModNuclearBlocks.SPENT_FUEL_POOL_CONTROLLER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<SpentFuelPoolPortBlockEntity>> SPENT_FUEL_POOL_PORT =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "spent_fuel_pool_port",
                    () -> BlockEntityType.Builder.of(
                            SpentFuelPoolPortBlockEntity::new,
                            ModNuclearBlocks.SPENT_FUEL_POOL_PORT.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<MultiblockGapBlockEntity>> MULTIBLOCK_GAP =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "multiblock_gap",
                    () -> BlockEntityType.Builder.of(
                            MultiblockGapBlockEntity::new,
                            ModAdvancedBlocks.MULTIBLOCK_GAP.get()
                    ).build(null)
            );

    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<CrushingTableBlockEntity>> CRUSHING_TABLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "crushing_table",
                    () -> BlockEntityType.Builder.of(
                            CrushingTableBlockEntity::new,
                            ModMachineBlocks.CRUSHING_TABLE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BatteryBlockEntity>> BATTERY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "battery_box",
                    () -> BlockEntityType.Builder.of(
                            BatteryBlockEntity::new,
                            ModMachineBlocks.BATTERY.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_furnace",
                    () -> BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            ModMachineBlocks.ELECTRIC_FURNACE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricCableBlockEntity>> ELECTRIC_CABLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_cable",
                    () -> BlockEntityType.Builder.of(
                            ElectricCableBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_CABLE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricConnectorBlockEntity>> ELECTRIC_CONNECTOR =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_connector",
                    () -> BlockEntityType.Builder.of(
                            ElectricConnectorBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_CONNECTOR.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPoleBlockEntity>> ELECTRIC_POLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_pole",
                    () -> BlockEntityType.Builder.of(
                            ElectricPoleBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_POLE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricPoleBlockEntity>> ELECTRIC_POLE_TRANSFORMER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_pole_transformer",
                    () -> BlockEntityType.Builder.of(
                            ElectricPoleBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BoxTransformerBlockEntity>> BOX_TRANSFORMER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "box_transformer",
                    () -> BlockEntityType.Builder.of(
                            BoxTransformerBlockEntity::new,
                            ModNetworkBlocks.BOX_TRANSFORMER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricalProtectionBlockEntity>> FUSE_BOX =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "fuse_box",
                    () -> BlockEntityType.Builder.of(
                            ElectricalProtectionBlockEntity::new,
                            ModNetworkBlocks.FUSE_BOX.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricalProtectionBlockEntity>> CIRCUIT_BREAKER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "circuit_breaker",
                    () -> BlockEntityType.Builder.of(
                            ElectricalProtectionBlockEntity::new,
                            ModNetworkBlocks.CIRCUIT_BREAKER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ElectricalControlBlockEntity>> ELECTRICAL_CONTROL =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electrical_control",
                    () -> BlockEntityType.Builder.of(
                            ElectricalControlBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_SWITCH.get(),
                            ModNetworkBlocks.DIODE.get(),
                            ModNetworkBlocks.RESISTOR.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<TeslaTowerBlockEntity>> TESLA_TOWER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "tesla_tower",
                    () -> BlockEntityType.Builder.of(
                            TeslaTowerBlockEntity::new,
                            ModNetworkBlocks.TESLA_TOWER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WirelessEnergyReceiverBlockEntity>> WIRELESS_ENERGY_RECEIVER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "wireless_energy_receiver",
                    () -> BlockEntityType.Builder.of(
                            WirelessEnergyReceiverBlockEntity::new,
                            ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WindTurbineBlockEntity>> WIND_TURBINE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "wind_turbine",
                    () -> BlockEntityType.Builder.of(
                            WindTurbineBlockEntity::new,
                            ModNetworkBlocks.WIND_TURBINE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HandCrankBlockEntity>> HAND_CRANK =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "hand_crank",
                    () -> BlockEntityType.Builder.of(
                            HandCrankBlockEntity::new,
                            ModNetworkBlocks.HAND_CRANK.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<WoodenShaftBlockEntity>> WOODEN_SHAFT =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "wooden_shaft",
                    () -> BlockEntityType.Builder.of(
                            WoodenShaftBlockEntity::new,
                            ModNetworkBlocks.WOODEN_SHAFT.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatPipeBlockEntity>> HEAT_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "heat_pipe",
                    () -> BlockEntityType.Builder.of(
                            HeatPipeBlockEntity::new,
                            ModNetworkBlocks.HEAT_PIPE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatPipeBlockEntity>> INSULATED_HEAT_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "insulated_heat_pipe",
                    () -> BlockEntityType.Builder.of(
                            HeatPipeBlockEntity::new,
                            ModNetworkBlocks.INSULATED_HEAT_PIPE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<HeatSinkBlockEntity>> HEAT_SINK =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "heat_sink",
                    () -> BlockEntityType.Builder.of(
                            HeatSinkBlockEntity::new,
                            ModNetworkBlocks.HEAT_SINK.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<IronPipeBlockEntity>> IRON_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "iron_fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            IronPipeBlockEntity::new,
                            ModNetworkBlocks.IRON_PIPE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticTubeBlockEntity>> PNEUMATIC_TUBE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "pneumatic_tube",
                    () -> BlockEntityType.Builder.of(
                            PneumaticTubeBlockEntity::new,
                            ModNetworkBlocks.PNEUMATIC_TUBE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PneumaticTubeBlockEntity>> PNEUMATIC_RESTRICTION_TUBE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "pneumatic_restriction_tube",
                    () -> BlockEntityType.Builder.of(
                            PneumaticTubeBlockEntity::new,
                            ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<BrassPressurePipeBlockEntity>> BRASS_PRESSURE_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "brass_pressure_pipe",
                    () -> BlockEntityType.Builder.of(
                            BrassPressurePipeBlockEntity::new,
                            ModNetworkBlocks.BRASS_PRESSURE_PIPE.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<PressureTankBlockEntity>> PRESSURE_TANK =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "pressure_tank",
                    () -> BlockEntityType.Builder.of(
                            PressureTankBlockEntity::new,
                            ModNetworkBlocks.PRESSURE_TANK.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<ConveyorBeltBlockEntity>> CONVEYOR_BELT =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "conveyor_belt",
                    () -> BlockEntityType.Builder.of(
                            ConveyorBeltBlockEntity::new,
                            ModNetworkBlocks.CONVEYOR_BELT.get()
                    ).build(null)
            );
    public static final DeferredHolder<BlockEntityType<?>, BlockEntityType<OilDepositBlockEntity>> OIL_DEPOSIT =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "oil_deposit",
                    () -> BlockEntityType.Builder.of(
                            OilDepositBlockEntity::new,
                            ModAdvancedBlocks.OIL_DEPOSIT.get()
                    ).build(null)
            );

    static {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            SINGLE_BLOCK_MACHINES.put(definition, ModRegistries.BLOCK_ENTITY_TYPES.register(
                    definition.id(),
                    () -> BlockEntityType.Builder.of(
                            SingleBlockMachineBlockEntity::new,
                            ModMachineBlocks.machine(definition).get()
                    ).build(null)
            ));
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            ADVANCED_MULTIBLOCKS.put(definition, ModRegistries.BLOCK_ENTITY_TYPES.register(
                    definition.id(),
                    () -> BlockEntityType.Builder.of(
                            AdvancedMultiblockBlockEntity::new,
                            ModAdvancedBlocks.controller(definition).get()
                    ).build(null)
            ));
        }
    }

    private ModBlockEntities() {
    }

    public static void bootstrap() {
    }

    /**
     * Wires the block-capability providers for every {@code MachineBlockEntity} type registered
     * above. NeoForge requires one {@code RegisterCapabilitiesEvent} provider per (capability kind,
     * block-entity type) pair instead of Forge's single per-instance {@code getCapability} override
     * (see {@link committee.nova.mods.magneticraft.content.machine.framework.MachineCapabilities}) -
     * this method is the composition root for that wiring, called once via
     * {@code modBus.addListener(ModBlockEntities::registerCapabilities)} from
     * {@link ModRegistries#register}. Every entry here is a plain {@code MachineBlockEntity} subtype
     * except {@code MULTIBLOCK_GAP} and the advanced-multiblock controllers, which expose
     * position-dependent views documented on {@link AdvancedMultiblockBlockEntity} and
     * {@link MultiblockGapBlockEntity} themselves instead of the generic aggregator.
     */
    public static void registerCapabilities(RegisterCapabilitiesEvent event) {
        MachineCapabilities.register(event, NUCLEAR_FACILITY_CONTROLLER.get());
        MachineCapabilities.register(event, NUCLEAR_FACILITY_PORT.get());
        MachineCapabilities.register(event, NUCLEAR_REACTOR_CONTROLLER.get());
        MachineCapabilities.register(event, NUCLEAR_REACTOR_PORT.get());
        MachineCapabilities.register(event, NUCLEAR_THERMAL_CONTROLLER.get());
        MachineCapabilities.register(event, NUCLEAR_THERMAL_PORT.get());
        MachineCapabilities.register(event, MAIN_COOLANT_PUMP.get());
        MachineCapabilities.register(event, RADIOACTIVE_SOURCE.get());
        MachineCapabilities.register(event, SPENT_FUEL_POOL_CONTROLLER.get());
        MachineCapabilities.register(event, SPENT_FUEL_POOL_PORT.get());
        MachineCapabilities.register(event, CRUSHING_TABLE.get());
        MachineCapabilities.register(event, BATTERY.get());
        MachineCapabilities.register(event, ELECTRIC_FURNACE.get());
        MachineCapabilities.register(event, ELECTRIC_CABLE.get());
        MachineCapabilities.register(event, ELECTRIC_CONNECTOR.get());
        MachineCapabilities.register(event, ELECTRIC_POLE.get());
        MachineCapabilities.register(event, ELECTRIC_POLE_TRANSFORMER.get());
        MachineCapabilities.register(event, BOX_TRANSFORMER.get());
        MachineCapabilities.register(event, FUSE_BOX.get());
        MachineCapabilities.register(event, CIRCUIT_BREAKER.get());
        MachineCapabilities.register(event, ELECTRICAL_CONTROL.get());
        MachineCapabilities.register(event, TESLA_TOWER.get());
        MachineCapabilities.register(event, WIRELESS_ENERGY_RECEIVER.get());
        MachineCapabilities.register(event, WIND_TURBINE.get());
        MachineCapabilities.register(event, HAND_CRANK.get());
        MachineCapabilities.register(event, WOODEN_SHAFT.get());
        MachineCapabilities.register(event, HEAT_PIPE.get());
        MachineCapabilities.register(event, INSULATED_HEAT_PIPE.get());
        MachineCapabilities.register(event, HEAT_SINK.get());
        MachineCapabilities.register(event, IRON_PIPE.get());
        MachineCapabilities.register(event, PNEUMATIC_TUBE.get());
        MachineCapabilities.register(event, PNEUMATIC_RESTRICTION_TUBE.get());
        MachineCapabilities.register(event, BRASS_PRESSURE_PIPE.get());
        MachineCapabilities.register(event, PRESSURE_TANK.get());
        MachineCapabilities.register(event, CONVEYOR_BELT.get());
        for (DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleBlockMachineBlockEntity>> type
                : SINGLE_BLOCK_MACHINES.values()) {
            MachineCapabilities.register(event, type.get());
        }

        for (DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedMultiblockBlockEntity>> type
                : ADVANCED_MULTIBLOCKS.values()) {
            BlockEntityType<AdvancedMultiblockBlockEntity> beType = type.get();
            event.registerBlockEntity(
                    Capabilities.ItemHandler.BLOCK,
                    beType,
                    (be, side) -> be.itemHandler(be.getBlockPos(), side)
            );
            event.registerBlockEntity(
                    Capabilities.FluidHandler.BLOCK,
                    beType,
                    (be, side) -> be.fluidHandler(be.getBlockPos(), side)
            );
            event.registerBlockEntity(
                    Capabilities.EnergyStorage.BLOCK,
                    beType,
                    (be, side) -> be.energyStorage(side)
            );
        }

        event.registerBlockEntity(
                Capabilities.ItemHandler.BLOCK,
                MULTIBLOCK_GAP.get(),
                (be, side) -> be.itemHandler(side)
        );
        event.registerBlockEntity(
                Capabilities.FluidHandler.BLOCK,
                MULTIBLOCK_GAP.get(),
                (be, side) -> be.fluidHandler(side)
        );
    }

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleBlockMachineBlockEntity>> singleBlockMachine(
            SingleBlockMachineDefinition definition
    ) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleBlockMachineBlockEntity>> type = SINGLE_BLOCK_MACHINES.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No block entity type registered for " + definition);
        }
        return type;
    }

    public static Map<SingleBlockMachineDefinition, DeferredHolder<BlockEntityType<?>, BlockEntityType<SingleBlockMachineBlockEntity>>>
    singleBlockMachines() {
        return Collections.unmodifiableMap(SINGLE_BLOCK_MACHINES);
    }

    public static DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedMultiblockBlockEntity>> advancedMultiblock(
            MultiblockDefinition definition
    ) {
        DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedMultiblockBlockEntity>> type = ADVANCED_MULTIBLOCKS.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No block entity type registered for " + definition);
        }
        return type;
    }

    public static Map<MultiblockDefinition, DeferredHolder<BlockEntityType<?>, BlockEntityType<AdvancedMultiblockBlockEntity>>>
    advancedMultiblocks() {
        return Collections.unmodifiableMap(ADVANCED_MULTIBLOCKS);
    }
}
