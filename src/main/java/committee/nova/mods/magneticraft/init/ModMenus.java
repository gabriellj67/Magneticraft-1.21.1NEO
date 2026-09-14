package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineMenu;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalMenu;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolMenu;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceMenu;
import committee.nova.mods.magneticraft.content.network.pressure.PressureTankMenu;
import net.minecraft.world.inventory.MenuType;
import net.neoforged.neoforge.common.extensions.IMenuTypeExtension;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Server/client machine menu factories.
 */
public final class ModMenus {
    private static final Map<SingleBlockMachineDefinition, DeferredHolder<MenuType<?>, MenuType<SingleBlockMachineMenu>>>
            SINGLE_BLOCK_MACHINES = new EnumMap<>(SingleBlockMachineDefinition.class);
    private static final Map<MultiblockDefinition, DeferredHolder<MenuType<?>, MenuType<AdvancedMultiblockMenu>>>
            ADVANCED_MULTIBLOCKS = new EnumMap<>(MultiblockDefinition.class);

    public static final DeferredHolder<MenuType<?>, MenuType<BatteryMenu>> BATTERY =
            ModRegistries.MENU_TYPES.register("battery_box", () -> IMenuTypeExtension.create(BatteryMenu::new));
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
            ModRegistries.MENU_TYPES.register(
                    "electric_furnace",
                    () -> IMenuTypeExtension.create(ElectricFurnaceMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<ProgrammableMenu>> COMPUTER =
            ModRegistries.MENU_TYPES.register(
                    "computer",
                    () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                            new ProgrammableMenu(false, containerId, inventory, buffer))
            );
    public static final DeferredHolder<MenuType<?>, MenuType<ProgrammableMenu>> MINING_ROBOT =
            ModRegistries.MENU_TYPES.register(
                    "mining_robot",
                    () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                            new ProgrammableMenu(true, containerId, inventory, buffer))
            );
    public static final DeferredHolder<MenuType<?>, MenuType<ElectricalDeviceMenu>> ELECTRICAL_DEVICE =
            ModRegistries.MENU_TYPES.register(
                    "electrical_device",
                    () -> IMenuTypeExtension.create(ElectricalDeviceMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<PressureTankMenu>> PRESSURE_TANK =
            ModRegistries.MENU_TYPES.register(
                    "pressure_tank",
                    () -> IMenuTypeExtension.create(PressureTankMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<WindTurbineMenu>> WIND_TURBINE =
            ModRegistries.MENU_TYPES.register(
                    "wind_turbine",
                    () -> IMenuTypeExtension.create(WindTurbineMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<NuclearFacilityMenu>> NUCLEAR_FACILITY =
            ModRegistries.MENU_TYPES.register(
                    "nuclear_facility",
                    () -> IMenuTypeExtension.create(NuclearFacilityMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<NuclearReactorMenu>> NUCLEAR_REACTOR =
            ModRegistries.MENU_TYPES.register(
                    "pressurized_water_reactor",
                    () -> IMenuTypeExtension.create(NuclearReactorMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<NuclearThermalMenu>> NUCLEAR_THERMAL_FACILITY =
            ModRegistries.MENU_TYPES.register(
                    "nuclear_thermal_facility",
                    () -> IMenuTypeExtension.create(NuclearThermalMenu::new)
            );
    public static final DeferredHolder<MenuType<?>, MenuType<SpentFuelPoolMenu>> SPENT_FUEL_POOL =
            ModRegistries.MENU_TYPES.register(
                    "spent_fuel_pool",
                    () -> IMenuTypeExtension.create(SpentFuelPoolMenu::new)
            );

    static {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            if (!definition.hasMenu()) {
                continue;
            }
            SINGLE_BLOCK_MACHINES.put(definition, ModRegistries.MENU_TYPES.register(
                    definition.id(),
                    () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                            new SingleBlockMachineMenu(definition, containerId, inventory, buffer))
            ));
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            ADVANCED_MULTIBLOCKS.put(definition, ModRegistries.MENU_TYPES.register(
                    definition.id(),
                    () -> IMenuTypeExtension.create((containerId, inventory, buffer) ->
                            new AdvancedMultiblockMenu(definition, containerId, inventory, buffer))
            ));
        }
    }

    private ModMenus() {
    }

    public static void bootstrap() {
    }

    public static DeferredHolder<MenuType<?>, MenuType<SingleBlockMachineMenu>> singleBlockMachine(
            SingleBlockMachineDefinition definition
    ) {
        DeferredHolder<MenuType<?>, MenuType<SingleBlockMachineMenu>> type = SINGLE_BLOCK_MACHINES.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No menu type registered for " + definition);
        }
        return type;
    }

    public static Map<SingleBlockMachineDefinition, DeferredHolder<MenuType<?>, MenuType<SingleBlockMachineMenu>>>
    singleBlockMachines() {
        return Collections.unmodifiableMap(SINGLE_BLOCK_MACHINES);
    }

    public static DeferredHolder<MenuType<?>, MenuType<ProgrammableMenu>> programmable(boolean miningRobot) {
        return miningRobot ? MINING_ROBOT : COMPUTER;
    }

    public static DeferredHolder<MenuType<?>, MenuType<AdvancedMultiblockMenu>> advancedMultiblock(
            MultiblockDefinition definition
    ) {
        DeferredHolder<MenuType<?>, MenuType<AdvancedMultiblockMenu>> type = ADVANCED_MULTIBLOCKS.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No menu type registered for " + definition);
        }
        return type;
    }

    public static Map<MultiblockDefinition, DeferredHolder<MenuType<?>, MenuType<AdvancedMultiblockMenu>>>
    advancedMultiblocks() {
        return Collections.unmodifiableMap(ADVANCED_MULTIBLOCKS);
    }
}
