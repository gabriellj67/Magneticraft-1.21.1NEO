package committee.nova.mods.magneticraft.content.nuclear.thermal;

import net.minecraft.core.Direction;
import org.jetbrains.annotations.Nullable;

/** Structural observation supplied to the independent thermal-facility validator. */
public record NuclearThermalPart(Kind kind, @Nullable Direction facing) {
    public enum Kind {
        AIR,
        CONTROLLER,
        CASING,
        PORT,
        HEAT_EXCHANGER,
        COOLING_FILL,
        COOLING_FAN
    }
}
