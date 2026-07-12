package committee.nova.mods.magneticraft.content.network;

import net.minecraft.world.item.Item;

/**
 * Stateless configuration tool for network side and redstone modes.
 */
public final class WrenchItem extends Item {
    public WrenchItem() {
        super(new Item.Properties().stacksTo(1));
    }
}
