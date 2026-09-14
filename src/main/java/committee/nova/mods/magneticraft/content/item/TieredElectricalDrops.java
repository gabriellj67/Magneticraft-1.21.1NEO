package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;

/** Shared tier-only drop projection for tiered electrical block entities. */
public final class TieredElectricalDrops {
    private TieredElectricalDrops() {
    }

    public static List<ItemStack> preserveTier(List<ItemStack> drops, Item blockItem, BlockEntity blockEntity) {
        if (!(blockItem instanceof TieredElectricalBlockItem)
                || !(blockEntity instanceof TieredElectricalHost host)) {
            return List.copyOf(drops);
        }
        ElectricalNetworkModule electricity = host.tieredElectricalModule();
        if (electricity == null) {
            return List.copyOf(drops);
        }
        TieredElectricalItemData itemData = TieredElectricalItemData.forTier(electricity.tierId());
        drops.stream().filter(stack -> stack.is(blockItem)).forEach(itemData::write);
        return List.copyOf(drops);
    }
}
