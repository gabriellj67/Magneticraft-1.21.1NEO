package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityMenu;
import committee.nova.mods.magneticraft.content.nuclear.reactor.NuclearReactorMenu;
import committee.nova.mods.magneticraft.content.nuclear.thermal.NuclearThermalMenu;
import committee.nova.mods.magneticraft.content.nuclear.spentfuel.SpentFuelPoolMenu;
import committee.nova.mods.magneticraft.content.network.electric.ElectricalDeviceMenu;
import committee.nova.mods.magneticraft.content.network.pressure.PressureTankMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Server/client machine menu factories.
 */
public final class ModMenus {
    private static final Map<SingleBlockMachineDefinition, RegistryObject<MenuType<SingleBlockMachineMenu>>>
            SINGLE_BLOCK_MACHINES = new EnumMap<>(SingleBlockMachineDefinition.class);
    private static final Map<MultiblockDefinition, RegistryObject<MenuType<AdvancedMultiblockMenu>>>
            ADVANCED_MULTIBLOCKS = new EnumMap<>(MultiblockDefinition.class);

    public static final RegistryObject<MenuType<BatteryMenu>> BATTERY =
            ModRegistries.MENU_TYPES.register("battery_box", () -> IForgeMenuType.create(BatteryMenu::new));
    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
            ModRegistries.MENU_TYPES.register(
                    "electric_furnace",
                    () -> IForgeMenuType.create(ElectricFurnaceMenu::new)
            );
    public static final RegistryObject<MenuType<ProgrammableMenu>> COMPUTER =
            ModRegistries.MENU_TYPES.register(
                    "computer",
                    () -> IForgeMenuType.create((containerId, inventory, buffer) ->
                            new ProgrammableMenu(false, containerId, inventory, buffer))
            );
    public static final RegistryObject<MenuType<ProgrammableMenu>> MINING_ROBOT =
            ModRegistries.MENU_TYPES.register(
                    "mining_robot",
                    () -> IForgeMenuType.create((containerId, inventory, buffer) ->
                            new ProgrammableMenu(true, containerId, inventory, buffer))
            );
    public static final RegistryObject<MenuType<ElectricalDeviceMenu>> ELECTRICAL_DEVICE =
            ModRegistries.MENU_TYPES.register(
                    "electrical_device",
                    () -> IForgeMenuType.create(ElectricalDeviceMenu::new)
            );
    public static final RegistryObject<MenuType<PressureTankMenu>> PRESSURE_TANK =
            ModRegistries.MENU_TYPES.register(
                    "pressure_tank",
                    () -> IForgeMenuType.create(PressureTankMenu::new)
            );
    public static final RegistryObject<MenuType<NuclearFacilityMenu>> NUCLEAR_FACILITY =
            ModRegistries.MENU_TYPES.register(
                    "nuclear_facility",
                    () -> IForgeMenuType.create(NuclearFacilityMenu::new)
            );
    public static final RegistryObject<MenuType<NuclearReactorMenu>> NUCLEAR_REACTOR =
            ModRegistries.MENU_TYPES.register(
                    "pressurized_water_reactor",
                    () -> IForgeMenuType.create(NuclearReactorMenu::new)
            );
    public static final RegistryObject<MenuType<NuclearThermalMenu>> NUCLEAR_THERMAL_FACILITY =
            ModRegistries.MENU_TYPES.register(
                    "nuclear_thermal_facility",
                    () -> IForgeMenuType.create(NuclearThermalMenu::new)
            );
    public static final RegistryObject<MenuType<SpentFuelPoolMenu>> SPENT_FUEL_POOL =
            ModRegistries.MENU_TYPES.register(
                    "spent_fuel_pool",
                    () -> IForgeMenuType.create(SpentFuelPoolMenu::new)
            );

    static {
        for (SingleBlockMachineDefinition definition : SingleBlockMachineDefinition.values()) {
            if (!definition.hasMenu()) {
                continue;
            }
            SINGLE_BLOCK_MACHINES.put(definition, ModRegistries.MENU_TYPES.register(
                    definition.id(),
                    () -> IForgeMenuType.create((containerId, inventory, buffer) ->
                            new SingleBlockMachineMenu(definition, containerId, inventory, buffer))
            ));
        }
        for (MultiblockDefinition definition : MultiblockDefinition.values()) {
            ADVANCED_MULTIBLOCKS.put(definition, ModRegistries.MENU_TYPES.register(
                    definition.id(),
                    () -> IForgeMenuType.create((containerId, inventory, buffer) ->
                            new AdvancedMultiblockMenu(definition, containerId, inventory, buffer))
            ));
        }
    }

    private ModMenus() {
    }

    public static void bootstrap() {
    }

    public static RegistryObject<MenuType<SingleBlockMachineMenu>> singleBlockMachine(
            SingleBlockMachineDefinition definition
    ) {
        RegistryObject<MenuType<SingleBlockMachineMenu>> type = SINGLE_BLOCK_MACHINES.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No menu type registered for " + definition);
        }
        return type;
    }

    public static Map<SingleBlockMachineDefinition, RegistryObject<MenuType<SingleBlockMachineMenu>>>
    singleBlockMachines() {
        return Collections.unmodifiableMap(SINGLE_BLOCK_MACHINES);
    }

    public static RegistryObject<MenuType<ProgrammableMenu>> programmable(boolean miningRobot) {
        return miningRobot ? MINING_ROBOT : COMPUTER;
    }

    public static RegistryObject<MenuType<AdvancedMultiblockMenu>> advancedMultiblock(
            MultiblockDefinition definition
    ) {
        RegistryObject<MenuType<AdvancedMultiblockMenu>> type = ADVANCED_MULTIBLOCKS.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No menu type registered for " + definition);
        }
        return type;
    }

    public static Map<MultiblockDefinition, RegistryObject<MenuType<AdvancedMultiblockMenu>>>
    advancedMultiblocks() {
        return Collections.unmodifiableMap(ADVANCED_MULTIBLOCKS);
    }
}
