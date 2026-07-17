package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/** Claimed structure penetration; only the outer electrical port owns native-J connectivity. */
public final class NuclearReactorPortBlockEntity extends MachineBlockEntity {
    private static final String CONTROLLER_TAG = "controller";

    private final NuclearReactorPortType blockPortType;
    @Nullable
    private final ElectricalNetworkModule electricity;
    @Nullable
    private final ElectricalPowerModule power;
    @Nullable
    private BlockPos controllerPosition;

    public NuclearReactorPortBlockEntity(BlockPos position, BlockState state) {
        this(position, state, portType(state));
    }

    private NuclearReactorPortBlockEntity(
            BlockPos position, BlockState state, NuclearReactorPortType portType
    ) {
        super(ModBlockEntities.NUCLEAR_REACTOR_PORT.get(), position, state);
        blockPortType = portType;
        if (portType == NuclearReactorPortType.ELECTRICAL) {
            electricity = addModule(new ElectricalNetworkModule(
                    Magneticraft.id("electricity"), this, VoltageTierIds.MEDIUM,
                    ElectricalNodeKind.MACHINE,
                    side -> controllerPosition != null && side == outwardFacing()
            ));
            power = addModule(new ElectricalPowerModule(
                    Magneticraft.id("energy_storage"), Magneticraft.id("nuclear_facility"),
                    this, electricity, ElectricalPowerModule.ForgeEnergyAccess.NONE,
                    side -> false, false
            ));
            electricity.setSideEnabled(outwardFacing(), false);
        } else {
            electricity = null;
            power = null;
        }
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearReactorPortBlockEntity port
    ) {
        port.tickModules();
        port.finishServerTick();
    }

    public NuclearReactorPortType blockPortType() {
        return blockPortType;
    }

    public void claim(BlockPos controller) {
        BlockPos next = controller.immutable();
        if (next.equals(controllerPosition)) {
            return;
        }
        controllerPosition = next;
        if (electricity != null) {
            electricity.setSideEnabled(outwardFacing(), true);
        }
        markChangedAndSync();
    }

    public void release(BlockPos controller) {
        if (!controller.equals(controllerPosition)) {
            return;
        }
        if (electricity != null) {
            electricity.setSideEnabled(outwardFacing(), false);
        }
        controllerPosition = null;
        markChangedAndSync();
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

    @Nullable
    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        if (controllerPosition != null) {
            tag.putLong(CONTROLLER_TAG, controllerPosition.asLong());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        controllerPosition = tag.contains(CONTROLLER_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(CONTROLLER_TAG)) : null;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (electricity != null) {
            electricity.setSideEnabled(outwardFacing(), controllerPosition != null);
        }
    }

    private Direction outwardFacing() {
        BlockState state = getBlockState();
        return state.hasProperty(NuclearReactorPortBlock.FACING)
                ? state.getValue(NuclearReactorPortBlock.FACING) : Direction.NORTH;
    }

    private static NuclearReactorPortType portType(BlockState state) {
        if (state.getBlock() instanceof NuclearReactorPortBlock port) {
            return port.portType();
        }
        throw new IllegalArgumentException("Reactor port entity requires a reactor port block");
    }
}
