package committee.nova.mods.magneticraft.content.multiblock;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.FluidTags;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * Immutable advanced-machine tank metadata. Exact capability positions,
 * sides and access modes live exclusively in {@link MultiblockPortLayout}.
 */
public final class MultiblockPortProfile {
    public static final String WATER_TAG = "#minecraft:water";
    public static final String COMBUSTION_FUEL_RECIPE = "@magneticraft:industrial_combustion_chamber";

    private MultiblockPortProfile() {
    }

    public static List<TankPort> tanks(MultiblockDefinition definition) {
        return switch (definition) {
            case STEAM_ENGINE, STEAM_TURBINE -> List.of(port(
                    definition, 0, "steam_input", List.of(fluid(FluidDefinition.STEAM))
            ));
            case PUMPJACK -> List.of(port(
                    definition, 0, "oil_output", List.of(fluid(FluidDefinition.OIL))
            ));
            case BIG_COMBUSTION_CHAMBER -> List.of(port(
                    definition, 0, "fluid_fuel", List.of(COMBUSTION_FUEL_RECIPE)
            ));
            case POLYMERIZER -> List.of(port(
                    definition, 0, "feed_input", List.of(
                            fluid(FluidDefinition.PLASTIC),
                            fluid(FluidDefinition.NATURAL_GAS)
                    )
            ));
            case OIL_HEATER -> List.of(
                    port(definition, 0, "feed_input", List.of(
                                    fluid(FluidDefinition.OIL), WATER_TAG)),
                    port(definition, 1, "product_output", List.of(
                                    fluid(FluidDefinition.HOT_CRUDE), fluid(FluidDefinition.STEAM)))
            );
            case BIG_STEAM_BOILER -> List.of(
                    port(definition, 0, "water_input", List.of(WATER_TAG)),
                    port(definition, 1, "steam_output", List.of(fluid(FluidDefinition.STEAM)))
            );
            case REFINERY -> List.of(
                    port(definition, 0, "feed_input", List.of(
                                    fluid(FluidDefinition.STEAM),
                                    fluid(FluidDefinition.HOT_CRUDE),
                                    fluid(FluidDefinition.HEAVY_OIL),
                                    fluid(FluidDefinition.LIGHT_OIL),
                                    fluid(FluidDefinition.LPG))),
                    port(definition, 1, "process_steam_input", List.of(fluid(FluidDefinition.STEAM))),
                    port(definition, 2, "heavy_product_output", List.of(
                                    WATER_TAG,
                                    fluid(FluidDefinition.HEAVY_OIL),
                                    fluid(FluidDefinition.OIL_RESIDUE),
                                    fluid(FluidDefinition.DIESEL),
                                    fluid(FluidDefinition.PLASTIC))),
                    port(definition, 3, "light_product_output", List.of(
                                    fluid(FluidDefinition.LIGHT_OIL),
                                    fluid(FluidDefinition.FUEL),
                                    fluid(FluidDefinition.KEROSENE),
                                    fluid(FluidDefinition.NAPHTHA))),
                    port(definition, 4, "gas_product_output", List.of(
                                    fluid(FluidDefinition.LPG),
                                    fluid(FluidDefinition.LUBRICANT),
                                    fluid(FluidDefinition.GASOLINE),
                                    fluid(FluidDefinition.NATURAL_GAS)))
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
            List<String> acceptedFluids
    ) {
        return new TankPort(
                index,
                definition.tankCapacity(index),
                role,
                acceptedFluids
        );
    }

    private static String fluid(FluidDefinition definition) {
        return Magneticraft.id(definition.id()).toString();
    }

    public record TankPort(
            int index,
            int capacity,
            String role,
            List<String> acceptedFluids
    ) {
        public TankPort {
            if (index < 0 || capacity <= 0 || role == null || role.isBlank()) {
                throw new IllegalArgumentException("Invalid multiblock tank port");
            }
            acceptedFluids = List.copyOf(acceptedFluids);
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
