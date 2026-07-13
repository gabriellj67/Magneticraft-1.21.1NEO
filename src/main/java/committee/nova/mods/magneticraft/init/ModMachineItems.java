package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.item.ElectricChainsawItem;
import committee.nova.mods.magneticraft.content.item.ElectricDrillItem;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.LowBatteryItem;
import committee.nova.mods.magneticraft.content.item.MediumBatteryItem;
import committee.nova.mods.magneticraft.content.item.ThermometerItem;
import committee.nova.mods.magneticraft.content.item.VoltmeterItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Registers portable items introduced by the machine framework.
 */
public final class ModMachineItems {
    public static final RegistryObject<Item> LOW_BATTERY =
            ModRegistries.ITEMS.register("battery_item_low", LowBatteryItem::new);
    public static final RegistryObject<Item> MEDIUM_BATTERY =
            ModRegistries.ITEMS.register("battery_item_medium", MediumBatteryItem::new);
    public static final RegistryObject<Item> ELECTRIC_DRILL =
            ModRegistries.ITEMS.register("electric_drill", ElectricDrillItem::new);
    public static final RegistryObject<Item> ELECTRIC_CHAINSAW =
            ModRegistries.ITEMS.register("electric_chainsaw", ElectricChainsawItem::new);
    public static final RegistryObject<Item> ELECTRIC_PISTON =
            ModRegistries.ITEMS.register("electric_piston", ElectricPistonItem::new);
    public static final RegistryObject<Item> VOLTMETER =
            ModRegistries.ITEMS.register("voltmeter", VoltmeterItem::new);
    public static final RegistryObject<Item> THERMOMETER =
            ModRegistries.ITEMS.register("thermometer", ThermometerItem::new);
    public static final RegistryObject<Item> INSERTER_SPEED_UPGRADE =
            ModRegistries.ITEMS.register("inserter_speed_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> INSERTER_STACK_UPGRADE =
            ModRegistries.ITEMS.register("inserter_stack_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));

    private ModMachineItems() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.of(
                LOW_BATTERY,
                MEDIUM_BATTERY,
                ELECTRIC_DRILL,
                ELECTRIC_CHAINSAW,
                ELECTRIC_PISTON,
                VOLTMETER,
                THERMOMETER,
                INSERTER_SPEED_UPGRADE,
                INSERTER_STACK_UPGRADE
        );
    }
}
