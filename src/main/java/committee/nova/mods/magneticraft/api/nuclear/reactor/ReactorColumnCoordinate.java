package committee.nova.mods.magneticraft.api.nuclear.reactor;

/** Horizontal coordinate in the active core grid, independent of world orientation. */
public record ReactorColumnCoordinate(int x, int z) {
    public ReactorColumnCoordinate {
        if (x < 0 || z < 0) {
            throw new IllegalArgumentException("Core coordinates must be non-negative");
        }
    }
}
