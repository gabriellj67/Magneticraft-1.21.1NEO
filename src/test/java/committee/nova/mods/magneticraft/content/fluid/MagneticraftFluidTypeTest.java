package committee.nova.mods.magneticraft.content.fluid;

import committee.nova.mods.magneticraft.Magneticraft;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class MagneticraftFluidTypeTest {
    @Test
    void clientTexturesAreDerivedFromConstructorSafeDescriptionIds() {
        for (FluidDefinition definition : FluidDefinition.values()) {
            assertEquals(
                    Magneticraft.id("fluid/" + definition.textureId() + "_still"),
                    MagneticraftFluidType.texture(definition.translationKey(), "_still")
            );
            assertEquals(
                    Magneticraft.id("fluid/" + definition.textureId() + "_flow"),
                    MagneticraftFluidType.texture(definition.translationKey(), "_flow")
            );
        }
    }

    @Test
    void foreignDescriptionIdsAreRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> MagneticraftFluidType.texture("fluid_type.other.oil", "_still")
        );
    }
}
