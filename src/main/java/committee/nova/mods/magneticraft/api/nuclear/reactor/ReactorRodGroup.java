package committee.nova.mods.magneticraft.api.nuclear.reactor;

/** Four independently commanded control-rod groups. */
public enum ReactorRodGroup {
    A(NuclearReactorColumnType.CONTROL_ROD_A),
    B(NuclearReactorColumnType.CONTROL_ROD_B),
    C(NuclearReactorColumnType.CONTROL_ROD_C),
    D(NuclearReactorColumnType.CONTROL_ROD_D);

    private final NuclearReactorColumnType columnType;

    ReactorRodGroup(NuclearReactorColumnType columnType) {
        this.columnType = columnType;
    }

    public NuclearReactorColumnType columnType() {
        return columnType;
    }

    public static ReactorRodGroup fromColumn(NuclearReactorColumnType type) {
        for (ReactorRodGroup group : values()) {
            if (group.columnType == type) {
                return group;
            }
        }
        throw new IllegalArgumentException("Not a control-rod column: " + type);
    }
}
