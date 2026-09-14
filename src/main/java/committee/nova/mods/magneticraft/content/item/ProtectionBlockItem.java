package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.content.network.electric.ElectricalProtectionKind;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Block;

import java.util.Objects;
import java.util.Optional;

/** Tiered block item whose breaker variant also requires a stable protection rating. */
public final class ProtectionBlockItem extends TieredElectricalBlockItem {
    private final ElectricalProtectionKind kind;

    public ProtectionBlockItem(Block block, Properties properties, ElectricalProtectionKind kind) {
        super(block, properties);
        this.kind = Objects.requireNonNull(kind, "kind");
    }

    @Override
    public InteractionResult place(BlockPlaceContext context) {
        Optional<TieredElectricalItemData> data = TieredElectricalItemData.read(context.getItemInHand());
        if (data.isEmpty() || !validRating(data.orElseThrow())) {
            return InteractionResult.FAIL;
        }
        return super.place(context);
    }

    public ItemStack stackFor(ResourceLocation tierId, ResourceLocation ratingId) {
        ItemStack stack = new ItemStack(this);
        new TieredElectricalItemData(tierId, Optional.ofNullable(ratingId), Optional.empty()).write(stack);
        return stack;
    }

    public ElectricalProtectionKind kind() {
        return kind;
    }

    private boolean validRating(TieredElectricalItemData data) {
        if (kind == ElectricalProtectionKind.FUSE_BOX) {
            return data.ratingId().isEmpty();
        }
        return data.ratingId().filter(ElectricalRatingIds::isKnown).isPresent();
    }
}
