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

    private ModMachineItems() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.of(LOW_BATTERY);
    }
}
