package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.network.WrenchItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Player tools introduced with the physical-network task.
 */
public final class ModNetworkItems {
    public static final RegistryObject<Item> WRENCH = ModRegistries.ITEMS.register("wrench", WrenchItem::new);

    private ModNetworkItems() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.of(WRENCH);
    }
}
