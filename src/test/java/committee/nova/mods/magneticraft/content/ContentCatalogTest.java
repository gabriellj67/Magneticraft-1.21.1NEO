package committee.nova.mods.magneticraft.content;

import committee.nova.mods.magneticraft.content.block.BaseBlockDefinition;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.item.CraftingComponent;
import committee.nova.mods.magneticraft.content.item.HammerType;
import committee.nova.mods.magneticraft.content.material.MaterialForm;
import committee.nova.mods.magneticraft.content.material.Metal;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContentCatalogTest {
    @Test
    void materialFormsMatchTheLegacyVisibleVariants() {
        Map<MaterialForm, Integer> expectedCounts = new EnumMap<>(MaterialForm.class);
        expectedCounts.put(MaterialForm.INGOT, 13);
        expectedCounts.put(MaterialForm.NUGGET, 12);
        expectedCounts.put(MaterialForm.LIGHT_PLATE, 6);
        expectedCounts.put(MaterialForm.HEAVY_PLATE, 6);
        expectedCounts.put(MaterialForm.CHUNK, 13);
        expectedCounts.put(MaterialForm.DUST, 15);
        expectedCounts.put(MaterialForm.ROCKY_CHUNK, 14);

        Set<String> ids = new HashSet<>();
        int total = 0;
        for (MaterialForm form : MaterialForm.values()) {
            long count = Arrays.stream(Metal.values()).filter(form::appliesTo).count();
            assertEquals(expectedCounts.get(form).longValue(), count, form.name());
            for (Metal metal : Metal.values()) {
                if (form.appliesTo(metal)) {
                    assertTrue(ids.add(form.id(metal)), form.id(metal));
                    total++;
                }
            }
        }

        assertEquals(79, total);
    }

    @Test
    void copperUsesOnlyTheVanillaFormsThatActuallyExist() {
        assertTrue(Metal.COPPER.hasVanillaIngot());
        assertFalse(Metal.COPPER.hasVanillaNugget());
        assertFalse(MaterialForm.INGOT.appliesTo(Metal.COPPER));
        assertTrue(MaterialForm.NUGGET.appliesTo(Metal.COPPER));
        assertTrue(MaterialForm.LIGHT_PLATE.appliesTo(Metal.COPPER));
        assertTrue(MaterialForm.HEAVY_PLATE.appliesTo(Metal.COPPER));
    }

    @Test
    void compositeGalenaOnlyOwnsItsRockyChunk() {
        for (MaterialForm form : MaterialForm.values()) {
            assertEquals(form == MaterialForm.ROCKY_CHUNK, form.appliesTo(Metal.GALENA), form.name());
        }
    }

    @Test
    void restoredIndustrialMaterialsDeclareOnlyTheirSupportedForms() {
        assertEquals(Set.of(MaterialForm.INGOT, MaterialForm.DUST), Metal.BRASS.supportedForms());
        assertEquals(Set.of(MaterialForm.INGOT), Metal.CARBIDE.supportedForms());
    }

    @Test
    void baseCataloguesHaveStableUniqueIds() {
        assertUniqueIds(
                Arrays.stream(BaseBlockDefinition.values()).map(BaseBlockDefinition::id).toArray(String[]::new),
                23
        );
        assertUniqueIds(
                Arrays.stream(CraftingComponent.values()).map(CraftingComponent::id).toArray(String[]::new),
                7
        );
        assertUniqueIds(
                Arrays.stream(HammerType.values()).map(HammerType::id).toArray(String[]::new),
                3
        );
        assertUniqueIds(
                Arrays.stream(FluidDefinition.values()).map(FluidDefinition::id).toArray(String[]::new),
                16
        );
    }

    @Test
    void fluidPropertiesMatchTheLegacyRegistry() {
        assertFluid(FluidDefinition.STEAM, 373, 1, 10, true);
        assertFluid(FluidDefinition.OIL, 298, 1100, 2000, false);
        assertFluid(FluidDefinition.HOT_CRUDE, 873, 10, 20, true);
        assertFluid(FluidDefinition.LPG, 298, 300, 1000, false);
        assertFluid(FluidDefinition.LIGHT_OIL, 298, 700, 1000, false);
        assertFluid(FluidDefinition.HEAVY_OIL, 298, 800, 1000, false);
        assertFluid(FluidDefinition.NATURAL_GAS, 298, 1, 10, true);
        assertFluid(FluidDefinition.NAPHTHA, 298, 800, 1000, false);
        assertFluid(FluidDefinition.PLASTIC, 298, 900, 1000, false);
        assertFluid(FluidDefinition.GASOLINE, 298, 500, 1000, false);
        assertFluid(FluidDefinition.KEROSENE, 298, 600, 1000, false);
        assertFluid(FluidDefinition.DIESEL, 298, 700, 1000, false);
        assertFluid(FluidDefinition.LUBRICANT, 298, 600, 1000, false);
        assertFluid(FluidDefinition.FUEL, 298, 700, 1000, false);
        assertFluid(FluidDefinition.OIL_RESIDUE, 298, 800, 2000, false);
        assertFluid(FluidDefinition.WOOD_GAS, 373, 1, 10, true);
    }

    private static void assertFluid(
            FluidDefinition fluid,
            int temperatureKelvin,
            int density,
            int viscosity,
            boolean gaseous
    ) {
        assertEquals(temperatureKelvin, fluid.temperatureKelvin(), fluid.id());
        assertEquals(density, fluid.density(), fluid.id());
        assertEquals(viscosity, fluid.viscosity(), fluid.id());
        assertEquals(gaseous, fluid.isGaseous(), fluid.id());
    }

    private static void assertUniqueIds(String[] ids, int expectedCount) {
        assertEquals(expectedCount, ids.length);
        assertEquals(expectedCount, Set.of(ids).size());
    }
}
