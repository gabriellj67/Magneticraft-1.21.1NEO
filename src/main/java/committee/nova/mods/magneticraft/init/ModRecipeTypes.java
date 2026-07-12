package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
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

    private ModRecipeTypes() {
    }

    public static void bootstrap() {
    }
}
