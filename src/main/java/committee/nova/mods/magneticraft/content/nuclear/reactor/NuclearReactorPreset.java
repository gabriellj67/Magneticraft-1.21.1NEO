package committee.nova.mods.magneticraft.content.nuclear.reactor;

import committee.nova.mods.magneticraft.api.nuclear.reactor.NuclearReactorColumnType;
import committee.nova.mods.magneticraft.api.nuclear.reactor.ReactorColumnCoordinate;

import java.util.LinkedHashMap;
import java.util.Map;

/** Shared starter layouts consumed by guide generation, tests and visual acceptance. */
public enum NuclearReactorPreset {
    ROBUST_BASELOAD(
            "robust_baseload",
            "FWF",
            "WAI",
            "FWF"
    ),
    COMPACT_HIGH_POWER(
            "compact_high_power",
            "RFWFR",
            "FWAWF",
            "WBIBW",
            "FWCWF",
            "RFWFR"
    ),
    FAST_LOAD_FOLLOWING(
            "fast_load_following",
            "RFWAWFR",
            "FWFWFWF",
            "WBFIFCW",
            "FWFWFWF",
            "WCIFIDW",
            "FWFWFWF",
            "RFWAWFR"
    );

    public static final int STARTER_HEIGHT = 7;

    private final String id;
    private final int width;
    private final int length;
    private final Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns;

    NuclearReactorPreset(String id, String... rows) {
        if (id.isBlank() || rows.length == 0) {
            throw new IllegalArgumentException("Reactor preset ID and rows must not be empty");
        }
        int activeWidth = rows[0].length();
        if (activeWidth == 0 || rows.length != activeWidth) {
            throw new IllegalArgumentException("Reactor preset core must be a non-empty square: " + id);
        }
        Map<ReactorColumnCoordinate, NuclearReactorColumnType> parsed = new LinkedHashMap<>();
        for (int z = 0; z < rows.length; z++) {
            if (rows[z].length() != activeWidth) {
                throw new IllegalArgumentException("Inconsistent reactor preset row width: " + id);
            }
            for (int x = 0; x < activeWidth; x++) {
                parsed.put(new ReactorColumnCoordinate(x, z), column(rows[z].charAt(x)));
            }
        }
        this.id = id;
        width = activeWidth + 4;
        length = rows.length + 4;
        columns = Map.copyOf(parsed);
        if (!NuclearReactorStructure.DESCRIPTOR.accepts(width, length, STARTER_HEIGHT)) {
            throw new IllegalArgumentException("Reactor preset dimensions are outside the PWR descriptor: " + id);
        }
        if (columns.values().stream().noneMatch(NuclearReactorColumnType::isFuel)
                || columns.values().stream().noneMatch(NuclearReactorColumnType::isControlRod)
                || !columns.containsValue(NuclearReactorColumnType.COOLANT_CHANNEL)
                || !columns.containsValue(NuclearReactorColumnType.INSTRUMENTATION)) {
            throw new IllegalArgumentException("Reactor preset misses a required column role: " + id);
        }
    }

    public String id() {
        return id;
    }

    public int width() {
        return width;
    }

    public int length() {
        return length;
    }

    public int height() {
        return STARTER_HEIGHT;
    }

    public Map<ReactorColumnCoordinate, NuclearReactorColumnType> columns() {
        return columns;
    }

    public String translationKey() {
        return "guide.magneticraft.reactor.blueprint." + id + ".name";
    }

    public String descriptionKey() {
        return "guide.magneticraft.reactor.blueprint." + id + ".description";
    }

    private static NuclearReactorColumnType column(char symbol) {
        return switch (symbol) {
            case 'F' -> NuclearReactorColumnType.FUEL_STANDARD;
            case 'A' -> NuclearReactorColumnType.CONTROL_ROD_A;
            case 'B' -> NuclearReactorColumnType.CONTROL_ROD_B;
            case 'C' -> NuclearReactorColumnType.CONTROL_ROD_C;
            case 'D' -> NuclearReactorColumnType.CONTROL_ROD_D;
            case 'W' -> NuclearReactorColumnType.COOLANT_CHANNEL;
            case 'I' -> NuclearReactorColumnType.INSTRUMENTATION;
            case 'R' -> NuclearReactorColumnType.REFLECTOR;
            default -> throw new IllegalArgumentException("Unknown reactor preset symbol: " + symbol);
        };
    }
}
