package committee.nova.mods.magneticraft.integration.crafttweaker;

import com.blamejared.crafttweaker.api.recipe.manager.base.IRecipeManager;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeType;

import java.util.Objects;
import java.util.function.Supplier;

/** Minimal manager adapter that delegates all add/remove actions to CraftTweaker's official implementation. */
final class MagneticraftRecipeManager<T extends Recipe<?>> implements IRecipeManager<T> {
    private final ResourceLocation id;
    private final Supplier<RecipeType<T>> recipeType;

    MagneticraftRecipeManager(String path, Supplier<RecipeType<T>> recipeType) {
        this.id = Magneticraft.id(path);
        this.recipeType = Objects.requireNonNull(recipeType);
    }

    @Override
    public RecipeType<T> getRecipeType() {
        return recipeType.get();
    }

    @Override
    public ResourceLocation getBracketResourceLocation() {
        return id;
    }
}
