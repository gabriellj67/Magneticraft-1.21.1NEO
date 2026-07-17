package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorPortType;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Claimed structure penetration; only the outer electrical port owns native-J connectivity. */
public final class NuclearReactorPortBlockEntity extends MachineBlockEntity {
    private static final String CONTROLLER_TAG = "controller";
    private static final String CLAIMED_ROLE_TAG = "claimed_role";
    private static final int COOLANT_CAPACITY = 64_000;

    private final NuclearReactorPortType blockPortType;
    @Nullable
    private final ElectricalNetworkModule electricity;
    @Nullable
    private final ElectricalPowerModule power;
    @Nullable
    private final FluidTankModule coolant;
    @Nullable
    private BlockPos controllerPosition;
    @Nullable
    private NuclearReactorPortType claimedRole;

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
        coolant = portType == NuclearReactorPortType.COOLANT_INPUT
                ? addModule(FluidTankModule.directional(
                Magneticraft.id("reactor_coolant"), this, COOLANT_CAPACITY,
                stack -> stack.getFluid() == ModFluids.get(FluidDefinition.COLD_REACTOR_COOLANT).source().get()
                        || stack.getFluid() == ModFluids.get(FluidDefinition.HOT_REACTOR_COOLANT).source().get(),
                side -> side == outwardFacing() ? coolantAccess() : FluidTankModule.TankAccess.NONE
        )) : null;
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

    public void claim(BlockPos controller, NuclearReactorPortType role) {
        if (blockPortType == NuclearReactorPortType.COOLANT_INPUT) {
            if (role != NuclearReactorPortType.COOLANT_INPUT && role != NuclearReactorPortType.COOLANT_OUTPUT) {
                throw new IllegalArgumentException("Coolant penetration cannot claim role " + role);
            }
        } else if (blockPortType != role) {
            throw new IllegalArgumentException(blockPortType + " penetration cannot claim role " + role);
        }
        BlockPos next = controller.immutable();
        if (next.equals(controllerPosition) && role == claimedRole) {
            return;
        }
        controllerPosition = next;
        claimedRole = role;
        if (electricity != null) {
            electricity.setSideEnabled(outwardFacing(), true);
        }
        refreshCapabilities();
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
        claimedRole = null;
        refreshCapabilities();
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

    public int coolantAmount(FluidDefinition definition) {
        if (coolant == null || coolant.tank().getFluid().isEmpty()
                || coolant.tank().getFluid().getFluid() != ModFluids.get(definition).source().get()) {
            return 0;
        }
        return coolant.tank().getFluidAmount();
    }

    public int coolantSpace(FluidDefinition definition) {
        if (coolant == null) {
            return 0;
        }
        FluidStack stored = coolant.tank().getFluid();
        return stored.isEmpty() || stored.getFluid() == ModFluids.get(definition).source().get()
                ? coolant.tank().getCapacity() - stored.getAmount() : 0;
    }

    public int drainCoolant(FluidDefinition definition, int amount) {
        if (coolantAmount(definition) <= 0 || amount <= 0) {
            return 0;
        }
        return coolant.tank().drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount();
    }

    public int fillCoolant(FluidDefinition definition, int amount) {
        if (coolant == null || amount <= 0) {
            return 0;
        }
        return coolant.tank().fill(
                new FluidStack(ModFluids.get(definition).source().get(), amount),
                IFluidHandler.FluidAction.EXECUTE
        );
    }

    @Nullable
    public NuclearReactorPortType claimedRole() {
        return claimedRole;
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
        if (claimedRole != null) {
            tag.putString(CLAIMED_ROLE_TAG, claimedRole.name());
        }
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        controllerPosition = tag.contains(CONTROLLER_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(CONTROLLER_TAG)) : null;
        claimedRole = tag.contains(CLAIMED_ROLE_TAG, Tag.TAG_STRING)
                ? parseRole(tag.getString(CLAIMED_ROLE_TAG)) : null;
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

    private FluidTankModule.TankAccess coolantAccess() {
        if (controllerPosition == null || claimedRole == null) {
            return FluidTankModule.TankAccess.NONE;
        }
        return claimedRole == NuclearReactorPortType.COOLANT_INPUT
                ? FluidTankModule.TankAccess.INPUT
                : claimedRole == NuclearReactorPortType.COOLANT_OUTPUT
                ? FluidTankModule.TankAccess.OUTPUT
                : FluidTankModule.TankAccess.NONE;
    }

    private void refreshCapabilities() {
        invalidateCaps();
        reviveCaps();
    }

    @Nullable
    private static NuclearReactorPortType parseRole(String value) {
        try {
            return NuclearReactorPortType.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    private static NuclearReactorPortType portType(BlockState state) {
        if (state.getBlock() instanceof NuclearReactorPortBlock port) {
            return port.portType();
        }
        throw new IllegalArgumentException("Reactor port entity requires a reactor port block");
    }
}
