package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPartRole;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorColumnBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalFacilityType;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.thermal.MainCoolantPumpBlock;
import committee.nova.mods.magneticraft.content.nuclear.radiation.RadioactiveSourceBlock;
import committee.nova.mods.magneticraft.content.nuclear.radiation.RadioactiveSourceKind;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.structure.NuclearStructurePartBlock;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Blocks shared by the compressed nuclear fuel front end. */
public final class ModNuclearBlocks {
    private static final List<DeferredHolder<Item, ? extends Item>> BLOCK_ITEMS = new ArrayList<>();
    private static final Map<NuclearFacilityType, DeferredHolder<Block, Block>> CONTROLLERS =
            new EnumMap<>(NuclearFacilityType.class);
    private static final Map<NuclearReactorColumnType, DeferredHolder<Block, Block>> REACTOR_COLUMNS =
            new EnumMap<>(NuclearReactorColumnType.class);
    private static final Map<NuclearThermalFacilityType, DeferredHolder<Block, Block>> THERMAL_CONTROLLERS =
            new EnumMap<>(NuclearThermalFacilityType.class);

    public static final DeferredHolder<Block, Block> FACILITY_CASING = register(
            "nuclear_facility_casing", () -> new NuclearStructurePartBlock(partProperties().noOcclusion()));
    public static final DeferredHolder<Block, Block> PROCESS_CORE = register(
            "nuclear_process_core", () -> new Block(partProperties()));
    public static final DeferredHolder<Block, Block> CENTRIFUGE_STAGE = register(
            "centrifuge_stage", () -> new Block(partProperties()));
    public static final DeferredHolder<Block, Block> ITEM_INPUT_PORT = register(
            "nuclear_item_input_port",
            () -> new NuclearFacilityPortBlock(NuclearFacilityPartRole.ITEM_INPUT, partProperties()));
    public static final DeferredHolder<Block, Block> ITEM_OUTPUT_PORT = register(
            "nuclear_item_output_port",
            () -> new NuclearFacilityPortBlock(NuclearFacilityPartRole.ITEM_OUTPUT, partProperties()));
    public static final DeferredHolder<Block, Block> ELECTRICAL_PORT = register(
            "nuclear_electrical_port",
            () -> new NuclearFacilityPortBlock(NuclearFacilityPartRole.ELECTRICAL, controllerProperties()));
    public static final DeferredHolder<Block, Block> REACTOR_CONTAINMENT_CASING = register(
            "reactor_containment_casing", () -> new NuclearStructurePartBlock(partProperties().noOcclusion()));
    public static final DeferredHolder<Block, Block> REACTOR_PRESSURE_VESSEL = register(
            "reactor_pressure_vessel", () -> new NuclearStructurePartBlock(partProperties().noOcclusion()));
    public static final DeferredHolder<Block, Block> REACTOR_CONTROL_ROD_ACTUATOR = register(
            "reactor_control_rod_actuator", () -> new NuclearStructurePartBlock(controllerProperties()));
    public static final DeferredHolder<Block, Block> REACTOR_COLUMN_SEGMENT = register(
            "reactor_column_segment", () -> new NuclearStructurePartBlock(partProperties().noOcclusion()));
    public static final DeferredHolder<Block, Block> REACTOR_MAIN_COOLANT_PORT = register(
            "reactor_main_coolant_port",
            () -> new NuclearReactorPortBlock(NuclearReactorPortType.COOLANT_INPUT, controllerProperties()));
    public static final DeferredHolder<Block, Block> REACTOR_ELECTRICAL_PORT = register(
            "reactor_electrical_port",
            () -> new NuclearReactorPortBlock(NuclearReactorPortType.ELECTRICAL, controllerProperties()));
    public static final DeferredHolder<Block, Block> REACTOR_INSTRUMENTATION_PORT = register(
            "reactor_instrumentation_port",
            () -> new NuclearReactorPortBlock(NuclearReactorPortType.INSTRUMENTATION, controllerProperties()));
    public static final DeferredHolder<Block, Block> REACTOR_CONTROLLER = register(
            "pressurized_water_reactor_controller",
            () -> new NuclearReactorControllerBlock(controllerProperties()));
    public static final DeferredHolder<Block, Block> NUCLEAR_THERMAL_PORT = register(
            "nuclear_thermal_port", () -> new NuclearThermalPortBlock(controllerProperties()));
    public static final DeferredHolder<Block, Block> NUCLEAR_HEAT_EXCHANGER = register(
            "nuclear_heat_exchanger", () -> new Block(partProperties()));
    public static final DeferredHolder<Block, Block> COOLING_TOWER_FILL = register(
            "cooling_tower_fill", () -> new Block(partProperties().noOcclusion()));
    public static final DeferredHolder<Block, Block> COOLING_TOWER_FAN = register(
            "cooling_tower_fan", () -> new Block(controllerProperties()));
    public static final DeferredHolder<Block, Block> MAIN_COOLANT_PUMP = register(
            "main_coolant_pump", () -> new MainCoolantPumpBlock(controllerProperties()));
    public static final DeferredHolder<Block, Block> LEAD_RADIATION_SHIELD = register(
            "lead_radiation_shield", () -> new Block(partProperties().strength(8.0F, 30.0F)));
    public static final DeferredHolder<Block, Block> RADIOACTIVE_DEBRIS = register(
            "radioactive_debris",
            () -> new RadioactiveSourceBlock(RadioactiveSourceKind.DEBRIS,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_GREEN)
                            .strength(0.5F).sound(SoundType.GRAVEL).lightLevel(state -> 3)));
    public static final DeferredHolder<Block, Block> CORIUM = register(
            "corium",
            () -> new RadioactiveSourceBlock(RadioactiveSourceKind.CORIUM,
                    BlockBehaviour.Properties.of().mapColor(MapColor.COLOR_ORANGE)
                            .strength(6.0F, 20.0F).sound(SoundType.BASALT).lightLevel(state -> 9)));
    public static final DeferredHolder<Block, Block> SPENT_FUEL_POOL_CONTROLLER = register(
            "spent_fuel_pool_controller", () -> new SpentFuelPoolControllerBlock(controllerProperties()));
    public static final DeferredHolder<Block, Block> SPENT_FUEL_POOL_PORT = register(
            "spent_fuel_pool_port", () -> new SpentFuelPoolPortBlock(controllerProperties()));

    static {
        for (NuclearFacilityType type : NuclearFacilityType.values()) {
            DeferredHolder<Block, Block> block = register(
                    type.id(),
                    () -> new NuclearFacilityControllerBlock(type, controllerProperties())
            );
            CONTROLLERS.put(type, block);
        }
        for (NuclearReactorColumnType type : NuclearReactorColumnType.values()) {
            REACTOR_COLUMNS.put(type, register(
                    "reactor_" + type.name().toLowerCase(java.util.Locale.ROOT),
                    () -> new NuclearReactorColumnBlock(type, partProperties())
            ));
        }
        for (NuclearThermalFacilityType type : NuclearThermalFacilityType.values()) {
            THERMAL_CONTROLLERS.put(type, register(
                    type.id(), () -> new NuclearThermalControllerBlock(type, controllerProperties())
            ));
        }
    }

    private ModNuclearBlocks() {
    }

    public static void bootstrap() {
    }

    public static DeferredHolder<Block, Block> controller(NuclearFacilityType type) {
        DeferredHolder<Block, Block> result = CONTROLLERS.get(type);
        if (result == null) {
            throw new IllegalArgumentException("No nuclear facility controller registered for " + type);
        }
        return result;
    }

    public static Map<NuclearFacilityType, DeferredHolder<Block, Block>> controllers() {
        return Collections.unmodifiableMap(CONTROLLERS);
    }

    public static List<DeferredHolder<Item, ? extends Item>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    public static DeferredHolder<Block, Block> reactorColumn(NuclearReactorColumnType type) {
        DeferredHolder<Block, Block> result = REACTOR_COLUMNS.get(type);
        if (result == null) {
            throw new IllegalArgumentException("No reactor column block registered for " + type);
        }
        return result;
    }

    public static Map<NuclearReactorColumnType, DeferredHolder<Block, Block>> reactorColumns() {
        return Collections.unmodifiableMap(REACTOR_COLUMNS);
    }

    public static DeferredHolder<Block, Block> thermalController(NuclearThermalFacilityType type) {
        DeferredHolder<Block, Block> result = THERMAL_CONTROLLERS.get(type);
        if (result == null) {
            throw new IllegalArgumentException("No nuclear thermal controller registered for " + type);
        }
        return result;
    }

    public static Map<NuclearThermalFacilityType, DeferredHolder<Block, Block>> thermalControllers() {
        return Collections.unmodifiableMap(THERMAL_CONTROLLERS);
    }

    private static DeferredHolder<Block, Block> register(String id, Supplier<? extends Block> factory) {
        DeferredHolder<Block, Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    private static BlockBehaviour.Properties partProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(4.0F, 12.0F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties controllerProperties() {
        return partProperties().noOcclusion();
    }
}
