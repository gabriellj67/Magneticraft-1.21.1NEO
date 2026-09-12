package committee.nova.mods.magneticraft.system.nuclear.radiation;

import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

/** Bounded line sampling for distance and material shielding. */
public final class RadiationShielding {
    private RadiationShielding() {
    }

    public static double attenuation(ServerLevel level, Vec3 source, Vec3 target) {
        double distance = source.distanceTo(target);
        int samples = Math.max(1, Math.min(64, (int) Math.ceil(distance * 2.0D)));
        double factor = 1.0D;
        BlockPos previous = null;
        for (int sample = 1; sample < samples; sample++) {
            double t = sample / (double) samples;
            BlockPos position = BlockPos.containing(source.lerp(target, t));
            if (position.equals(previous) || !level.hasChunkAt(position)) {
                continue;
            }
            previous = position;
            var state = level.getBlockState(position);
            if (state.is(ModNuclearBlocks.LEAD_RADIATION_SHIELD.get())) {
                factor *= 0.05D;
            } else if (state.is(ModNuclearBlocks.REACTOR_CONTAINMENT_CASING.get())) {
                factor *= 0.45D;
            } else if (state.is(ModNuclearBlocks.REACTOR_PRESSURE_VESSEL.get())) {
                factor *= 0.6D;
            } else if (state.is(ModNuclearBlocks.FACILITY_CASING.get())) {
                factor *= 0.75D;
            } else if (state.is(Blocks.WATER)) {
                factor *= 0.88D;
            }
            if (factor < 0.0001D) {
                return 0.0001D;
            }
        }
        return factor;
    }
}
