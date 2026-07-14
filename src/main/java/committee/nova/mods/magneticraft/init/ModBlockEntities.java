package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingTableBlockEntity;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceBlockEntity;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockBlockEntity;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.worldgen.OilDepositBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricConnectorBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.WirelessEnergyReceiverBlockEntity;
import committee.nova.mods.magneticraft.content.network.fluid.IronPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatSinkBlockEntity;
import committee.nova.mods.magneticraft.content.network.logistics.ConveyorBeltBlockEntity;
import committee.nova.mods.magneticraft.content.network.pneumatic.PneumaticTubeBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Machine block entity type registrations.
 */
public final class ModBlockEntities {
    private static final Map<SingleBlockMachineDefinition, RegistryObject<BlockEntityType<SingleBlockMachineBlockEntity>>>
            SINGLE_BLOCK_MACHINES = new EnumMap<>(SingleBlockMachineDefinition.class);
    private static final Map<MultiblockDefinition, RegistryObject<BlockEntityType<AdvancedMultiblockBlockEntity>>>
            ADVANCED_MULTIBLOCKS = new EnumMap<>(MultiblockDefinition.class);

    public static final RegistryObject<BlockEntityType<CrushingTableBlockEntity>> CRUSHING_TABLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "crushing_table",
                    () -> BlockEntityType.Builder.of(
                            CrushingTableBlockEntity::new,
                            ModMachineBlocks.CRUSHING_TABLE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<BatteryBlockEntity>> BATTERY =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "battery_box",
                    () -> BlockEntityType.Builder.of(
                            BatteryBlockEntity::new,
                            ModMachineBlocks.BATTERY.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ElectricFurnaceBlockEntity>> ELECTRIC_FURNACE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_furnace",
                    () -> BlockEntityType.Builder.of(
                            ElectricFurnaceBlockEntity::new,
                            ModMachineBlocks.ELECTRIC_FURNACE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ElectricCableBlockEntity>> ELECTRIC_CABLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_cable",
                    () -> BlockEntityType.Builder.of(
                            ElectricCableBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_CABLE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ElectricConnectorBlockEntity>> ELECTRIC_CONNECTOR =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_connector",
                    () -> BlockEntityType.Builder.of(
                            ElectricConnectorBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_CONNECTOR.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ElectricPoleBlockEntity>> ELECTRIC_POLE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_pole",
                    () -> BlockEntityType.Builder.of(
                            ElectricPoleBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_POLE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ElectricPoleBlockEntity>> ELECTRIC_POLE_TRANSFORMER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "electric_pole_transformer",
                    () -> BlockEntityType.Builder.of(
                            ElectricPoleBlockEntity::new,
                            ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<TeslaTowerBlockEntity>> TESLA_TOWER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "tesla_tower",
                    () -> BlockEntityType.Builder.of(
                            TeslaTowerBlockEntity::new,
                            ModNetworkBlocks.TESLA_TOWER.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<WirelessEnergyReceiverBlockEntity>> WIRELESS_ENERGY_RECEIVER =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "wireless_energy_receiver",
                    () -> BlockEntityType.Builder.of(
                            WirelessEnergyReceiverBlockEntity::new,
                            ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<WindTurbineBlockEntity>> WIND_TURBINE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "wind_turbine",
                    () -> BlockEntityType.Builder.of(
                            WindTurbineBlockEntity::new,
                            ModNetworkBlocks.WIND_TURBINE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<HeatPipeBlockEntity>> HEAT_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "heat_pipe",
                    () -> BlockEntityType.Builder.of(
                            HeatPipeBlockEntity::new,
                            ModNetworkBlocks.HEAT_PIPE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<HeatPipeBlockEntity>> INSULATED_HEAT_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "insulated_heat_pipe",
                    () -> BlockEntityType.Builder.of(
                            HeatPipeBlockEntity::new,
                            ModNetworkBlocks.INSULATED_HEAT_PIPE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<HeatSinkBlockEntity>> HEAT_SINK =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "heat_sink",
                    () -> BlockEntityType.Builder.of(
                            HeatSinkBlockEntity::new,
                            ModNetworkBlocks.HEAT_SINK.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<IronPipeBlockEntity>> IRON_PIPE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "iron_fluid_pipe",
                    () -> BlockEntityType.Builder.of(
                            IronPipeBlockEntity::new,
                            ModNetworkBlocks.IRON_PIPE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<PneumaticTubeBlockEntity>> PNEUMATIC_TUBE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "pneumatic_tube",
                    () -> BlockEntityType.Builder.of(
                            PneumaticTubeBlockEntity::new,
                            ModNetworkBlocks.PNEUMATIC_TUBE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<PneumaticTubeBlockEntity>> PNEUMATIC_RESTRICTION_TUBE =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "pneumatic_restriction_tube",
                    () -> BlockEntityType.Builder.of(
                            PneumaticTubeBlockEntity::new,
                            ModNetworkBlocks.PNEUMATIC_RESTRICTION_TUBE.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<ConveyorBeltBlockEntity>> CONVEYOR_BELT =
            ModRegistries.BLOCK_ENTITY_TYPES.register(
                    "conveyor_belt",
                    () -> BlockEntityType.Builder.of(
                            ConveyorBeltBlockEntity::new,
                            ModNetworkBlocks.CONVEYOR_BELT.get()
                    ).build(null)
            );
    public static final RegistryObject<BlockEntityType<OilDepositBlockEntity>> OIL_DEPOSIT =
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

    public static RegistryObject<BlockEntityType<SingleBlockMachineBlockEntity>> singleBlockMachine(
            SingleBlockMachineDefinition definition
    ) {
        RegistryObject<BlockEntityType<SingleBlockMachineBlockEntity>> type = SINGLE_BLOCK_MACHINES.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No block entity type registered for " + definition);
        }
        return type;
    }

    public static Map<SingleBlockMachineDefinition, RegistryObject<BlockEntityType<SingleBlockMachineBlockEntity>>>
    singleBlockMachines() {
        return Collections.unmodifiableMap(SINGLE_BLOCK_MACHINES);
    }

    public static RegistryObject<BlockEntityType<AdvancedMultiblockBlockEntity>> advancedMultiblock(
            MultiblockDefinition definition
    ) {
        RegistryObject<BlockEntityType<AdvancedMultiblockBlockEntity>> type = ADVANCED_MULTIBLOCKS.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No block entity type registered for " + definition);
        }
        return type;
    }

    public static Map<MultiblockDefinition, RegistryObject<BlockEntityType<AdvancedMultiblockBlockEntity>>>
    advancedMultiblocks() {
        return Collections.unmodifiableMap(ADVANCED_MULTIBLOCKS);
    }
}
