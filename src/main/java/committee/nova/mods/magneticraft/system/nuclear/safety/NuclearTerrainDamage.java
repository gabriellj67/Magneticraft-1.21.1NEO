package committee.nova.mods.magneticraft.system.nuclear.safety;

import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameters;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.common.util.FakePlayer;
import net.minecraftforge.common.util.FakePlayerFactory;
import net.minecraftforge.event.level.BlockEvent;

/** One-shot bounded deterministic terrain consequence with claim/event and block-entity protection. */
public final class NuclearTerrainDamage {
    private static final int MAX_DESTROYED_BLOCKS = 4_096;

    private NuclearTerrainDamage() {
    }

    public static int radius(double accidentEnergyJoules, double containmentIntegrity,
                             ReactorParameters parameters, double scale) {
        double energyRatio = Math.max(0.0D, accidentEnergyJoules)
                / parameters.accidentTerrainDamageEnergyJoules();
        double raw = 6.0D + Math.min(6.0D, Math.sqrt(energyRatio) * 3.0D)
                + (1.0D - Math.max(0.0D, Math.min(1.0D, containmentIntegrity))) * 12.0D;
        return (int) Math.max(6, Math.min(24, Math.round(raw * Math.max(0.0D, scale))));
    }

    public static int apply(ServerLevel level, BlockPos center, double accidentEnergyJoules,
                            double containmentIntegrity, ReactorParameters parameters) {
        if (!MagneticraftConfig.ENABLE_NUCLEAR_TERRAIN_DAMAGE.get()) return 0;
        int radius = radius(accidentEnergyJoules, containmentIntegrity, parameters,
                MagneticraftConfig.NUCLEAR_ACCIDENT_DAMAGE_SCALE.get());
        int destroyed = 0;
        int debris = 0;
        long seed = center.asLong() ^ Double.doubleToLongBits(accidentEnergyJoules);
        FakePlayer actor = FakePlayerFactory.getMinecraft(level);
        for (BlockPos position : BlockPos.betweenClosed(center.offset(-radius, -radius, -radius),
                center.offset(radius, radius, radius))) {
            if (destroyed >= MAX_DESTROYED_BLOCKS || !level.hasChunkAt(position)
                    || level.getBlockEntity(position) != null) continue;
            double distance = Math.sqrt(position.distSqr(center));
            if (distance > radius || distance < 2.0D) continue;
            long hash = mix(seed ^ position.asLong());
            double chance = (1.0D - distance / (radius + 1.0D)) * 0.45D;
            if (((hash >>> 11) * 0x1.0p-53) > chance) continue;
            BlockState state = level.getBlockState(position);
            if (state.isAir() || state.getDestroySpeed(level, position) < 0.0F) continue;
            BlockEvent.BreakEvent event = new BlockEvent.BreakEvent(level, position, state, actor);
            if (MinecraftForge.EVENT_BUS.post(event)) continue;
            if (level.destroyBlock(position, true)) {
                destroyed++;
                if (debris < 24 && Math.floorMod(hash, 43L) == 0L
                        && level.getBlockState(position).isAir()
                        && !level.getBlockState(position.below()).isAir()) {
                    level.setBlockAndUpdate(position, ModNuclearBlocks.RADIOACTIVE_DEBRIS.get().defaultBlockState());
                    debris++;
                }
            }
        }
        return destroyed;
    }

    private static long mix(long value) {
        value ^= value >>> 33;
        value *= 0xff51afd7ed558ccdl;
        value ^= value >>> 33;
        value *= 0xc4ceb9fe1a85ec53l;
        return value ^ value >>> 33;
    }
}
