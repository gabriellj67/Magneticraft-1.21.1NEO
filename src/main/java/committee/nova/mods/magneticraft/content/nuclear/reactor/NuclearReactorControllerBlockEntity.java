package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorSnapshot;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.system.nuclear.data.NuclearDataRegistry;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutEstimate;
import committee.nova.mods.magneticraft.system.nuclear.reactor.ReactorLayoutSimulator;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;
import java.util.UUID;

/** Owns the immutable formed snapshot and the structure-change-only static estimate. */
public final class NuclearReactorControllerBlockEntity extends MachineBlockEntity implements MenuProvider {
    private static final int STRUCTURE_CHECK_INTERVAL = 40;
    private static final String OWNER_TAG = "owner";
    private static final String SNAPSHOT_TAG = "reactor_snapshot";

    @Nullable
    private UUID owner;
    @Nullable
    private NuclearReactorSnapshot snapshot;
    private ReactorLayoutEstimate estimate = ReactorLayoutEstimate.empty("unformed");
    private long estimateGeneration = Long.MIN_VALUE;

    public NuclearReactorControllerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.NUCLEAR_REACTOR_CONTROLLER.get(), position, state);
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearReactorControllerBlockEntity controller
    ) {
        controller.tickModules();
        if (controller.snapshot != null && level.getGameTime() % STRUCTURE_CHECK_INTERVAL == 0L) {
            controller.revalidate();
            controller.refreshEstimateIfDataChanged();
        }
        controller.finishServerTick();
    }

    public Direction facing() {
        BlockState state = getBlockState();
        return state.hasProperty(NuclearReactorControllerBlock.FACING)
                ? state.getValue(NuclearReactorControllerBlock.FACING)
                : Direction.NORTH;
    }

    public boolean formed() {
        return snapshot != null;
    }

    public Optional<NuclearReactorSnapshot> snapshot() {
        return Optional.ofNullable(snapshot);
    }

    public ReactorLayoutEstimate estimate() {
        return estimate;
    }

    public void setOwner(UUID owner) {
        this.owner = owner;
        markChanged();
    }

    public boolean canManage(Player player) {
        return player.distanceToSqr(
                worldPosition.getX() + 0.5D,
                worldPosition.getY() + 0.5D,
                worldPosition.getZ() + 0.5D
        ) <= 64.0D && (owner == null || owner.equals(player.getUUID())
                || player instanceof ServerPlayer serverPlayer && serverPlayer.hasPermissions(2));
    }

    public boolean tryForm(ServerPlayer player) {
        NuclearReactorStructure.Result result = validateAny();
        if (result.snapshot().isEmpty()) {
            player.displayClientMessage(Component.translatable(
                    "message.magneticraft.reactor.invalid",
                    Component.translatable("message.magneticraft.reactor.reason." + result.reason()),
                    result.position().toShortString()
            ), true);
            return false;
        }
        applySnapshot(result.snapshot().orElseThrow());
        return true;
    }

    public void unform() {
        snapshot = null;
        estimate = ReactorLayoutEstimate.empty("unformed");
        updateFormedState(false);
        markChangedAndSync();
    }

    public void writeMenuData(FriendlyByteBuf buffer) {
        NuclearReactorMenu.write(buffer, worldPosition, snapshot, estimate);
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("block.magneticraft.pressurized_water_reactor_controller");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return canManage(player) && snapshot != null
                ? new NuclearReactorMenu(containerId, inventory, this)
                : null;
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        if (owner != null) {
            tag.putUUID(OWNER_TAG, owner);
        }
        if (snapshot != null) {
            tag.put(SNAPSHOT_TAG, NuclearReactorSnapshotCodec.save(snapshot));
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        owner = tag.hasUUID(OWNER_TAG) ? tag.getUUID(OWNER_TAG) : null;
        snapshot = tag.contains(SNAPSHOT_TAG, Tag.TAG_COMPOUND)
                ? NuclearReactorSnapshotCodec.load(tag.getCompound(SNAPSHOT_TAG), worldPosition).orElse(null)
                : null;
        estimate = ReactorLayoutEstimate.empty(snapshot == null ? "unformed" : "awaiting_nuclear_data");
        estimateGeneration = Long.MIN_VALUE;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide && snapshot != null) {
            refreshEstimate();
        }
    }

    private void revalidate() {
        NuclearReactorSnapshot current = snapshot;
        if (current == null) {
            return;
        }
        NuclearReactorStructure.Result result = NuclearReactorStructure.validateExact(
                worldPosition, facing(), current.width(), current.length(), current.height(), worldLookup());
        if (result.snapshot().isEmpty()) {
            unform();
            return;
        }
        NuclearReactorSnapshot valid = result.snapshot().orElseThrow();
        if (!valid.columns().equals(current.columns()) || valid.facing() != current.facing()) {
            applySnapshot(valid);
        }
    }

    private NuclearReactorStructure.Result validateAny() {
        return NuclearReactorStructure.validate(worldPosition, facing(), worldLookup());
    }

    private NuclearReactorStructure.PartLookup worldLookup() {
        return new NuclearReactorStructure.PartLookup() {
            @Nullable
            @Override
            public NuclearReactorStructure.ObservedPart partAt(BlockPos position) {
                return level == null ? null : observe(level.getBlockState(position));
            }

            @Override
            public boolean isLoaded(BlockPos position) {
                return level instanceof ServerLevel serverLevel && serverLevel.hasChunkAt(position);
            }
        };
    }

    private void applySnapshot(NuclearReactorSnapshot next) {
        snapshot = next;
        refreshEstimate();
        updateFormedState(true);
        markChangedAndSync();
    }

    private void refreshEstimate() {
        NuclearReactorSnapshot current = snapshot;
        if (current == null) {
            estimate = ReactorLayoutEstimate.empty("unformed");
            estimateGeneration = Long.MIN_VALUE;
            return;
        }
        NuclearDataRegistry.INSTANCE.current().ifPresentOrElse(data -> {
            estimate = ReactorLayoutSimulator.estimate(current, data.fuelDefinitions());
            estimateGeneration = data.generation();
        }, () -> {
            estimate = ReactorLayoutEstimate.empty("nuclear_data_unavailable");
            estimateGeneration = Long.MIN_VALUE;
        });
    }

    private void refreshEstimateIfDataChanged() {
        long currentGeneration = NuclearDataRegistry.INSTANCE.current()
                .map(data -> data.generation())
                .orElse(Long.MIN_VALUE);
        if (currentGeneration != estimateGeneration) {
            refreshEstimate();
            markChangedAndSync();
        }
    }

    private void updateFormedState(boolean formed) {
        if (level == null) {
            return;
        }
        BlockState state = getBlockState();
        if (state.hasProperty(NuclearReactorControllerBlock.FORMED)
                && state.getValue(NuclearReactorControllerBlock.FORMED) != formed) {
            level.setBlock(worldPosition, state.setValue(NuclearReactorControllerBlock.FORMED, formed),
                    Block.UPDATE_CLIENTS);
        }
    }

    @Nullable
    private NuclearReactorStructure.ObservedPart observe(BlockState state) {
        Block block = state.getBlock();
        if (block == ModNuclearBlocks.REACTOR_CONTROLLER.get()) {
            return new NuclearReactorStructure.ObservedPart(
                    NuclearReactorStructure.PartKind.CONTROLLER, null,
                    state.getValue(NuclearReactorControllerBlock.FACING));
        }
        if (block == ModNuclearBlocks.REACTOR_CONTAINMENT_CASING.get()) {
            return part(NuclearReactorStructure.PartKind.CONTAINMENT_CASING);
        }
        if (block == ModNuclearBlocks.REACTOR_PRESSURE_VESSEL.get()) {
            return part(NuclearReactorStructure.PartKind.PRESSURE_VESSEL);
        }
        if (block == ModNuclearBlocks.REACTOR_CONTROL_ROD_ACTUATOR.get()) {
            return part(NuclearReactorStructure.PartKind.CONTROL_ROD_ACTUATOR);
        }
        if (block == ModNuclearBlocks.REACTOR_COLUMN_SEGMENT.get()) {
            return part(NuclearReactorStructure.PartKind.COLUMN_SEGMENT);
        }
        if (block == ModNuclearBlocks.REACTOR_MAIN_COOLANT_PORT.get()) {
            return port(NuclearReactorStructure.PartKind.COOLANT_PORT, state);
        }
        if (block == ModNuclearBlocks.REACTOR_ELECTRICAL_PORT.get()) {
            return port(NuclearReactorStructure.PartKind.ELECTRICAL_PORT, state);
        }
        if (block == ModNuclearBlocks.REACTOR_INSTRUMENTATION_PORT.get()) {
            return port(NuclearReactorStructure.PartKind.INSTRUMENTATION_PORT, state);
        }
        if (block instanceof NuclearReactorColumnBlock column) {
            return new NuclearReactorStructure.ObservedPart(
                    NuclearReactorStructure.PartKind.COLUMN_BASE, column.columnType(), null);
        }
        return null;
    }

    private static NuclearReactorStructure.ObservedPart part(NuclearReactorStructure.PartKind kind) {
        return new NuclearReactorStructure.ObservedPart(kind, null, null);
    }

    private static NuclearReactorStructure.ObservedPart port(
            NuclearReactorStructure.PartKind kind, BlockState state
    ) {
        return new NuclearReactorStructure.ObservedPart(
                kind, null, state.getValue(NuclearReactorPortBlock.FACING));
    }
}
