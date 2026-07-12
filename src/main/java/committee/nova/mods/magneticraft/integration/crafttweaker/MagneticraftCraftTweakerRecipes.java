package committee.nova.mods.magneticraft.integration.crafttweaker;

import com.blamejared.crafttweaker.api.annotation.ZenRegister;
import com.blamejared.crafttweaker.api.data.MapData;
import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import org.openzen.zencode.java.ZenCodeType;

import java.util.Locale;

/** ZenScript facade for Magneticraft's six data-driven recipe types. */
@ZenRegister(modDeps = Magneticraft.MOD_ID)
@ZenCodeType.Name("mods.magneticraft.Recipes")
public final class MagneticraftCraftTweakerRecipes {
    private static final IRecipeManager<?> CRUSHING =
            new MagneticraftRecipeManager<>("crushing", ModRecipeTypes.CRUSHING_TYPE::get);
    private static final IRecipeManager<?> SLUICE =
            new MagneticraftRecipeManager<>("sluice", ModRecipeTypes.SLUICE_TYPE::get);
    private static final IRecipeManager<?> GASIFICATION =
            new MagneticraftRecipeManager<>("gasification", ModRecipeTypes.GASIFICATION_TYPE::get);
    private static final IRecipeManager<?> THERMOPILE =
            new MagneticraftRecipeManager<>("thermopile", ModRecipeTypes.THERMOPILE_TYPE::get);
    private static final IRecipeManager<?> FLUID_FUEL =
            new MagneticraftRecipeManager<>("fluid_fuel", ModRecipeTypes.FLUID_FUEL_TYPE::get);
    private static final IRecipeManager<?> ADVANCED_PROCESSING =
            new MagneticraftRecipeManager<>("advanced_processing", ModRecipeTypes.ADVANCED_PROCESSING_TYPE::get);

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
            case "crushing" -> CRUSHING;
            case "sluice" -> SLUICE;
            case "gasification" -> GASIFICATION;
            case "thermopile" -> THERMOPILE;
            case "fluid_fuel" -> FLUID_FUEL;
            case "advanced_processing" -> ADVANCED_PROCESSING;
            default -> throw new IllegalArgumentException("Unknown Magneticraft recipe type: " + recipeType);
        };
    }
}
