package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.energy.IEnergyStorage;

/** Transactional 1 J : 1 FE output used by wired and wireless adapters. */
public final class ElectricEnergyExporter {
    private ElectricEnergyExporter() {
    }

    public static int export(
            ServerLevel level,
            BlockPos position,
            Direction outwardFacing,
            ElectricalNode node,
            VoltageTier tier,
            int maximumTransfer
    ) {
        if (maximumTransfer <= 0) {
            return 0;
        }
        double fraction = tier.operatingRateFraction(node.voltage());
        int rate = (int) Math.floor(fraction * maximumTransfer);
        return exportAtMost(level, position, outwardFacing, node, rate);
    }

    /** Executes a transactional J -> FE transfer whose voltage/rate limit is already resolved. */
    public static int exportAtMost(
            ServerLevel level,
            BlockPos position,
            Direction outwardFacing,
            ElectricalNode node,
            int maximumTransfer
    ) {
        int rate = Math.max(0, maximumTransfer);
        int available = (int) Math.floor(node.removeEnergy(rate, true));
        if (available <= 0) {
            return 0;
        }

        BlockPos targetPosition = position.relative(outwardFacing);
        if (level.getChunkSource().getChunkNow(targetPosition.getX() >> 4, targetPosition.getZ() >> 4) == null) {
            return 0;
        }
        BlockEntity target = level.getBlockEntity(targetPosition);
        if (target == null) {
            return 0;
        }
        Direction targetSide = outwardFacing.getOpposite();
        if (target instanceof NetworkConnectionHost host
                && host.supportsNetworkConnection(NetworkDomain.ELECTRICITY, targetSide)) {
            return 0;
        }
        IEnergyStorage storage = Capabilities.EnergyStorage.BLOCK.getCapability(
                level, targetPosition, target.getBlockState(), target, targetSide
        );
        if (storage == null) {
            return 0;
        }
        int accepted = Math.min(available, Math.max(0, storage.receiveEnergy(available, true)));
        if (accepted <= 0) {
            return 0;
        }
        int inserted = Math.min(accepted, Math.max(0, storage.receiveEnergy(accepted, false)));
        if (inserted > 0) {
            node.removeEnergy(inserted, false);
        }
        return inserted;
    }
}
