package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfile;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTier;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;

import java.util.Objects;
import java.util.function.BooleanSupplier;

/** Mutable profile binding around a stable pair of isolated transformer terminals. */
public final class TransformerCoupler implements ElectricalCoupler {
    public static final double REVERSAL_VOLTAGE_FRACTION = 0.95D;
    private static final double EPSILON = 1.0E-9D;

    private final PhysicalNodeKey firstTerminal;
    private final PhysicalNodeKey secondTerminal;
    private final BooleanSupplier enabledSupplier;
    private TransformerProfile profile;
    private VoltageTier firstTier;
    private VoltageTier secondTier;
    private boolean reversed;
    private boolean enabled = true;

    public TransformerCoupler(PhysicalNodeKey firstTerminal, PhysicalNodeKey secondTerminal) {
        this(firstTerminal, secondTerminal, () -> true);
    }

    public TransformerCoupler(
            PhysicalNodeKey firstTerminal,
            PhysicalNodeKey secondTerminal,
            BooleanSupplier enabledSupplier
    ) {
        this.firstTerminal = Objects.requireNonNull(firstTerminal, "firstTerminal");
        this.secondTerminal = Objects.requireNonNull(secondTerminal, "secondTerminal");
        this.enabledSupplier = Objects.requireNonNull(enabledSupplier, "enabledSupplier");
        if (firstTerminal.equals(secondTerminal)) {
            throw new IllegalArgumentException("Transformer terminals must be distinct");
        }
    }

    public void reconfigure(TransformerProfile profile, VoltageTier firstTier, VoltageTier secondTier) {
        this.profile = Objects.requireNonNull(profile, "profile");
        this.firstTier = Objects.requireNonNull(firstTier, "firstTier");
        this.secondTier = Objects.requireNonNull(secondTier, "secondTier");
        if (!profile.inputTierId().equals(firstTier.id())
                || !profile.outputTierId().equals(secondTier.id())) {
            throw new IllegalArgumentException("Transformer profile tiers do not match terminal tiers");
        }
    }

    public void clearProfile() {
        profile = null;
        firstTier = null;
        secondTier = null;
    }

    public boolean configured() {
        return profile != null;
    }

    public boolean reversed() {
        return reversed;
    }

    public void setReversed(boolean reversed) {
        this.reversed = reversed;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public boolean canReverse(ElectricalNode first, ElectricalNode second) {
        return configured()
                && first.voltage() < firstTier.nominalVoltage() * REVERSAL_VOLTAGE_FRACTION
                && second.voltage() < secondTier.nominalVoltage() * REVERSAL_VOLTAGE_FRACTION;
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
        if (!enabled || !enabledSupplier.getAsBoolean() || !configured()) {
            return CouplingResult.ZERO;
        }
        ElectricalNode source = reversed ? second : first;
        ElectricalNode destination = reversed ? first : second;
        VoltageTier sourceTier = reversed ? secondTier : firstTier;
        VoltageTier destinationTier = reversed ? firstTier : secondTier;
        if (source.voltage() < sourceTier.minimumOperatingVoltage()) {
            return CouplingResult.ZERO;
        }

        double destinationTargetEnergy = energyAtVoltage(destination, destinationTier.generatorVoltage());
        double destinationRoom = Math.max(0.0D, destinationTargetEnergy - destination.energyJoules());
        double maximumWithdrawal = Math.min(
                profile.maximumTransferJoulesPerTick(),
                source.energyJoules()
        );
        double deliverable = Math.min(destinationRoom, maximumWithdrawal * profile.efficiency());
        if (deliverable <= EPSILON) {
            return CouplingResult.ZERO;
        }
        double requestedWithdrawal = deliverable / profile.efficiency();
        double removable = source.removeEnergy(requestedWithdrawal, true);
        double acceptable = destination.addEnergy(removable * profile.efficiency(), true);
        double delivered = Math.min(acceptable, removable * profile.efficiency());
        double withdrawn = delivered / profile.efficiency();
        if (withdrawn <= EPSILON) {
            return CouplingResult.ZERO;
        }

        double sourceVoltageBefore = source.voltage();
        double destinationVoltageBefore = destination.voltage();
        if (!simulate) {
            double actuallyWithdrawn = source.removeEnergy(withdrawn, false);
            double actuallyDelivered = destination.addEnergy(actuallyWithdrawn * profile.efficiency(), false);
            withdrawn = actuallyWithdrawn;
            delivered = actuallyDelivered;
        }
        double sourceVoltageAfter = voltageAfterEnergy(source, source.energyJoules() - (simulate ? withdrawn : 0.0D));
        double destinationVoltageAfter = voltageAfterEnergy(
                destination,
                destination.energyJoules() + (simulate ? delivered : 0.0D)
        );
        double sourceCharge = source.capacitance() * Math.max(0.0D, sourceVoltageBefore - sourceVoltageAfter);
        double destinationCharge = destination.capacitance()
                * Math.max(0.0D, destinationVoltageAfter - destinationVoltageBefore);
        return new CouplingResult(
                withdrawn,
                delivered,
                Math.max(0.0D, withdrawn - delivered),
                sourceCharge,
                destinationCharge,
                !reversed
        );
    }

    private static double energyAtVoltage(ElectricalNode node, double voltage) {
        return 0.5D * node.capacitance() * voltage * voltage;
    }

    private static double voltageAfterEnergy(ElectricalNode node, double energy) {
        return Math.sqrt(2.0D * Math.max(0.0D, energy) / node.capacitance());
    }
}
