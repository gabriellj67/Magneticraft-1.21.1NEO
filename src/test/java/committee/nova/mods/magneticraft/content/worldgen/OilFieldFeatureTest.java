package committee.nova.mods.magneticraft.content.worldgen;

import committee.nova.mods.magneticraft.MinecraftTestBootstrap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.BitSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OilFieldFeatureTest {
    private static final int MIN_SAMPLE_CHUNK = -16;
    private static final int MAX_SAMPLE_CHUNK_EXCLUSIVE = 16;
    private static final int BLOCKS_PER_CHUNK = 16;
    private static final int SAMPLE_SIZE = 32 * 32 * BLOCKS_PER_CHUNK * BLOCKS_PER_CHUNK;

    @BeforeAll
    static void bootstrapMinecraftRegistries() {
        MinecraftTestBootstrap.ensureBootstrapped();
    }

    @Test
    void legacyCoordinateKeepsTheEightBlockShiftAtPositiveAndNegativeChunkBoundaries() {
        int[] targetChunks = {-161, -160, -1, 0, 1, 159, 160};
        for (int targetChunk : targetChunks) {
            assertEquals(
                    new OilFieldFeature.LegacyCoordinate(targetChunk - 1, 8),
                    OilFieldFeature.legacyCoordinate(targetChunk, 0)
            );
            assertEquals(
                    new OilFieldFeature.LegacyCoordinate(targetChunk - 1, 15),
                    OilFieldFeature.legacyCoordinate(targetChunk, 7)
            );
            assertEquals(
                    new OilFieldFeature.LegacyCoordinate(targetChunk, 0),
                    OilFieldFeature.legacyCoordinate(targetChunk, 8)
            );
            assertEquals(
                    new OilFieldFeature.LegacyCoordinate(targetChunk, 7),
                    OilFieldFeature.legacyCoordinate(targetChunk, 15)
            );

            for (int local = 0; local < BLOCKS_PER_CHUNK; local++) {
                OilFieldFeature.LegacyCoordinate source = OilFieldFeature.legacyCoordinate(targetChunk, local);
                assertEquals(
                        targetChunk * BLOCKS_PER_CHUNK + local - 8,
                        source.chunk() * BLOCKS_PER_CHUNK + source.local()
                );
            }
        }
    }

    @Test
    void sectorEligibilityUsesFloorSemanticsForNegativeCoordinates() {
        assertEquals(
                new OilFieldFeature.FieldCenter(128, 128),
                OilFieldFeature.fieldCenter(0, 15)
        );
        assertEquals(
                new OilFieldFeature.FieldCenter(128, 128),
                OilFieldFeature.fieldCenter(15, 0)
        );
        assertNull(OilFieldFeature.fieldCenter(-1, 0));
        assertNull(OilFieldFeature.fieldCenter(16, 0));

        OilFieldFeature.FieldCenter negativeCenter = new OilFieldFeature.FieldCenter(-2432, -2432);
        assertEquals(negativeCenter, OilFieldFeature.fieldCenter(-160, -160));
        assertEquals(negativeCenter, OilFieldFeature.fieldCenter(-145, -145));
        assertNull(OilFieldFeature.fieldCenter(-161, -160));
        assertNull(OilFieldFeature.fieldCenter(-144, -160));

        OilFieldFeature.FieldCenter mixedCenter = new OilFieldFeature.FieldCenter(2688, -2432);
        assertEquals(mixedCenter, OilFieldFeature.fieldCenter(160, -160));
        assertEquals(mixedCenter, OilFieldFeature.fieldCenter(175, -145));
        assertNull(OilFieldFeature.fieldCenter(159, -160));
        assertNull(OilFieldFeature.fieldCenter(176, -160));
    }

    @Test
    void fieldGeometryKeepsLegacyOuterGuardAndInnerDomeBoundaries() {
        OilFieldFeature.FieldCenter center = new OilFieldFeature.FieldCenter(128, 128);
        assertEquals(4, OilFieldFeature.elevation(128, 128, center));
        assertEquals(0, OilFieldFeature.elevation(207, 128, center));
        assertEquals(-1, OilFieldFeature.elevation(208, 128, center));
        assertEquals(0, OilFieldFeature.elevation(49, 128, center));
        assertEquals(-1, OilFieldFeature.elevation(48, 128, center));

        assertTrue(OilFieldFeature.sourceChunkWithinOuterRadius(13, 10, center));
        assertFalse(OilFieldFeature.sourceChunkWithinOuterRadius(13, 11, center));
        assertTrue(OilFieldFeature.sourceChunkWithinOuterRadius(3, 6, center));
        assertFalse(OilFieldFeature.sourceChunkWithinOuterRadius(3, 5, center));
    }

    @Test
    void selectionIsDeterministicAndApproximatelyOneThird() {
        long seed = 0x5A17C0FFEE1234ABL;
        BitSet first = selectionPattern(seed);
        BitSet repeated = selectionPattern(seed);
        BitSet anotherSeed = selectionPattern(seed + 1L);

        assertEquals(first, repeated);
        assertNotEquals(first, anotherSeed);

        double selectedRatio = (double) first.cardinality() / SAMPLE_SIZE;
        assertTrue(
                selectedRatio >= 0.32D && selectedRatio <= 0.35D,
                () -> "Expected a roughly one-third selection ratio, got " + selectedRatio
        );
    }

    private static BitSet selectionPattern(long seed) {
        BitSet selections = new BitSet(SAMPLE_SIZE);
        int index = 0;
        for (int chunkX = MIN_SAMPLE_CHUNK; chunkX < MAX_SAMPLE_CHUNK_EXCLUSIVE; chunkX++) {
            for (int chunkZ = MIN_SAMPLE_CHUNK; chunkZ < MAX_SAMPLE_CHUNK_EXCLUSIVE; chunkZ++) {
                for (int localX = 0; localX < BLOCKS_PER_CHUNK; localX++) {
                    for (int localZ = 0; localZ < BLOCKS_PER_CHUNK; localZ++) {
                        if (OilFieldFeature.selected(seed, chunkX, chunkZ, localX, localZ)) {
                            selections.set(index);
                        }
                        index++;
                    }
                }
            }
        }
        assertEquals(SAMPLE_SIZE, index);
        return selections;
    }
}
