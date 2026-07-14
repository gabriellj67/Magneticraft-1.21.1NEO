package committee.nova.mods.magneticraft.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.data.MapData;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Locale;

/** ZenScript facade for Magneticraft's content-owned data-driven recipe types. */
@ZenRegister(modDeps = Magneticraft.MOD_ID)
@ZenCodeType.Name("mods.magneticraft.Recipes")
public final class MagneticraftCraftTweakerRecipes {
    private static final IRecipeManager<?> CRUSHING =
            new MagneticraftRecipeManager<>("crushing_table", ModRecipeTypes.CRUSHING_TYPE::get);
    private static final IRecipeManager<?> SLUICE =
            new MagneticraftRecipeManager<>("sluice_box", ModRecipeTypes.SLUICE_TYPE::get);
    private static final IRecipeManager<?> GASIFICATION =
            new MagneticraftRecipeManager<>("gasification_unit", ModRecipeTypes.GASIFICATION_TYPE::get);
    private static final IRecipeManager<?> THERMOPILE =
            new MagneticraftRecipeManager<>("thermopile", ModRecipeTypes.THERMOPILE_TYPE::get);
    private static final IRecipeManager<?> FLUID_FUEL =
            new MagneticraftRecipeManager<>(
                    MultiblockDefinition.BIG_COMBUSTION_CHAMBER.id(),
                    ModRecipeTypes.FLUID_FUEL_TYPE::get
            );
    private static final IRecipeManager<?> GRINDER = advanced(MultiblockDefinition.GRINDER);
    private static final IRecipeManager<?> SIEVE = advanced(MultiblockDefinition.SIEVE);
    private static final IRecipeManager<?> HYDRAULIC_PRESS = advanced(MultiblockDefinition.HYDRAULIC_PRESS);
    private static final IRecipeManager<?> OIL_HEATER = advanced(MultiblockDefinition.OIL_HEATER);
    private static final IRecipeManager<?> REFINERY = advanced(MultiblockDefinition.REFINERY);

    private MagneticraftCraftTweakerRecipes() {
    }

    /** Adds one JSON recipe through CraftTweaker's validated ActionAddRecipe path. */
    @ZenCodeType.Method
    public static void add(String recipeType, String name, MapData recipeData) {
        manager(recipeType).addJsonRecipe(name, recipeData);
    }

    /** Removes named recipes through CraftTweaker's official removal action. */
    @ZenCodeType.Method
    public static void remove(String recipeType, String... names) {
        manager(recipeType).removeByName(names);
    }

    private static IRecipeManager<?> manager(String recipeType) {
        String normalized = recipeType.toLowerCase(Locale.ROOT);
        if (normalized.startsWith(Magneticraft.MOD_ID + ":")) {
            normalized = normalized.substring(Magneticraft.MOD_ID.length() + 1);
        }
        return switch (normalized) {
            case "crushing_table" -> CRUSHING;
            case "sluice_box" -> SLUICE;
            case "gasification_unit" -> GASIFICATION;
            case "thermopile" -> THERMOPILE;
            case "industrial_combustion_chamber" -> FLUID_FUEL;
            case "grinder" -> GRINDER;
            case "sieve" -> SIEVE;
            case "hydraulic_press" -> HYDRAULIC_PRESS;
            case "oil_heater" -> OIL_HEATER;
            case "refinery" -> REFINERY;
            default -> throw new IllegalArgumentException("Unknown Magneticraft recipe type: " + recipeType);
        };
    }

    private static IRecipeManager<?> advanced(MultiblockDefinition definition) {
        return new MagneticraftRecipeManager<>(
                definition.id(),
                () -> ModRecipeTypes.advancedProcessingType(definition).get()
        );
    }
}
