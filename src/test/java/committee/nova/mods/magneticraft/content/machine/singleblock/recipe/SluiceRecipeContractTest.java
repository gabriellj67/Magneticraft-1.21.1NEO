package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SluiceRecipeContractTest {
    @Test
    void outputCountMatchesTheJeiLayoutContract() {
        assertDoesNotThrow(() -> SluiceRecipe.validateOutputCount(1));
        assertDoesNotThrow(() -> SluiceRecipe.validateOutputCount(SluiceRecipe.MAX_OUTPUTS));
        assertThrows(IllegalArgumentException.class, () -> SluiceRecipe.validateOutputCount(0));
        assertThrows(
                IllegalArgumentException.class,
                () -> SluiceRecipe.validateOutputCount(SluiceRecipe.MAX_OUTPUTS + 1)
        );
    }
}
