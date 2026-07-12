package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.computer.ProgrammableMenu;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineMenu;
import committee.nova.mods.magneticraft.content.multiblock.AdvancedMultiblockMenu;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Server/client machine menu factories.
 */
public final class ModMenus {
    public static final RegistryObject<MenuType<BatteryMenu>> BATTERY =
            ModRegistries.MENU_TYPES.register("battery", () -> IForgeMenuType.create(BatteryMenu::new));
    public static final RegistryObject<MenuType<ElectricFurnaceMenu>> ELECTRIC_FURNACE =
            ModRegistries.MENU_TYPES.register(
                    "electric_furnace",
                    () -> IForgeMenuType.create(ElectricFurnaceMenu::new)
            );
    public static final RegistryObject<MenuType<SingleBlockMachineMenu>> SINGLE_BLOCK_MACHINE =
            ModRegistries.MENU_TYPES.register(
                    "single_block_machine",
                    () -> IForgeMenuType.create(SingleBlockMachineMenu::new)
            );
    public static final RegistryObject<MenuType<ProgrammableMenu>> PROGRAMMABLE =
            ModRegistries.MENU_TYPES.register(
                    "programmable",
                    () -> IForgeMenuType.create(ProgrammableMenu::new)
            );
    public static final RegistryObject<MenuType<AdvancedMultiblockMenu>> ADVANCED_MULTIBLOCK =
            ModRegistries.MENU_TYPES.register(
                    "advanced_multiblock",
                    () -> IForgeMenuType.create(AdvancedMultiblockMenu::new)
            );

    private ModMenus() {
    }

    public static void bootstrap() {
    }
}
