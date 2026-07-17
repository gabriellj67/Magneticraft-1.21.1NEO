package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorRodGroup;
import org.jetbrains.annotations.Nullable;

/** Validated control intent carried by the dedicated C2S reactor packet. */
public record NuclearReactorAction(
        Type type,
        @Nullable ReactorRodGroup group,
        @Nullable ReactorControlMode mode,
        @Nullable ReactorColumnCoordinate coordinate,
        int value,
        boolean confirmed
) {
    public NuclearReactorAction {
        if (type == null) {
            throw new IllegalArgumentException("type must not be null");
        }
        if (value < 0 || value > 1000) {
            throw new IllegalArgumentException("value must be in [0, 1000]");
        }
        if (type == Type.SET_ROD_GROUP && group == null) {
            throw new IllegalArgumentException("rod group is required");
        }
        if (type == Type.SET_MODE && mode == null) {
            throw new IllegalArgumentException("control mode is required");
        }
        if ((type == Type.LOAD_FUEL_FROM_HAND || type == Type.UNLOAD_FUEL) && coordinate == null) {
            throw new IllegalArgumentException("fuel coordinate is required");
        }
    }

    public static NuclearReactorAction simple(Type type) {
        return new NuclearReactorAction(type, null, null, null, 0, false);
    }

    public enum Type {
        SET_ROD_GROUP,
        SCRAM,
        RESET,
        START,
        STOP,
        SET_MODE,
        SET_TARGET_POWER,
        SET_OVERRIDE,
        LOAD_FUEL_FROM_HAND,
        UNLOAD_FUEL,
        INSTALL_UPGRADE
    }
}
