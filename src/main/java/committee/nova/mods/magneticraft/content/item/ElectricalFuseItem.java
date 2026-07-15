package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

import java.util.Optional;

public final class ElectricalFuseItem extends Item {
    public ElectricalFuseItem() {
        super(new Properties().stacksTo(16));
    }

    @Override
    public Component getName(ItemStack stack) {
        return TieredElectricalItemName.decorate(stack, super.getName(stack));
    }

    public ItemStack stackFor(ResourceLocation tierId, ResourceLocation ratingId) {
        if (!ElectricalRatingIds.isKnown(ratingId)) {
            throw new IllegalArgumentException("Unknown electrical rating " + ratingId);
        }
        ItemStack stack = new ItemStack(this);
        new TieredElectricalItemData(tierId, Optional.of(ratingId), Optional.empty()).write(stack);
        return stack;
    }
}
