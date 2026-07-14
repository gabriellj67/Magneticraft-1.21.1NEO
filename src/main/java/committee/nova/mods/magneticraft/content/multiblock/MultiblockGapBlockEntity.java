package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost;
import committee.nova.mods.magneticraft.content.network.module.AbstractPhysicalNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/** Formed-structure proxy that preserves the released 1.12 per-position port behavior. */
public final class MultiblockGapBlockEntity extends BlockEntity
        implements MachineModuleHost, NetworkConnectionHost {
    private final PhysicalPortModule electricity = new PhysicalPortModule(NetworkDomain.ELECTRICITY);
    private final PhysicalPortModule heat = new PhysicalPortModule(NetworkDomain.HEAT);

    public MultiblockGapBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.MULTIBLOCK_GAP.get(), position, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (!electricity.connectionSides().isEmpty()) {
            electricity.onLoad();
        }
        if (!heat.connectionSides().isEmpty()) {
            heat.onLoad();
        }
    }

    @Override
    public void setRemoved() {
        electricity.onUnload();
        heat.onUnload();
        super.setRemoved();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        AdvancedMultiblockBlockEntity controller = controller();
        if (controller != null && side != null) {
            LazyOptional<T> result = controller.portCapability(worldPosition, side, capability);
            if (result.isPresent()) {
                return result;
            }
        }
        return super.getCapability(capability, side);
    }

    @Override
    public boolean supportsNetworkConnection(NetworkDomain domain, Direction side) {
        AdvancedMultiblockBlockEntity controller = controller();
        return controller != null
                && controller.operational()
                && MultiblockPortLayout.supports(controller, worldPosition, domain, side);
    }

    @Nullable
    private AdvancedMultiblockBlockEntity controller() {
        if (!(level instanceof ServerLevel serverLevel)) {
            return null;
        }
        BlockPos controllerPosition = MultiblockMembershipService.controllerAt(serverLevel, worldPosition);
        if (controllerPosition == null
                || !serverLevel.hasChunk(controllerPosition.getX() >> 4, controllerPosition.getZ() >> 4)) {
            return null;
        }
        return serverLevel.getBlockEntity(controllerPosition) instanceof AdvancedMultiblockBlockEntity controller
                ? controller
                : null;
    }

    @Override
    public void markChanged() {
        setChanged();
    }

    @Override
    public void markChangedAndSync() {
        setChanged();
    }

    @Nullable
    @Override
    public Level level() {
        return level;
    }

    @Override
    public BlockPos position() {
        return worldPosition;
    }

    private final class PhysicalPortModule extends AbstractPhysicalNetworkModule {
        private PhysicalPortModule(NetworkDomain domain) {
            super(Magneticraft.id("multiblock_" + domain.name().toLowerCase() + "_port"),
                    MultiblockGapBlockEntity.this, domain);
        }

        @Override
        protected boolean supportsSide(Direction direction) {
            AdvancedMultiblockBlockEntity controller = controller();
            return controller != null
                    && MultiblockPortLayout.supports(controller, worldPosition, domain(), direction);
        }

        @Override
        public PhysicalNetworkNode transferNode() {
            AdvancedMultiblockBlockEntity controller = controller();
            if (controller == null || !controller.operational()) {
                return this;
            }
            return switch (domain()) {
                case ELECTRICITY -> controller.electricity() == null ? this : controller.electricity();
                case HEAT -> controller.heat() == null ? this : controller.heat();
                default -> this;
            };
        }

        @Override
        public void exchangeWith(PhysicalNetworkNode other) {
            PhysicalNetworkNode delegate = transferNode();
            if (delegate != this) {
                delegate.exchangeWith(other.transferNode());
            }
        }
    }
}
