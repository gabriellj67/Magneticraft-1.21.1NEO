package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Transactional 1 J : 1 FE output used by wired and wireless adapters. */
final class ElectricEnergyExporter {
    private ElectricEnergyExporter() {
    }

    static int export(
            ServerLevel level,
            BlockPos position,
            Direction outwardFacing,
            ElectricalNode node,
            VoltageTier tier,
            int maximumTransfer
    ) {
        if (maximumTransfer <= 0 || node.voltage() <= tier.minimumOperatingVoltage()) {
            return 0;
        }
        double fraction = Math.min(
                1.0D,
                Math.max(0.0D, (node.voltage() - tier.minimumOperatingVoltage())
                        / (tier.nominalVoltage() - tier.minimumOperatingVoltage()))
        );
        int rate = (int) Math.floor(fraction * maximumTransfer);
        int available = (int) Math.floor(node.removeEnergy(rate, true));
        if (available <= 0) {
            return 0;
        }

        BlockPos targetPosition = position.relative(outwardFacing);
        if (level.getChunkSource().getChunkNow(targetPosition.getX() >> 4, targetPosition.getZ() >> 4) == null) {
            return 0;
        }
        var target = level.getBlockEntity(targetPosition);
        if (target == null) {
            return 0;
        }
        return target.getCapability(ForgeCapabilities.ENERGY, outwardFacing.getOpposite()).map(storage -> {
            int accepted = storage.receiveEnergy(available, true);
            if (accepted <= 0) {
                return 0;
            }
            int inserted = storage.receiveEnergy(accepted, false);
            if (inserted > 0) {
                node.removeEnergy(inserted, false);
            }
            return inserted;
        }).orElse(0);
    }
}
