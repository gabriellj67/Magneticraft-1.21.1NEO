package committee.nova.mods.magneticraft.content.nuclear.facility;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.capabilities.RegisterCapabilitiesEvent;
import net.neoforged.neoforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Exact-face structure port. Item members forward storage; the electrical member owns native J. */
public final class NuclearFacilityPortBlockEntity extends MachineBlockEntity {
    private static final String CONTROLLER_TAG = "controller";

    private final NuclearFacilityPartRole role;
    @Nullable
    private final ElectricalNetworkModule electricity;
    @Nullable
    private final ElectricalPowerModule power;
    @Nullable
    private final IItemHandler forwardingItemHandler;
    @Nullable
    private BlockPos controllerPosition;

    public NuclearFacilityPortBlockEntity(BlockPos position, BlockState state) {
        this(position, state, role(state));
    }

    private NuclearFacilityPortBlockEntity(
            BlockPos position, BlockState state, NuclearFacilityPartRole role
    ) {
        super(ModBlockEntities.NUCLEAR_FACILITY_PORT.get(), position, state);
        this.role = role;
        if (role == NuclearFacilityPartRole.ELECTRICAL) {
            electricity = addModule(new ElectricalNetworkModule(
                    Magneticraft.id("electricity"),
                    this,
                    VoltageTierIds.MEDIUM,
                    ElectricalNodeKind.MACHINE,
                    side -> side == outwardFacing()
            ));
            power = addModule(new ElectricalPowerModule(
                    Magneticraft.id("energy_storage"),
                    Magneticraft.id("nuclear_facility"),
                    this,
                    electricity,
                    ElectricalPowerModule.ForgeEnergyAccess.NONE,
                    side -> false,
                    false
            ));
            forwardingItemHandler = null;
        } else {
            electricity = null;
            power = null;
            forwardingItemHandler = new ForwardingItemHandler();
        }
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearFacilityPortBlockEntity port
    ) {
        port.tickModules();
        port.finishServerTick();
    }

    /** Registers the item-handler and energy-storage views for one {@link NuclearFacilityPortBlockEntity} type. */
    public static void registerCapabilities(
            RegisterCapabilitiesEvent event, BlockEntityType<NuclearFacilityPortBlockEntity> type
    ) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.itemHandler(side));
        event.registerBlockEntity(Capabilities.EnergyStorage.BLOCK, type, (be, side) -> be.exposedEnergyStorage(side));
    }

    public NuclearFacilityPartRole role() {
        return role;
    }

    public void claim(BlockPos controller) {
        BlockPos next = controller.immutable();
        if (!next.equals(controllerPosition)) {
            controllerPosition = next;
            invalidateItemCapability();
            markChangedAndSync();
        }
    }

    public void release(BlockPos controller) {
        if (controller.equals(controllerPosition)) {
            controllerPosition = null;
            invalidateItemCapability();
            markChangedAndSync();
        }
    }

    public boolean claimedBy(BlockPos controller) {
        return controller.equals(controllerPosition);
    }

    public int consumeJoules(int requested, boolean simulate) {
        return controllerPosition != null && power != null ? power.consumeJoules(requested, simulate) : 0;
    }

    public int storedJoules() {
        return power == null ? 0 : power.storedWholeJoules();
    }

    public int capacityJoules() {
        return power == null ? 0 : power.ratedCapacityWholeJoules();
    }

    @Override
    protected void saveMachineData(CompoundTag tag, HolderLookup.Provider registries) {
        if (controllerPosition != null) {
            tag.putLong(CONTROLLER_TAG, controllerPosition.asLong());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag, HolderLookup.Provider registries) {
        controllerPosition = tag.contains(CONTROLLER_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(CONTROLLER_TAG))
                : null;
    }

    @Nullable
    private IItemHandler itemHandler(@Nullable Direction side) {
        return forwardingItemHandler != null && controllerPosition != null && side == outwardFacing()
                ? forwardingItemHandler
                : null;
    }

    private void invalidateItemCapability() {
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    private Direction outwardFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(NuclearFacilityPortBlock.FACING)
                ? state.getValue(NuclearFacilityPortBlock.FACING)
                : Direction.NORTH;
    }

    @Nullable
    private IItemHandler delegate() {
        Level currentLevel = getLevel();
        if (currentLevel == null || controllerPosition == null || !currentLevel.hasChunkAt(controllerPosition)) {
            return null;
        }
        if (currentLevel.getBlockEntity(controllerPosition) instanceof NuclearFacilityControllerBlockEntity controller
                && controller.formed()) {
            return controller.portInventory(role);
        }
        return null;
    }

    private static NuclearFacilityPartRole role(BlockState state) {
        if (state.getBlock() instanceof NuclearFacilityPortBlock port) {
            return port.role();
        }
        throw new IllegalArgumentException("Nuclear facility port entity requires a port block");
    }

    private final class ForwardingItemHandler implements IItemHandler {
        @Override
        public int getSlots() {
            IItemHandler delegate = delegate();
            return delegate == null ? 0 : delegate.getSlots();
        }

        @Override
        public ItemStack getStackInSlot(int slot) {
            IItemHandler delegate = delegate();
            return delegate == null || slot < 0 || slot >= delegate.getSlots()
                    ? ItemStack.EMPTY : delegate.getStackInSlot(slot);
        }

        @Override
        public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            IItemHandler delegate = delegate();
            return delegate == null || slot < 0 || slot >= delegate.getSlots()
                    ? stack : delegate.insertItem(slot, stack, simulate);
        }

        @Override
        public ItemStack extractItem(int slot, int amount, boolean simulate) {
            IItemHandler delegate = delegate();
            return delegate == null || slot < 0 || slot >= delegate.getSlots()
                    ? ItemStack.EMPTY : delegate.extractItem(slot, amount, simulate);
        }

        @Override
        public int getSlotLimit(int slot) {
            IItemHandler delegate = delegate();
            return delegate == null || slot < 0 || slot >= delegate.getSlots()
                    ? 0 : delegate.getSlotLimit(slot);
        }

        @Override
        public boolean isItemValid(int slot, ItemStack stack) {
            IItemHandler delegate = delegate();
            return delegate != null && slot >= 0 && slot < delegate.getSlots()
                    && delegate.isItemValid(slot, stack);
        }
    }
}
