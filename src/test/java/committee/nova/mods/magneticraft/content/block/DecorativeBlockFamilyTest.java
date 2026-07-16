package committee.nova.mods.magneticraft.content.block;

import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DecorativeBlockFamilyTest {
    @Test
    void catalogueKeepsEightLimestoneFamiliesAndOneRoofFamily() {
        assertEquals(9, DecorativeBlockFamily.values().length);
        assertEquals(8, java.util.Arrays.stream(DecorativeBlockFamily.values())
                .filter(family -> !family.registersBase())
                .count());

        DecorativeBlockFamily roof = DecorativeBlockFamily.TERRACOTTA_ROOF_TILE;
        assertTrue(roof.registersBase());
        assertTrue(roof.weightedTextures());
        assertEquals("roof_tile", roof.baseId());
        assertEquals("roof_tile_stairs", roof.stairsId());
        assertEquals("roof_tile_slab", roof.slabId());

        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            if (family != roof) {
                assertFalse(family.weightedTextures(), family.name());
            }
        }
    }

    @Test
    void everyBaseStairAndSlabIdIsUnique() {
        Set<String> ids = new HashSet<>();
        for (DecorativeBlockFamily family : DecorativeBlockFamily.values()) {
            assertTrue(ids.add(family.baseId()), family.baseId());
            assertTrue(ids.add(family.stairsId()), family.stairsId());
            assertTrue(ids.add(family.slabId()), family.slabId());
        }
        assertEquals(27, ids.size());
    }
}
