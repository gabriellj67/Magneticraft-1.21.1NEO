package committee.nova.mods.magneticraft.system.network.heat;

/** Preserves the released Nova 1.12 uninsulated-pipe contact thresholds. */
public final class HeatPipeContactDamage {
    private HeatPipeContactDamage() {
    }

    public static float atCelsius(double temperatureCelsius) {
        if (!Double.isFinite(temperatureCelsius) || temperatureCelsius < 80.0D) {
            return 0.0F;
        }
        // The legacy branch order made every value above 80 C deal 2 damage,
        // while the exact 80 C boundary fell through to 20 damage.
        return temperatureCelsius > 80.0D ? 2.0F : 20.0F;
    }
}
