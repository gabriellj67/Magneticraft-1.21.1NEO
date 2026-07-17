package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModFluids;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.thermal.NuclearThermalTransactions;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;

/** Lossless two-tank primary pump. It never exposes Forge Energy and never owns an FE buffer. */
public final class MainCoolantPumpBlockEntity extends MachineBlockEntity {
    private static final String FLOW_SETTING_TAG = "flow_setting";
    private static final int CAPACITY = 32_000;

    private final FluidTankModule input;
    private final FluidTankModule output;
    private final ElectricalNetworkModule electricity;
    private final ElectricalPowerModule power;
    private double flowSetting = 1.0D;
    private int lastFlow;

    public MainCoolantPumpBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.MAIN_COOLANT_PUMP.get(), position, state);
        input = addModule(FluidTankModule.directional(
                Magneticraft.id("main_coolant_pump_input"), this, CAPACITY, this::isColdCoolant,
                side -> side == facing().getOpposite()
                        ? FluidTankModule.TankAccess.INPUT : FluidTankModule.TankAccess.NONE));
        output = addModule(FluidTankModule.directional(
                Magneticraft.id("main_coolant_pump_output"), this, CAPACITY, this::isColdCoolant,
                side -> side == facing()
                        ? FluidTankModule.TankAccess.OUTPUT : FluidTankModule.TankAccess.NONE));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("main_coolant_pump_electricity"), this, VoltageTierIds.MEDIUM,
                ElectricalNodeKind.MACHINE,
                side -> side != facing() && side != facing().getOpposite()));
        power = addModule(new ElectricalPowerModule(
                Magneticraft.id("main_coolant_pump_energy"), Magneticraft.id("nuclear_facility"),
                this, electricity, ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false, false));
    }

    public static void serverTick(
            Level level, BlockPos position, BlockState state, MainCoolantPumpBlockEntity pump
    ) {
        pump.tickModules();
        pump.tickPump();
        pump.finishServerTick();
    }

    public void cycleFlowSetting() {
        flowSetting = flowSetting >= 1.0D ? 0.25D : Math.min(1.0D, flowSetting + 0.25D);
        markChangedAndSync();
    }

    public double effectiveFlowSetting() {
        if (level == null) return flowSetting;
        int signal = level.getBestNeighborSignal(worldPosition);
        return signal > 0 ? signal / 15.0D : flowSetting;
    }

    public int lastFlow() {
        return lastFlow;
    }

    public int storedJoules() {
        return power.storedWholeJoules();
    }

    public int capacityJoules() {
        return power.ratedCapacityWholeJoules();
    }

    private void tickPump() {
        var transfer = NuclearThermalTransactions.pump(
                input.tank().getFluidAmount(),
                output.tank().getCapacity() - output.tank().getFluidAmount(),
                power.storedWholeJoules(), effectiveFlowSetting(),
                ReactorParameterRegistry.INSTANCE.current().parameters());
        lastFlow = transfer.coolantMoved();
        if (lastFlow <= 0) return;
        int drained = input.tank().drain(lastFlow, IFluidHandler.FluidAction.EXECUTE).getAmount();
        int filled = output.tank().fill(
                new FluidStack(ModFluids.get(FluidDefinition.COLD_REACTOR_COOLANT).source().get(), drained),
                IFluidHandler.FluidAction.EXECUTE);
        if (filled != drained) {
            throw new IllegalStateException("Primary pump output changed after successful simulation");
        }
        power.consumeJoules(transfer.energyConsumedJoules(), false);
        markChanged();
    }

    private boolean isColdCoolant(FluidStack stack) {
        return stack.getFluid() == ModFluids.get(FluidDefinition.COLD_REACTOR_COOLANT).source().get();
    }

    private Direction facing() {
        return getBlockState().getValue(MainCoolantPumpBlock.FACING);
    }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putDouble(FLOW_SETTING_TAG, flowSetting);
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        double saved = tag.getDouble(FLOW_SETTING_TAG);
        flowSetting = Double.isFinite(saved) && saved >= 0.25D && saved <= 1.0D ? saved : 1.0D;
        lastFlow = 0;
    }
}
