package committee.nova.mods.magneticraft.content.worldgen;

import com.mojang.serialization.Codec;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Chunk-local reconstruction of Nova 1.12's deterministic 16x16-sector oil fields.
 * It never writes outside the chunk currently being populated.
 */
public final class OilFieldFeature extends Feature<NoneFeatureConfiguration> {
    static final int SECTOR_SIZE_CHUNKS = 16;
    static final int FIELD_DISTANCE_SECTORS = 10;
    static final int FIELD_CENTER_OFFSET_BLOCKS = 128;
    static final int OUTER_RADIUS_BLOCKS = 88;
    static final int INNER_RADIUS_BLOCKS = 80;
    static final int BASE_Y = 20;

    public OilFieldFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        int targetChunkX = Math.floorDiv(context.origin().getX(), 16);
        int targetChunkZ = Math.floorDiv(context.origin().getZ(), 16);
        int placed = 0;

        for (int localX = 0; localX < 16; localX++) {
            LegacyCoordinate sourceX = legacyCoordinate(targetChunkX, localX);
            for (int localZ = 0; localZ < 16; localZ++) {
                LegacyCoordinate sourceZ = legacyCoordinate(targetChunkZ, localZ);
                FieldCenter center = fieldCenter(sourceX.chunk(), sourceZ.chunk());
                if (center == null
                        || !sourceChunkWithinOuterRadius(sourceX.chunk(), sourceZ.chunk(), center)
                        || !selected(context.level().getSeed(), sourceX.chunk(), sourceZ.chunk(),
                        sourceX.local(), sourceZ.local())) {
                    continue;
                }

                int worldX = targetChunkX * 16 + localX;
                int worldZ = targetChunkZ * 16 + localZ;
                int elevation = elevation(worldX, worldZ, center);
                if (elevation < 0) {
                    continue;
                }
                for (int y = BASE_Y - elevation; y <= BASE_Y + elevation; y++) {
                    BlockPos position = new BlockPos(worldX, y, worldZ);
                    BlockState current = level.getBlockState(position);
                    if (!current.is(BlockTags.STONE_ORE_REPLACEABLES)
                            && !current.is(BlockTags.DEEPSLATE_ORE_REPLACEABLES)) {
                        continue;
                    }
                    if (level.setBlock(position, ModAdvancedBlocks.OIL_DEPOSIT.get().defaultBlockState(), 2)) {
                        if (level.getBlockEntity(position) instanceof OilDepositBlockEntity deposit) {
                            deposit.setFieldOrigin(new BlockPos(center.x(), BASE_Y, center.z()));
                        }
                        placed++;
                    }
                }
            }
        }
        return placed > 0;
    }

    static LegacyCoordinate legacyCoordinate(int targetChunk, int localBlock) {
        return localBlock < 8
                ? new LegacyCoordinate(targetChunk - 1, localBlock + 8)
                : new LegacyCoordinate(targetChunk, localBlock - 8);
    }

    static FieldCenter fieldCenter(int sourceChunkX, int sourceChunkZ) {
        int sectorX = sourceChunkX >> 4;
        int sectorZ = sourceChunkZ >> 4;
        if (Math.floorMod(sectorX, FIELD_DISTANCE_SECTORS) != 0
                || Math.floorMod(sectorZ, FIELD_DISTANCE_SECTORS) != 0) {
            return null;
        }
        return new FieldCenter(
                sectorX * SECTOR_SIZE_CHUNKS * 16 + FIELD_CENTER_OFFSET_BLOCKS,
                sectorZ * SECTOR_SIZE_CHUNKS * 16 + FIELD_CENTER_OFFSET_BLOCKS
        );
    }

    static boolean sourceChunkWithinOuterRadius(int chunkX, int chunkZ, FieldCenter center) {
        long deltaX = (long) chunkX * 16L - center.x();
        long deltaZ = (long) chunkZ * 16L - center.z();
        return deltaX * deltaX + deltaZ * deltaZ
                <= (long) OUTER_RADIUS_BLOCKS * OUTER_RADIUS_BLOCKS;
    }

    static int elevation(int x, int z, FieldCenter center) {
        long deltaX = (long) x - center.x();
        long deltaZ = (long) z - center.z();
        long distanceSquared = deltaX * deltaX + deltaZ * deltaZ;
        long radiusSquared = (long) INNER_RADIUS_BLOCKS * INNER_RADIUS_BLOCKS;
        if (distanceSquared >= radiusSquared) {
            return -1;
        }
        return (int) ((1.0D - (double) distanceSquared / radiusSquared) * 4.0D);
    }

    static boolean selected(long worldSeed, int chunkX, int chunkZ, int localX, int localZ) {
        long value = worldSeed;
        value ^= (long) chunkX * 0x9E3779B97F4A7C15L;
        value ^= (long) chunkZ * 0xC2B2AE3D27D4EB4FL;
        value ^= (long) localX * 0x165667B19E3779F9L;
        value ^= (long) localZ * 0x85EBCA77C2B2AE63L;
        value ^= value >>> 30;
        value *= 0xBF58476D1CE4E5B9L;
        value ^= value >>> 27;
        value *= 0x94D049BB133111EBL;
        value ^= value >>> 31;
        return Math.floorMod(value, 3L) == 0L;
    }

    record LegacyCoordinate(int chunk, int local) {
    }

    record FieldCenter(int x, int z) {
    }
}
