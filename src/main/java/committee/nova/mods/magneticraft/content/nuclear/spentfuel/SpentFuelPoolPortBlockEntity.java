package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
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

/** Non-owning item view plus real heat-network node on the exact outward face. */
public final class SpentFuelPoolPortBlockEntity extends MachineBlockEntity {
    private final HeatNetworkModule heat;
    private final IItemHandler itemHandler = new IItemHandler() {
        @Override public int getSlots() { return SpentFuelPoolControllerBlockEntity.SLOTS; }
        @Override public ItemStack getStackInSlot(int slot) {
            SpentFuelPoolControllerBlockEntity controller = controller();
            return controller == null ? ItemStack.EMPTY : controller.stack(slot);
        }
        @Override public ItemStack insertItem(int slot, ItemStack stack, boolean simulate) {
            SpentFuelPoolControllerBlockEntity controller = controller();
            return controller == null ? stack : controller.insertFuel(slot, stack, simulate);
        }
        @Override public ItemStack extractItem(int slot, int amount, boolean simulate) {
            SpentFuelPoolControllerBlockEntity controller = controller();
            return controller == null ? ItemStack.EMPTY : controller.extractCooledFuel(slot, amount, simulate);
        }
        @Override public int getSlotLimit(int slot) { return 1; }
        @Override public boolean isItemValid(int slot, ItemStack stack) {
            SpentFuelPoolControllerBlockEntity controller = controller();
            return controller != null && controller.isSpentFuel(stack);
        }
    };
    @Nullable private BlockPos controllerPosition;

    public SpentFuelPoolPortBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SPENT_FUEL_POOL_PORT.get(), position, state);
        heat = addModule(new HeatNetworkModule(
                Magneticraft.id("spent_fuel_pool_heat"), this, new HeatNode(64, 4096), Double.MAX_VALUE,
                side -> controllerPosition != null && side == outwardFacing()));
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpentFuelPoolPortBlockEntity port) {
        port.tickModules();
        port.finishServerTick();
    }

    /** Registers the item-handler view for one {@link SpentFuelPoolPortBlockEntity} type. */
    public static void registerCapabilities(
            RegisterCapabilitiesEvent event, BlockEntityType<SpentFuelPoolPortBlockEntity> type
    ) {
        event.registerBlockEntity(Capabilities.ItemHandler.BLOCK, type, (be, side) -> be.itemHandler(side));
    }

    public void claim(BlockPos controller) {
        if (controller.equals(controllerPosition)) return;
        controllerPosition = controller.immutable();
        invalidateItemCapability();
        markChangedAndSync();
    }

    public void release(BlockPos controller) {
        if (!controller.equals(controllerPosition)) return;
        controllerPosition = null;
        invalidateItemCapability();
        markChangedAndSync();
    }

    public boolean claimedBy(BlockPos controller) { return controller.equals(controllerPosition); }
    public HeatNetworkModule heat() { return heat; }

    @Override protected void saveMachineData(CompoundTag tag, HolderLookup.Provider registries) {
        if (controllerPosition != null) tag.putLong("controller", controllerPosition.asLong());
    }
    @Override protected void loadMachineData(CompoundTag tag, HolderLookup.Provider registries) {
        controllerPosition = tag.contains("controller", Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong("controller")) : null;
    }

    @Nullable
    private IItemHandler itemHandler(@Nullable Direction side) {
        return controllerPosition != null && side != null && side == outwardFacing() ? itemHandler : null;
    }

    private void invalidateItemCapability() {
        if (level != null) {
            level.invalidateCapabilities(worldPosition);
        }
    }

    @Nullable private SpentFuelPoolControllerBlockEntity controller() {
        return level != null && controllerPosition != null
                && level.getBlockEntity(controllerPosition) instanceof SpentFuelPoolControllerBlockEntity controller
                && controller.formed() ? controller : null;
    }

    private Direction outwardFacing() {
        return getBlockState().getValue(SpentFuelPoolPortBlock.FACING);
    }
}
