package committee.nova.mods.magneticraft.system.network.electric;

import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNodeKey;

/**
 * Explicit isolated exchange between two terminals, such as a transformer.
 * Couplers never add a graph edge and therefore never merge voltage components.
 */
public interface ElectricalCoupler {
    PhysicalNodeKey firstTerminal();

    PhysicalNodeKey secondTerminal();

    CouplingResult transfer(ElectricalNode first, ElectricalNode second, boolean simulate);

    record CouplingResult(
            double withdrawnJoules,
            double deliveredJoules,
            double lostJoules,
            double sourceChargeCoulombs,
            double destinationChargeCoulombs,
            boolean firstWasSource
    ) {
        private static final double EPSILON = 1.0E-7D;
        public static final CouplingResult ZERO = new CouplingResult(0.0D, 0.0D, 0.0D, 0.0D, 0.0D, true);

        public CouplingResult {
            requireNonNegativeFinite("withdrawnJoules", withdrawnJoules);
            requireNonNegativeFinite("deliveredJoules", deliveredJoules);
            requireNonNegativeFinite("lostJoules", lostJoules);
            requireNonNegativeFinite("sourceChargeCoulombs", sourceChargeCoulombs);
            requireNonNegativeFinite("destinationChargeCoulombs", destinationChargeCoulombs);
            if (Math.abs(withdrawnJoules - deliveredJoules - lostJoules) > EPSILON
                    * Math.max(1.0D, withdrawnJoules)) {
                throw new IllegalArgumentException("Electrical coupler result does not conserve energy");
            }
        }

        public boolean moved() {
            return withdrawnJoules > EPSILON;
        }

        private static void requireNonNegativeFinite(String field, double value) {
            if (!Double.isFinite(value) || value < 0.0D) {
                throw new IllegalArgumentException(field + " must be finite and non-negative");
            }
        }
    }
}
