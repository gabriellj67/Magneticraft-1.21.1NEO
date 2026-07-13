package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

/** Transactional 1 J : 1 FE output used by wired and wireless adapters. */
final class ElectricEnergyExporter {
    private static final double MINIMUM_VOLTAGE = 60.0D;
    private static final double MAXIMUM_VOLTAGE = 120.0D;
    private static final int MAX_TRANSFER = 400;

    private ElectricEnergyExporter() {
    }

    static int export(ServerLevel level, BlockPos position, Direction outwardFacing, ElectricalNode node) {
        if (node.voltage() < MINIMUM_VOLTAGE) {
            return 0;
        }
        double fraction = Math.min(
                1.0D,
                Math.max(0.0D, (node.voltage() - MINIMUM_VOLTAGE) / (MAXIMUM_VOLTAGE - MINIMUM_VOLTAGE))
        );
        int rate = (int) Math.floor(fraction * MAX_TRANSFER);
        int available = (int) Math.floor(node.removeEnergy(rate, true));
        if (available <= 0) {
            return 0;
        }

        Direction towardTarget = outwardFacing.getOpposite();
        BlockPos targetPosition = position.relative(towardTarget);
        if (!level.hasChunkAt(targetPosition)) {
            return 0;
        }
        var target = level.getBlockEntity(targetPosition);
        if (target == null) {
            return 0;
        }
        return target.getCapability(ForgeCapabilities.ENERGY, outwardFacing).map(storage -> {
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
