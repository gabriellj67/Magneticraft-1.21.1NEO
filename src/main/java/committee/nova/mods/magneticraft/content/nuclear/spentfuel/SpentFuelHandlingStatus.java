package committee.nova.mods.magneticraft.content.nuclear.spentfuel;

/** Player-facing handling state derived from pool inventory and the two safety thresholds. */
public enum SpentFuelHandlingStatus {
    EMPTY,
    DANGEROUS,
    COOLING,
    TRANSFERABLE,
    SEALABLE;

    public static SpentFuelHandlingStatus from(
            int fuelCount,
            int transferableAssemblies,
            int sealableAssemblies,
            boolean cooling
    ) {
        if (fuelCount <= 0) {
            return EMPTY;
        }
        if (sealableAssemblies >= fuelCount) {
            return SEALABLE;
        }
        if (transferableAssemblies > 0) {
            return TRANSFERABLE;
        }
        return cooling ? COOLING : DANGEROUS;
    }

    public String translationKey() {
        return "gui.magneticraft.spent_fuel_pool.status." + name().toLowerCase(java.util.Locale.ROOT);
    }
}
