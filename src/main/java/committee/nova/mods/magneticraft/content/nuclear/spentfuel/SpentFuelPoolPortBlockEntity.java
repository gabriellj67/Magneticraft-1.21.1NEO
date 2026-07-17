package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.items.IItemHandler;
import org.jetbrains.annotations.Nullable;

/** Non-owning item view plus real heat-network node on the exact outward face. */
public final class SpentFuelPoolPortBlockEntity extends MachineBlockEntity {
    private final HeatNetworkModule heat;
    private LazyOptional<IItemHandler> items = LazyOptional.empty();
    @Nullable private BlockPos controllerPosition;

    public SpentFuelPoolPortBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.SPENT_FUEL_POOL_PORT.get(), position, state);
        heat = addModule(new HeatNetworkModule(
                Magneticraft.id("spent_fuel_pool_heat"), this, new HeatNode(64, 4096), Double.MAX_VALUE,
                side -> controllerPosition != null && side == outwardFacing()));
        reviveItemCapability();
    }

    public static void serverTick(Level level, BlockPos pos, BlockState state, SpentFuelPoolPortBlockEntity port) {
        port.tickModules();
        port.finishServerTick();
    }

    public void claim(BlockPos controller) {
        if (controller.equals(controllerPosition)) return;
        controllerPosition = controller.immutable();
        invalidateCaps();
        reviveCaps();
        markChangedAndSync();
    }

    public void release(BlockPos controller) {
        if (!controller.equals(controllerPosition)) return;
        controllerPosition = null;
        invalidateCaps();
        reviveCaps();
        markChangedAndSync();
    }

    public boolean claimedBy(BlockPos controller) { return controller.equals(controllerPosition); }
    public HeatNetworkModule heat() { return heat; }

    @Override public void invalidateCaps() {
        items.invalidate();
        items = LazyOptional.empty();
        super.invalidateCaps();
    }

    @Override public void reviveCaps() {
        super.reviveCaps();
        reviveItemCapability();
    }

    @Override
    public <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        if (capability == ForgeCapabilities.ITEM_HANDLER && controllerPosition != null
                && side != null && side == outwardFacing()) {
            return items.cast();
        }
        return super.getCapability(capability, side);
    }

    @Override protected void saveMachineData(CompoundTag tag) {
        if (controllerPosition != null) tag.putLong("controller", controllerPosition.asLong());
    }
    @Override protected void loadMachineData(CompoundTag tag) {
        controllerPosition = tag.contains("controller", Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong("controller")) : null;
    }

    private void reviveItemCapability() {
        items = LazyOptional.of(() -> new IItemHandler() {
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
        });
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
