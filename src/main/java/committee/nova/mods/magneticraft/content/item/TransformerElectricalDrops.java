package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.network.module.TransformerElectricalHost;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.List;
import java.util.Optional;

/** Preserves a transformer's profile identity while deliberately discarding node energy. */
public final class TransformerElectricalDrops {
    private TransformerElectricalDrops() {
    }

    public static List<ItemStack> preserveProfile(
            List<ItemStack> drops,
            Item expectedItem,
            BlockEntity blockEntity
    ) {
        if (!(blockEntity instanceof TransformerElectricalHost host)) {
            return List.copyOf(drops);
        }
        var transformer = host.transformerCoupler();
        TieredElectricalItemData data = new TieredElectricalItemData(
                transformer.first().tierId(),
                Optional.empty(),
                Optional.of(transformer.profileId())
        );
        drops.stream().filter(stack -> stack.is(expectedItem)).forEach(data::write);
        return List.copyOf(drops);
    }
}
