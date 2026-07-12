package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;

/**
 * Custom machine recipe registrations.
 */
public final class ModRecipeTypes {
    public static final RegistryObject<RecipeType<CrushingRecipe>> CRUSHING_TYPE =
            ModRegistries.RECIPE_TYPES.register("crushing", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "magneticraft:crushing";
                }
            });
    public static final RegistryObject<RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("crushing", CrushingRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<SluiceRecipe>> SLUICE_TYPE = type("sluice");
    public static final RegistryObject<RecipeSerializer<SluiceRecipe>> SLUICE_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("sluice", SluiceRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<GasificationRecipe>> GASIFICATION_TYPE = type("gasification");
    public static final RegistryObject<RecipeSerializer<GasificationRecipe>> GASIFICATION_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("gasification", GasificationRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<ThermopileRecipe>> THERMOPILE_TYPE = type("thermopile");
    public static final RegistryObject<RecipeSerializer<ThermopileRecipe>> THERMOPILE_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("thermopile", ThermopileRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<FluidFuelRecipe>> FLUID_FUEL_TYPE = type("fluid_fuel");
    public static final RegistryObject<RecipeSerializer<FluidFuelRecipe>> FLUID_FUEL_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("fluid_fuel", FluidFuelRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<AdvancedProcessingRecipe>> ADVANCED_PROCESSING_TYPE =
            type("advanced_processing");
    public static final RegistryObject<RecipeSerializer<AdvancedProcessingRecipe>> ADVANCED_PROCESSING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register(
                    "advanced_processing",
                    AdvancedProcessingRecipe.Serializer::new
            );

    private ModRecipeTypes() {
    }

    public static void bootstrap() {
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RegistryObject<RecipeType<T>> type(String id) {
        return ModRegistries.RECIPE_TYPES.register(id, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return "magneticraft:" + id;
            }
        });
    }
}
