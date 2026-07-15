package committee.nova.mods.magneticraft.client;

import committee.nova.mods.magneticraft.client.electrical.ClientVoltageTierRegistry;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierDisplay;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ElectricalTierColorsTest {
    private static final int LOW_VOLTAGE_COLOR = 0xD98245;

    @AfterEach
    void resetClientSnapshot() {
        ClientVoltageTierRegistry.reset();
    }

    @Test
    void anyValidElectricalPayloadCanTintAnItemWithoutDependingOnItsItemSubclass() {
        ClientVoltageTierRegistry.apply(1L, List.of(new VoltageTierDisplay(
                VoltageTierIds.LOW,
                "voltage_tier.magneticraft.low_voltage",
                60.0D,
                120.0D,
                125.0D,
                LOW_VOLTAGE_COLOR
        )));
        TieredElectricalItemData transformerPayload = new TieredElectricalItemData(
                VoltageTierIds.LOW,
                Optional.empty(),
                Optional.of(TransformerProfileIds.LV_TO_MV)
        );

        assertEquals(
                LOW_VOLTAGE_COLOR,
                ElectricalTierColors.itemColor(0, Optional.of(transformerPayload))
        );
        assertEquals(ElectricalTierColors.FALLBACK,
                ElectricalTierColors.itemColor(1, Optional.of(transformerPayload)));
        assertEquals(ElectricalTierColors.FALLBACK, ElectricalTierColors.itemColor(0, Optional.empty()));
    }
}
