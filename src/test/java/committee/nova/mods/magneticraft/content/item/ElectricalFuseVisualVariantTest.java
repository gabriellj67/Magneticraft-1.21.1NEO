package committee.nova.mods.magneticraft.content.item;

import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalFuseVisualVariantTest {
    @Test
    void heavyFuseSelectsTheVisualForItsVoltageTier() {
        assertEquals(
                ElectricalFuseVisualVariant.HEAVY_LOW,
                ElectricalFuseVisualVariant.from(data(VoltageTierIds.LOW, ElectricalRatingIds.HEAVY)).orElseThrow()
        );
        assertEquals(
                ElectricalFuseVisualVariant.HEAVY_MEDIUM,
                ElectricalFuseVisualVariant.from(data(VoltageTierIds.MEDIUM, ElectricalRatingIds.HEAVY)).orElseThrow()
        );
        assertEquals(
                ElectricalFuseVisualVariant.HEAVY_HIGH,
                ElectricalFuseVisualVariant.from(data(VoltageTierIds.HIGH, ElectricalRatingIds.HEAVY)).orElseThrow()
        );
    }

    @Test
    void standardFuseKeepsTheBaseVisual() {
        assertTrue(ElectricalFuseVisualVariant.from(
                data(VoltageTierIds.LOW, ElectricalRatingIds.STANDARD)
        ).isEmpty());
    }

    private static TieredElectricalItemData data(
            net.minecraft.resources.ResourceLocation tierId,
            net.minecraft.resources.ResourceLocation ratingId
    ) {
        return new TieredElectricalItemData(tierId, Optional.of(ratingId), Optional.empty());
    }
}
