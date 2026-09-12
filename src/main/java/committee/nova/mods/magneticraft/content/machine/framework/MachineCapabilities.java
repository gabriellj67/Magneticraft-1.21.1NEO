package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.world.level.block.entity.BlockEntityType;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;

/**
 * Wires the three module-backed capability views ({@link MachineBlockEntity#exposedItemHandler},
 * {@link MachineBlockEntity#exposedFluidHandler}, {@link MachineBlockEntity#exposedEnergyStorage})
 * for one machine block-entity type.
 *
 * <p>NeoForge requires a separate provider registration per (capability kind, block-entity type)
 * pair rather than the single generic {@code getCapability(Capability, Direction)} dispatch this
 * framework used under Forge, so every concrete {@link MachineBlockEntity} subclass's registry
 * bootstrap should call this once its {@code BlockEntityType} exists.</p>
 */
public final class MachineCapabilities {
    private MachineCapabilities() {
    }

    public static <BE extends MachineBlockEntity> void register(RegisterCapabilitiesEvent event, BlockEntityType<BE> type) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, MachineBlockEntity::exposedItemHandler);
        event.registerBlockEntity(Capabilities.FluidHandler.BLOCK, type, MachineBlockEntity::exposedFluidHandler);
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, MachineBlockEntity::exposedEnergyStorage);
    }
}
