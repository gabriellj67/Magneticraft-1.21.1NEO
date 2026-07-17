package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationSourceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import org.jetbrains.annotations.Nullable;

/** Persisted role-bound facility penetration. Non-role sides expose no capability. */
public final class NuclearThermalPortBlockEntity extends MachineBlockEntity implements RadiationSource {
    private static final String CONTROLLER_TAG = "controller";
    private static final String ROLE_TAG = "role";
    private static final int FLUID_CAPACITY = 128_000;

    private final FluidTankModule tank;
    private final ElectricalNetworkModule electricity;
    private final ElectricalPowerModule power;
    private final HeatNetworkModule heat;
    @Nullable
    private BlockPos controllerPosition;
    @Nullable
    private NuclearThermalPortRole role;

    public NuclearThermalPortBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.NUCLEAR_THERMAL_PORT.get(), position, state);
        tank = addModule(FluidTankModule.directional(
                Magneticraft.id("thermal_port_tank"), this, FLUID_CAPACITY,
                this::validFluid,
                side -> controllerPosition != null && role != null && side == outwardFacing()
                        ? role.fluidAccess() : FluidTankModule.TankAccess.NONE
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("thermal_port_electricity"), this, VoltageTierIds.MEDIUM,
                ElectricalNodeKind.MACHINE,
                side -> controllerPosition != null && role == NuclearThermalPortRole.ELECTRICAL
                        && side == outwardFacing()
        ));
        power = addModule(new ElectricalPowerModule(
                Magneticraft.id("thermal_port_energy"), Magneticraft.id("nuclear_facility"),
                this, electricity, ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false, false
        ));
        heat = addModule(new HeatNetworkModule(
                Magneticraft.id("thermal_port_heat"), this,
                new HeatNode(32.0D, 2_048.0D), Double.MAX_VALUE,
                side -> controllerPosition != null && role == NuclearThermalPortRole.HEAT
                        && side == outwardFacing()
        ));
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, NuclearThermalPortBlockEntity port
    ) {
        port.tickModules();
        port.finishServerTick();
    }

    public void claim(BlockPos controller, NuclearThermalPortRole nextRole) {
        if (controller.equals(controllerPosition) && nextRole == role) {
            return;
        }
        controllerPosition = controller.immutable();
        role = nextRole;
        invalidateCaps();
        reviveCaps();
        markChangedAndSync();
    }

    public void release(BlockPos controller) {
        if (!controller.equals(controllerPosition)) {
            return;
        }
        controllerPosition = null;
        role = null;
        invalidateCaps();
        reviveCaps();
        markChangedAndSync();
    }

    public boolean claimedBy(BlockPos controller, NuclearThermalPortRole expectedRole) {
        return controller.equals(controllerPosition) && role == expectedRole;
    }

    public int fluidAmount(Fluid fluid) {
        FluidStack stored = tank.tank().getFluid();
        return !stored.isEmpty() && stored.getFluid() == fluid ? stored.getAmount() : 0;
    }

    @Nullable
    public Fluid storedFluid() {
        return tank.tank().getFluid().isEmpty() ? null : tank.tank().getFluid().getFluid();
    }

    public int storedFluidAmount() {
        return tank.tank().getFluidAmount();
    }

    public int fluidSpace(Fluid fluid) {
        FluidStack stored = tank.tank().getFluid();
        return stored.isEmpty() || stored.getFluid() == fluid
                ? tank.tank().getCapacity() - stored.getAmount() : 0;
    }

    public int drain(Fluid fluid, int amount) {
        return amount > 0 && fluidAmount(fluid) > 0
                ? tank.tank().drain(amount, IFluidHandler.FluidAction.EXECUTE).getAmount() : 0;
    }

    public int fill(Fluid fluid, int amount) {
        return amount <= 0 ? 0 : tank.tank().fill(new FluidStack(fluid, amount), IFluidHandler.FluidAction.EXECUTE);
    }

    public int consumeJoules(int requested, boolean simulate) {
        return role == NuclearThermalPortRole.ELECTRICAL ? power.consumeJoules(requested, simulate) : 0;
    }

    public int storedJoules() {
        return role == NuclearThermalPortRole.ELECTRICAL ? power.storedWholeJoules() : 0;
    }

    public int capacityJoules() {
        return role == NuclearThermalPortRole.ELECTRICAL ? power.ratedCapacityWholeJoules() : 0;
    }

    public HeatNetworkModule heat() {
        return heat;
    }

    @Nullable
    public NuclearThermalPortRole role() {
        return role;
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            RadiationSourceRegistry.register(serverLevel, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            RadiationSourceRegistry.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    @Override public BlockPos radiationOrigin() { return worldPosition; }

    @Override
    public double doseRateMillisievertsPerHour() {
        return fluidAmount(ModFluids.get(FluidDefinition.HOT_REACTOR_COOLANT).source().get())
                / (double) FLUID_CAPACITY
                * ReactorParameterRegistry.INSTANCE.current().parameters().hotCoolantDoseRateMillisievertsPerHour();
    }

    @Override public boolean contaminationSource() { return false; }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        if (controllerPosition != null) tag.putLong(CONTROLLER_TAG, controllerPosition.asLong());
        if (role != null) tag.putString(ROLE_TAG, role.name());
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        controllerPosition = tag.contains(CONTROLLER_TAG, Tag.TAG_LONG)
                ? BlockPos.of(tag.getLong(CONTROLLER_TAG)) : null;
        role = tag.contains(ROLE_TAG, Tag.TAG_STRING) ? parseRole(tag.getString(ROLE_TAG)) : null;
    }

    private boolean validFluid(FluidStack stack) {
        Fluid fluid = stack.getFluid();
        return fluid.defaultFluidState().is(FluidTags.WATER)
                || fluid == ModFluids.get(FluidDefinition.HOT_REACTOR_COOLANT).source().get()
                || fluid == ModFluids.get(FluidDefinition.COLD_REACTOR_COOLANT).source().get()
                || fluid == ModFluids.get(FluidDefinition.STEAM).source().get()
                || fluid == ModFluids.get(FluidDefinition.LOW_PRESSURE_EXHAUST_STEAM).source().get();
    }

    private Direction outwardFacing() {
        return getBlockState().hasProperty(NuclearThermalPortBlock.FACING)
                ? getBlockState().getValue(NuclearThermalPortBlock.FACING) : Direction.NORTH;
    }

    @Nullable
    private static NuclearThermalPortRole parseRole(String value) {
        try {
            return NuclearThermalPortRole.valueOf(value);
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }
}
