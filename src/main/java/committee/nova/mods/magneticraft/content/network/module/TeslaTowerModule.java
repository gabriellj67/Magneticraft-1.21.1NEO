package committee.nova.mods.magneticraft.content.network.module;

import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNode;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.common.capabilities.ForgeCapabilities;

import java.util.Objects;

/** Server-authoritative item charging and receiver transfer for a Tesla tower. */
public final class TeslaTowerModule implements MachineModule {
    public static final double RANGE_BLOCKS = 32.0D;
    public static final int TRANSFER_RATE = 500;
    public static final double MINIMUM_VOLTAGE = 60.0D;

    private final ResourceLocation id;
    private final MachineModuleHost host;
    private final ElectricalNode node;

    public TeslaTowerModule(ResourceLocation id, MachineModuleHost host, ElectricalNode node) {
        this.id = Objects.requireNonNull(id);
        this.host = Objects.requireNonNull(host);
        this.node = Objects.requireNonNull(node);
    }

    @Override
    public ResourceLocation id() {
        return id;
    }

    @Override
    public void serverTick() {
        if (!(host.level() instanceof ServerLevel level) || node.voltage() < MINIMUM_VOLTAGE) {
            return;
        }
        boolean moved = chargePlayerItems(level);
        if (node.voltage() >= MINIMUM_VOLTAGE) {
            moved |= chargeReceivers(level);
        }
        if (moved) {
            host.markChanged();
        }
    }

    private boolean chargePlayerItems(ServerLevel level) {
        boolean moved = false;
        for (Player player : level.players()) {
            if (!insideRange(player.getX(), player.getY(), player.getZ()) || player.isSpectator()) {
                continue;
            }
            for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
                if (node.voltage() < MINIMUM_VOLTAGE) {
                    return moved;
                }
                var stack = player.getInventory().getItem(slot);
                if (stack.isEmpty()) {
                    continue;
                }
                int available = (int) Math.floor(node.removeEnergy(TRANSFER_RATE, true));
                if (available <= 0) {
                    return moved;
                }
                int inserted = stack.getCapability(ForgeCapabilities.ENERGY)
                        .map(storage -> {
                            int accepted = storage.receiveEnergy(available, true);
                            return accepted <= 0 ? 0 : storage.receiveEnergy(accepted, false);
                        })
                        .orElse(0);
                if (inserted > 0) {
                    node.removeEnergy(inserted, false);
                    moved = true;
                }
            }
        }
        return moved;
    }

    private boolean chargeReceivers(ServerLevel level) {
        boolean moved = false;
        for (var receiver : LongDistanceElectricityService.get(level).receiversWithin(host.position(), RANGE_BLOCKS)) {
            if (node.voltage() < MINIMUM_VOLTAGE) {
                break;
            }
            double available = node.removeEnergy(TRANSFER_RATE, true);
            double accepted = receiver.electricity().node().addEnergy(available, true);
            if (accepted <= 0.0D) {
                continue;
            }
            double removed = node.removeEnergy(accepted, false);
            double inserted = receiver.electricity().node().addEnergy(removed, false);
            if (inserted < removed) {
                node.addEnergy(removed - inserted, false);
            }
            if (inserted > 0.0D) {
                receiver.markChanged();
                moved = true;
            }
        }
        return moved;
    }

    private boolean insideRange(double x, double y, double z) {
        return Math.abs(x - (host.position().getX() + 0.5D)) <= RANGE_BLOCKS
                && Math.abs(y - (host.position().getY() + 0.5D)) <= RANGE_BLOCKS
                && Math.abs(z - (host.position().getZ() + 0.5D)) <= RANGE_BLOCKS;
    }
}
