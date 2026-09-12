package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;

import java.util.Objects;
import java.util.function.BooleanSupplier;
import java.util.function.DoubleSupplier;

/**
 * Isolated two-terminal transfer used by switches, diodes and adjustable resistors.
 * The resistor follows the one-tick Ohm-law charge request and reports all dissipated energy.
 */
public final class ElectricalControlCoupler implements ElectricalCoupler {
    private static final double SECONDS_PER_TICK = 1.0D / ElectricalNode.TICKS_PER_SECOND;
    private static final double EPSILON = 1.0E-9D;

    private final PhysicalNodeKey firstTerminal;
    private final PhysicalNodeKey secondTerminal;
    private final DirectionMode directionMode;
    private final BooleanSupplier enabledSupplier;
    private final DoubleSupplier resistanceSupplier;
    private CouplingResult lastResult = CouplingResult.ZERO;

    public ElectricalControlCoupler(
            PhysicalNodeKey firstTerminal,
            PhysicalNodeKey secondTerminal,
            DirectionMode directionMode,
            BooleanSupplier enabledSupplier,
            DoubleSupplier resistanceSupplier
    ) {
        this.firstTerminal = Objects.requireNonNull(firstTerminal, "firstTerminal");
        this.secondTerminal = Objects.requireNonNull(secondTerminal, "secondTerminal");
        this.directionMode = Objects.requireNonNull(directionMode, "directionMode");
        this.enabledSupplier = Objects.requireNonNull(enabledSupplier, "enabledSupplier");
        this.resistanceSupplier = Objects.requireNonNull(resistanceSupplier, "resistanceSupplier");
        if (firstTerminal.equals(secondTerminal)) {
            throw new IllegalArgumentException("Electrical-control terminals must be distinct");
        }
    }

    @Override
    public PhysicalNodeKey firstTerminal() {
        return firstTerminal;
    }

    @Override
    public PhysicalNodeKey secondTerminal() {
        return secondTerminal;
    }

    @Override
    public CouplingResult transfer(ElectricalNode first, ElectricalNode second, boolean simulate) {
        Objects.requireNonNull(first, "first");
        Objects.requireNonNull(second, "second");
        if (!enabledSupplier.getAsBoolean()) {
            return finish(CouplingResult.ZERO, simulate);
        }

        double voltageDifference = first.voltage() - second.voltage();
        if (Math.abs(voltageDifference) <= EPSILON
                || directionMode == DirectionMode.FIRST_TO_SECOND && voltageDifference <= 0.0D) {
            return finish(CouplingResult.ZERO, simulate);
        }

        boolean firstWasSource = voltageDifference > 0.0D;
        ElectricalNode source = firstWasSource ? first : second;
        ElectricalNode destination = firstWasSource ? second : first;
        double equivalentCapacitance = first.capacitance() * second.capacitance()
                / (first.capacitance() + second.capacitance());
        double equilibriumCharge = equivalentCapacitance * Math.abs(voltageDifference);
        double resistance = resistanceSupplier.getAsDouble();
        if (!Double.isFinite(resistance) || resistance < 0.0D) {
            return finish(CouplingResult.ZERO, simulate);
        }
        double requestedCharge = resistance <= EPSILON
                ? equilibriumCharge
                : Math.min(equilibriumCharge, Math.abs(voltageDifference) / resistance * SECONDS_PER_TICK);
        double removable = Math.abs(source.applyCharge(-requestedCharge, true));
        double acceptable = Math.abs(destination.applyCharge(requestedCharge, true));
        double charge = Math.min(requestedCharge, Math.min(removable, acceptable));
        if (charge <= EPSILON) {
            return finish(CouplingResult.ZERO, simulate);
        }

        double sourceBefore = source.energyJoules();
        double destinationBefore = destination.energyJoules();
        double sourceAfter = energyAfterCharge(source, -charge);
        double destinationAfter = energyAfterCharge(destination, charge);
        double withdrawn = Math.max(0.0D, sourceBefore - sourceAfter);
        double delivered = Math.max(0.0D, destinationAfter - destinationBefore);
        double loss = Math.max(0.0D, withdrawn - delivered);
        if (!simulate) {
            source.applyCharge(-charge, false);
            destination.applyCharge(charge, false);
        }
        return finish(new CouplingResult(
                withdrawn,
                delivered,
                loss,
                charge,
                charge,
                firstWasSource
        ), simulate);
    }

    public CouplingResult lastResult() {
        return lastResult;
    }

    public double lastCurrentAmps() {
        return Math.max(lastResult.sourceChargeCoulombs(), lastResult.destinationChargeCoulombs())
                * ElectricalNode.TICKS_PER_SECOND;
    }

    private CouplingResult finish(CouplingResult result, boolean simulate) {
        if (!simulate) {
            lastResult = result;
        }
        return result;
    }

    private static double energyAfterCharge(ElectricalNode node, double charge) {
        double voltage = Math.max(0.0D, node.voltage() + charge / node.capacitance());
        return 0.5D * node.capacitance() * voltage * voltage;
    }

    public enum DirectionMode {
        BIDIRECTIONAL,
        FIRST_TO_SECOND
    }
}
