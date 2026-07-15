package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.network.WrenchItem;
import committee.nova.mods.magneticraft.content.item.CopperWireCoilItem;
import committee.nova.mods.magneticraft.content.item.ElectricalFuseItem;
import committee.nova.mods.magneticraft.content.item.ElectricalRepairToolItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.RegistryObject;

import java.util.List;

/**
 * Player tools introduced with the physical-network task.
 */
public final class ModNetworkItems {
    public static final RegistryObject<Item> WRENCH = ModRegistries.ITEMS.register("wrench", WrenchItem::new);
    public static final RegistryObject<Item> COPPER_WIRE_COIL = ModRegistries.ITEMS.register(
            "copper_wire_coil",
            CopperWireCoilItem::new
    );
    public static final RegistryObject<ElectricalFuseItem> FUSE = ModRegistries.ITEMS.register(
            "electrical_fuse",
            ElectricalFuseItem::new
    );
    public static final RegistryObject<Item> ELECTRICAL_REPAIR_TOOL = ModRegistries.ITEMS.register(
            "electrical_repair_tool",
            ElectricalRepairToolItem::new
    );

    private ModNetworkItems() {
    }

    public static void bootstrap() {
    }

    public static List<RegistryObject<? extends Item>> creativeItems() {
        return List.of(WRENCH, COPPER_WIRE_COIL, FUSE, ELECTRICAL_REPAIR_TOOL);
    }
}
