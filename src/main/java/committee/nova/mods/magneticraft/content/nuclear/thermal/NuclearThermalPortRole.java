package committee.nova.mods.magneticraft.content.nuclear.thermal;

import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;

/** Exact external role assigned to a thermal-facility penetration by its immutable snapshot. */
public enum NuclearThermalPortRole {
    HOT_COOLANT_INPUT(FluidTankModule.TankAccess.INPUT),
    COLD_COOLANT_OUTPUT(FluidTankModule.TankAccess.OUTPUT),
    WATER_INPUT(FluidTankModule.TankAccess.INPUT),
    STEAM_OUTPUT(FluidTankModule.TankAccess.OUTPUT),
    EXHAUST_INPUT(FluidTankModule.TankAccess.INPUT),
    WATER_OUTPUT(FluidTankModule.TankAccess.OUTPUT),
    MAKEUP_WATER_INPUT(FluidTankModule.TankAccess.INPUT),
    ELECTRICAL(FluidTankModule.TankAccess.NONE),
    HEAT(FluidTankModule.TankAccess.NONE);

    private final FluidTankModule.TankAccess fluidAccess;

    NuclearThermalPortRole(FluidTankModule.TankAccess fluidAccess) {
        this.fluidAccess = fluidAccess;
    }

    public FluidTankModule.TankAccess fluidAccess() {
        return fluidAccess;
    }

    public boolean isFluid() {
        return fluidAccess != FluidTankModule.TankAccess.NONE;
    }
}
