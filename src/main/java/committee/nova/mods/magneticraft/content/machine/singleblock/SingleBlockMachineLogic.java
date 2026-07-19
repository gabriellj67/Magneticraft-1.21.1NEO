package committee.nova.mods.magneticraft.content.machine.singleblock;

import net.minecraft.server.level.ServerLevel;

/**
 * Exhaustive behavior router that keeps the block entity focused on hosting modules and persistence.
 */
final class SingleBlockMachineLogic {
    private static final double VISUAL_HEAT_THRESHOLD_KELVIN = 90.0D + 273.15D;

    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;
    private final SingleBlockAutomationLogic automation;
    private final SingleBlockThermalLogic thermal;
    private final SingleBlockElectricalLogic electrical;

    SingleBlockMachineLogic(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
        automation = new SingleBlockAutomationLogic(machine, state);
        thermal = new SingleBlockThermalLogic(machine, state);
        electrical = new SingleBlockElectricalLogic(machine, state);
    }

    void tick(ServerLevel level) {
        state.working = false;
        switch (machine.definition()) {
            case BOX, FABRICATOR, SMALL_TANK, FILTER -> automation.tickPassive(level);
            case SLUICE_BOX -> automation.tickSluice(level);
            case FEEDING_TROUGH -> automation.tickFeedingTrough(level);
            case INSERTER -> automation.tickInserter(level);
            case BLOCK_BREAKER -> automation.tickBlockBreaker(level);
            case SPRINKLER -> automation.tickSprinkler(level);
            case WATER_GENERATOR -> automation.tickWaterGenerator();
            case RELAY -> automation.tickRelay(level);
            case TRANSPOSER -> automation.tickTransposer(level);
            case COMBUSTION_CHAMBER -> thermal.tickCombustionChamber();
            case STEAM_BOILER -> thermal.tickSteamBoiler();
            case ELECTRIC_HEATER, RF_HEATER -> thermal.tickHeater();
            case GASIFICATION_UNIT -> thermal.tickGasification(level);
            case BRICK_FURNACE -> thermal.tickBrickFurnace(level);
            case INFINITE_ENERGY -> electrical.tickInfiniteEnergy();
            case AIRLOCK -> electrical.tickAirlock(level);
            case THERMOPILE -> electrical.tickThermopile(level);
            case INTERNAL_COMBUSTION_ENGINE -> electrical.tickInternalCombustionEngine(level);
            case ELECTRIC_ENGINE -> electrical.tickElectricEngine();
            case GEOTHERMAL_PUMP -> thermal.tickGeothermalPump(level);
            case RF_TRANSFORMER -> {
            }
        }
    }

    void activateSluiceChain() {
        automation.activateSluiceChain();
    }

    void onBroken(ServerLevel level) {
        if (machine.definition() == SingleBlockMachineDefinition.AIRLOCK) {
            electrical.startAirBubbleDecay(level);
        }
    }

    boolean isVisuallyLit() {
        return switch (machine.definition()) {
            case ELECTRIC_HEATER, RF_HEATER -> machine.heat() != null
                    && machine.heat().node().temperatureKelvin() > VISUAL_HEAT_THRESHOLD_KELVIN;
            case BRICK_FURNACE -> state.working;
            default -> false;
        };
    }
}
