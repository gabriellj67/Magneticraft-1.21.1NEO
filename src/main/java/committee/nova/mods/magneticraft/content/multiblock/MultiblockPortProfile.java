package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * One immutable source of truth for advanced-machine tank roles, accepted
 * fluids and controller-relative side access.
 */
public final class MultiblockPortProfile {
    public static final String WATER_TAG = "#minecraft:water";
    public static final String COMBUSTION_FUEL_RECIPE = "@magneticraft:industrial_combustion_chamber";

    private MultiblockPortProfile() {
    }

    public static List<TankPort> tanks(MultiblockDefinition definition) {
        return switch (definition) {
            case STEAM_ENGINE, STEAM_TURBINE -> List.of(port(
                    definition, 0, "steam_input", List.of(fluid(FluidDefinition.STEAM)),
                    sides(FluidTankModule.TankAccess.INPUT, RelativeSide.values())
            ));
            case PUMPJACK -> List.of(port(
                    definition, 0, "oil_output", List.of(fluid(FluidDefinition.OIL)),
                    sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.values())
            ));
            case BIG_COMBUSTION_CHAMBER -> List.of(port(
                    definition, 0, "fluid_fuel", List.of(COMBUSTION_FUEL_RECIPE),
                    merge(
                            sides(FluidTankModule.TankAccess.INPUT,
                                    RelativeSide.FRONT, RelativeSide.BACK,
                                    RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.UP),
                            sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.DOWN)
                    )
            ));
            case OIL_HEATER -> List.of(
                    port(definition, 0, "feed_input", List.of(
                                    fluid(FluidDefinition.OIL), WATER_TAG),
                            sides(FluidTankModule.TankAccess.INPUT, RelativeSide.FRONT)),
                    port(definition, 1, "product_output", List.of(
                                    fluid(FluidDefinition.HOT_CRUDE), fluid(FluidDefinition.STEAM)),
                            sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.BACK, RelativeSide.UP))
            );
            case BIG_STEAM_BOILER -> List.of(
                    port(definition, 0, "water_input", List.of(WATER_TAG),
                            sides(FluidTankModule.TankAccess.INPUT,
                                    RelativeSide.FRONT, RelativeSide.BACK,
                                    RelativeSide.LEFT, RelativeSide.RIGHT, RelativeSide.DOWN)),
                    port(definition, 1, "steam_output", List.of(fluid(FluidDefinition.STEAM)),
                            sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.UP))
            );
            case REFINERY -> List.of(
                    port(definition, 0, "feed_input", List.of(
                                    fluid(FluidDefinition.STEAM),
                                    fluid(FluidDefinition.HOT_CRUDE),
                                    fluid(FluidDefinition.HEAVY_OIL),
                                    fluid(FluidDefinition.LIGHT_OIL),
                                    fluid(FluidDefinition.LPG)),
                            sides(FluidTankModule.TankAccess.INPUT, RelativeSide.FRONT)),
                    port(definition, 1, "process_steam_input", List.of(fluid(FluidDefinition.STEAM)),
                            sides(FluidTankModule.TankAccess.INPUT, RelativeSide.BACK)),
                    port(definition, 2, "heavy_product_output", List.of(
                                    WATER_TAG,
                                    fluid(FluidDefinition.HEAVY_OIL),
                                    fluid(FluidDefinition.OIL_RESIDUE),
                                    fluid(FluidDefinition.DIESEL),
                                    fluid(FluidDefinition.PLASTIC)),
                            sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.LEFT)),
                    port(definition, 3, "light_product_output", List.of(
                                    fluid(FluidDefinition.LIGHT_OIL),
                                    fluid(FluidDefinition.FUEL),
                                    fluid(FluidDefinition.KEROSENE),
                                    fluid(FluidDefinition.NAPHTHA)),
                            sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.RIGHT)),
                    port(definition, 4, "gas_product_output", List.of(
                                    fluid(FluidDefinition.LPG),
                                    fluid(FluidDefinition.LUBRICANT),
                                    fluid(FluidDefinition.GASOLINE),
                                    fluid(FluidDefinition.NATURAL_GAS)),
                            sides(FluidTankModule.TankAccess.OUTPUT, RelativeSide.UP))
            );
            default -> List.of();
        };
    }

    public static TankPort tank(MultiblockDefinition definition, int index) {
        return tanks(definition).stream()
                .filter(port -> port.index() == index)
                .findFirst()
                .orElseThrow(() -> new IndexOutOfBoundsException("Tank " + index + " for " + definition.id()));
    }

    private static TankPort port(
            MultiblockDefinition definition,
            int index,
            String role,
            List<String> acceptedFluids,
            Map<RelativeSide, FluidTankModule.TankAccess> sideAccess
    ) {
        return new TankPort(
                index,
                definition.tankCapacity(index),
                role,
                acceptedFluids,
                index == 0 ? FluidTankModule.TankAccess.BOTH : FluidTankModule.TankAccess.NONE,
                sideAccess
        );
    }

    private static String fluid(FluidDefinition definition) {
        return Magneticraft.id(definition.id()).toString();
    }

    private static Map<RelativeSide, FluidTankModule.TankAccess> sides(
            FluidTankModule.TankAccess access,
            RelativeSide... sides
    ) {
        EnumMap<RelativeSide, FluidTankModule.TankAccess> result = new EnumMap<>(RelativeSide.class);
        for (RelativeSide side : sides) {
            result.put(side, access);
        }
        return result;
    }

    @SafeVarargs
    private static Map<RelativeSide, FluidTankModule.TankAccess> merge(
            Map<RelativeSide, FluidTankModule.TankAccess>... maps
    ) {
        EnumMap<RelativeSide, FluidTankModule.TankAccess> result = new EnumMap<>(RelativeSide.class);
        for (Map<RelativeSide, FluidTankModule.TankAccess> map : maps) {
            result.putAll(map);
        }
        return result;
    }

    public enum RelativeSide {
        FRONT,
        BACK,
        LEFT,
        RIGHT,
        UP,
        DOWN;

        public String serializedName() {
            return name().toLowerCase(Locale.ROOT);
        }

        static RelativeSide fromWorld(Direction side, Direction front) {
            if (side == Direction.UP) {
                return UP;
            }
            if (side == Direction.DOWN) {
                return DOWN;
            }
            if (side == front) {
                return FRONT;
            }
            if (side == front.getOpposite()) {
                return BACK;
            }
            return side == front.getCounterClockWise() ? LEFT : RIGHT;
        }
    }

    public record TankPort(
            int index,
            int capacity,
            String role,
            List<String> acceptedFluids,
            FluidTankModule.TankAccess unsidedAccess,
            Map<RelativeSide, FluidTankModule.TankAccess> sideAccess
    ) {
        public TankPort {
            if (index < 0 || capacity <= 0 || role == null || role.isBlank()) {
                throw new IllegalArgumentException("Invalid multiblock tank port");
            }
            acceptedFluids = List.copyOf(acceptedFluids);
            unsidedAccess = Objects.requireNonNull(unsidedAccess);
            sideAccess = Map.copyOf(sideAccess);
        }

        public FluidTankModule.TankAccess access(@Nullable Direction side, Direction front) {
            return side == null
                    ? unsidedAccess
                    : sideAccess.getOrDefault(RelativeSide.fromWorld(side, front), FluidTankModule.TankAccess.NONE);
        }

        public boolean accepts(FluidStack stack, @Nullable ServerLevel level) {
            if (stack.isEmpty()) {
                return false;
            }
            for (String accepted : acceptedFluids) {
                if (WATER_TAG.equals(accepted) && stack.getFluid().defaultFluidState().is(FluidTags.WATER)) {
                    return true;
                }
                if (COMBUSTION_FUEL_RECIPE.equals(accepted) && level != null && level.getRecipeManager()
                        .getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get())
                        .stream()
                        .anyMatch(recipe -> recipe.fluid() == stack.getFluid())) {
                    return true;
                }
                if (ForgeRegistries.FLUIDS.getKey(stack.getFluid()) != null
                        && accepted.equals(ForgeRegistries.FLUIDS.getKey(stack.getFluid()).toString())) {
                    return true;
                }
            }
            return false;
        }
    }
}
