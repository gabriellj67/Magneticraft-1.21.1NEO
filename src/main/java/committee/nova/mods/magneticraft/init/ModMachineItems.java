package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.item.LowBatteryItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Registers portable items introduced by the machine framework.
 */
public final class ModMachineItems {
    public static final RegistryObject<Item> LOW_BATTERY =
            ModRegistries.ITEMS.register("battery_item_low", LowBatteryItem::new);
    public static final RegistryObject<Item> INSERTER_SPEED_UPGRADE =
            ModRegistries.ITEMS.register("inserter_speed_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));
    public static final RegistryObject<Item> INSERTER_STACK_UPGRADE =
            ModRegistries.ITEMS.register("inserter_stack_upgrade", () -> new Item(new Item.Properties().stacksTo(1)));

    private ModMachineItems() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.of(LOW_BATTERY, INSERTER_SPEED_UPGRADE, INSERTER_STACK_UPGRADE);
    }
}
