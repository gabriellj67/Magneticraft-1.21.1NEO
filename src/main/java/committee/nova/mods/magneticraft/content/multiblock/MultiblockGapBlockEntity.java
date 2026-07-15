package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleHost;
import committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost;
import committee.nova.mods.magneticraft.content.network.module.AbstractPhysicalNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.diagnostic.DiagnosticHost;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.diagnostic.ThermalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkNode;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Formed-structure proxy that preserves the released 1.12 per-position port behavior. */
public final class MultiblockGapBlockEntity extends BlockEntity
        implements MachineModuleHost, NetworkConnectionHost, DiagnosticHost {
    private static final String CONTROLLER_TAG = "controller";
    private static final String DEFINITION_TAG = "definition";
    private static final String FACING_TAG = "facing";

    private final PhysicalPortModule electricity = new PhysicalPortModule(NetworkDomain.ELECTRICITY);
    private final PhysicalPortModule heat = new PhysicalPortModule(NetworkDomain.HEAT);
    @Nullable
    private BlockPos controllerPosition;
    @Nullable
    private MultiblockDefinition definition;
    @Nullable
    private Direction facing;

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

    void configure(
            BlockPos controllerPosition,
            MultiblockDefinition definition,
            Direction facing
    ) {
        if (facing.getAxis().isVertical()) {
            throw new IllegalArgumentException("Multiblock facing must be horizontal: " + facing);
        }
        BlockPos stableController = controllerPosition.immutable();
        if (stableController.equals(this.controllerPosition)
                && definition == this.definition
                && facing == this.facing) {
            return;
        }
        this.controllerPosition = stableController;
        this.definition = definition;
        this.facing = facing;
        markChangedAndSync();
    }

    VoxelShape collisionShape() {
        return controllerPosition == null || definition == null || facing == null
                ? Shapes.empty()
                : LegacyMultiblockCollision.shapeAt(
                worldPosition, controllerPosition, definition, facing
        );
    }

    @Override
    protected void saveAdditional(CompoundTag tag) {
        super.saveAdditional(tag);
        writeCollisionData(tag);
    }

    @Override
    public void load(CompoundTag tag) {
        super.load(tag);
        loadCollisionData(tag);
    }

    @Override
    public CompoundTag getUpdateTag() {
        CompoundTag tag = new CompoundTag();
        writeCollisionData(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag) {
        loadCollisionData(tag);
    }

    @Override
    public void onDataPacket(
            net.minecraft.network.Connection connection,
            ClientboundBlockEntityDataPacket packet
    ) {
        CompoundTag tag = packet.getTag();
        if (tag != null) {
            handleUpdateTag(tag);
        }
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket() {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    private void writeCollisionData(CompoundTag tag) {
        if (controllerPosition == null || definition == null || facing == null) {
            return;
        }
        tag.putLong(CONTROLLER_TAG, controllerPosition.asLong());
        tag.putString(DEFINITION_TAG, definition.id());
        tag.putInt(FACING_TAG, facing.get3DDataValue());
    }

    private void loadCollisionData(CompoundTag tag) {
        controllerPosition = tag.contains(CONTROLLER_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(CONTROLLER_TAG))
                : null;
        definition = tag.contains(DEFINITION_TAG, Tag.TAG_STRING)
                ? definition(tag.getString(DEFINITION_TAG))
                : null;
        Direction loadedFacing = tag.contains(FACING_TAG, Tag.TAG_INT)
                ? Direction.from3DDataValue(tag.getInt(FACING_TAG))
                : null;
        facing = loadedFacing != null && loadedFacing.getAxis().isHorizontal()
                ? loadedFacing
                : null;
    }

    @Nullable
    private static MultiblockDefinition definition(String id) {
        for (MultiblockDefinition candidate : MultiblockDefinition.values()) {
            if (candidate.id().equals(id)) {
                return candidate;
            }
        }
        return null;
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

    @Override
    public Optional<ElectricalDiagnosticSource.ElectricalReading> electricalReading(Direction side) {
        AdvancedMultiblockBlockEntity controller = controllerForPort(NetworkDomain.ELECTRICITY, side);
        return controller == null || controller.electricity() == null
                ? Optional.empty()
                : Optional.of(controller.electricity().displayReading());
    }

    @Override
    public Optional<ElectricalDiagnosticSource.NetworkSummary> electricalNetworkSummary(
            Direction side,
            int maxVisitedNodes
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || controllerForPort(NetworkDomain.ELECTRICITY, side) == null) {
            return Optional.empty();
        }
        return PhysicalNetworkService.manager(serverLevel)
                .electricalNetworkSummary(electricity.nodeKey(), maxVisitedNodes);
    }

    @Override
    public Optional<ElectricalDiagnosticSource.FaultSearchResult> nearestElectricalFault(
            Direction side,
            int maxVisitedNodes
    ) {
        if (!(level instanceof ServerLevel serverLevel)
                || controllerForPort(NetworkDomain.ELECTRICITY, side) == null) {
            return Optional.empty();
        }
        return PhysicalNetworkService.manager(serverLevel)
                .nearestElectricalFault(electricity.nodeKey(), maxVisitedNodes);
    }

    @Override
    public Optional<ThermalDiagnosticSource.ThermalReading> thermalReading(Direction side) {
        AdvancedMultiblockBlockEntity controller = controllerForPort(NetworkDomain.HEAT, side);
        return controller == null ? Optional.empty() : controller.thermalReading(side);
    }

    @Nullable
    private AdvancedMultiblockBlockEntity controllerForPort(NetworkDomain domain, Direction side) {
        AdvancedMultiblockBlockEntity controller = controller();
        return controller != null
                && controller.operational()
                && MultiblockPortLayout.supports(controller, worldPosition, domain, side)
                ? controller
                : null;
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
        if (level != null && !level.isClientSide) {
            level.sendBlockUpdated(worldPosition, getBlockState(), getBlockState(), 3);
        }
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
