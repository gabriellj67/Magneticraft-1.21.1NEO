package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

import java.util.Arrays;
import java.util.Optional;

/** Heavy-duty fuse visuals keyed by the existing electrical tier payload. */
public enum ElectricalFuseVisualVariant {
    HEAVY_LOW(VoltageTierIds.LOW, "heavy_low_voltage", 1),
    HEAVY_MEDIUM(VoltageTierIds.MEDIUM, "heavy_medium_voltage", 2),
    HEAVY_HIGH(VoltageTierIds.HIGH, "heavy_high_voltage", 3);

    private final ResourceLocation tierId;
    private final String modelSuffix;
    private final int predicateValue;

    ElectricalFuseVisualVariant(ResourceLocation tierId, String modelSuffix, int predicateValue) {
        this.tierId = tierId;
        this.modelSuffix = modelSuffix;
        this.predicateValue = predicateValue;
    }

    public String modelSuffix() {
        return modelSuffix;
    }

    public int predicateValue() {
        return predicateValue;
    }

    public static Optional<ElectricalFuseVisualVariant> from(ItemStack stack) {
        return TieredElectricalItemData.read(stack).flatMap(ElectricalFuseVisualVariant::from);
    }

    public static Optional<ElectricalFuseVisualVariant> from(TieredElectricalItemData data) {
        if (!data.ratingId().filter(ElectricalRatingIds.HEAVY::equals).isPresent()) {
            return Optional.empty();
        }
        return Arrays.stream(values())
                .filter(variant -> variant.tierId.equals(data.tierId()))
                .findFirst();
    }
}
