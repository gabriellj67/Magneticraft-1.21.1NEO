package committee.nova.mods.magneticraft.api.nuclear.structure;

import net.minecraft.resources.ResourceLocation;

import java.util.Objects;

/** Public size contract for a sealed, controller-anchored nuclear structure. */
public record VariableNuclearStructureDescriptor(
        ResourceLocation id,
        IntRange width,
        IntRange length,
        IntRange height
) {
    public VariableNuclearStructureDescriptor {
        Objects.requireNonNull(id);
        Objects.requireNonNull(width);
        Objects.requireNonNull(length);
        Objects.requireNonNull(height);
    }

    public boolean accepts(int width, int length, int height) {
        return this.width.contains(width) && this.length.contains(length) && this.height.contains(height);
    }

    public record IntRange(int minimum, int maximum) {
        public IntRange {
            if (minimum <= 0 || maximum < minimum) {
                throw new IllegalArgumentException("Invalid structure range " + minimum + ".." + maximum);
            }
        }

        public boolean contains(int value) {
            return value >= minimum && value <= maximum;
        }
    }
}
