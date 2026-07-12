package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.battery.BatteryMenu;
import committee.nova.mods.magneticraft.content.machine.electricfurnace.ElectricFurnaceMenu;
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

    private ModMenus() {
    }

    public static void bootstrap() {
    }
}
