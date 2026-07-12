package committee.nova.mods.magneticraft.content.machine.singleblock.recipe;

import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.registries.ForgeRegistries;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Data-driven block-state heat source used by the thermopile.
 */
public final class ThermopileRecipe implements Recipe<SimpleContainer> {
    private final ResourceLocation id;
    private final Block block;
    private final Map<String, String> stateProperties;
    private final double temperatureKelvin;
    private final double conductivity;

    public ThermopileRecipe(
            ResourceLocation id,
            Block block,
            Map<String, String> stateProperties,
            double temperatureKelvin,
            double conductivity
    ) {
        this.id = Objects.requireNonNull(id);
        this.block = Objects.requireNonNull(block);
        this.stateProperties = Map.copyOf(stateProperties);
        this.temperatureKelvin = Math.max(0.0D, temperatureKelvin);
        this.conductivity = Math.max(0.000001D, conductivity);
    }

    public boolean matches(BlockState state) {
        if (!state.is(block)) {
            return false;
        }
        for (Map.Entry<String, String> expected : stateProperties.entrySet()) {
            Property<?> property = state.getProperties().stream()
                    .filter(candidate -> candidate.getName().equals(expected.getKey()))
                    .findFirst()
                    .orElse(null);
            if (property == null || !valueName(state, property).equals(expected.getValue())) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static String valueName(BlockState state, Property property) {
        return property.getName(state.getValue(property));
    }

    @Override
    public boolean matches(SimpleContainer container, Level level) {
        return false;
    }

    @Override
    public ItemStack assemble(SimpleContainer container, RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return false;
    }

    @Override
    public ItemStack getResultItem(RegistryAccess registryAccess) {
        return ItemStack.EMPTY;
    }

    @Override
    public ResourceLocation getId() {
        return id;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipeTypes.THERMOPILE_SERIALIZER.get();
    }

    @Override
    public RecipeType<?> getType() {
        return ModRecipeTypes.THERMOPILE_TYPE.get();
    }

    public Block block() {
        return block;
    }

    public Map<String, String> stateProperties() {
        return stateProperties;
    }

    public double temperatureKelvin() {
        return temperatureKelvin;
    }

    public double conductivity() {
        return conductivity;
    }

    public int specificity() {
        return stateProperties.size();
    }

    public static final class Serializer implements RecipeSerializer<ThermopileRecipe> {
        @Override
        public ThermopileRecipe fromJson(ResourceLocation id, JsonObject json) {
            ResourceLocation blockId = ResourceLocation.parse(GsonHelper.getAsString(json, "block"));
            Block block = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(blockId), "Unknown block " + blockId);
            Map<String, String> properties = new LinkedHashMap<>();
            if (json.has("state")) {
                GsonHelper.getAsJsonObject(json, "state").entrySet()
                        .forEach(entry -> properties.put(entry.getKey(), entry.getValue().getAsString()));
            }
            return new ThermopileRecipe(
                    id,
                    block,
                    properties,
                    GsonHelper.getAsDouble(json, "temperature"),
                    GsonHelper.getAsDouble(json, "conductivity")
            );
        }

        @Nullable
        @Override
        public ThermopileRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            ResourceLocation blockId = buffer.readResourceLocation();
            Block block = Objects.requireNonNull(ForgeRegistries.BLOCKS.getValue(blockId), "Unknown block " + blockId);
            int size = buffer.readVarInt();
            Map<String, String> properties = new LinkedHashMap<>();
            for (int index = 0; index < size; index++) {
                properties.put(buffer.readUtf(), buffer.readUtf());
            }
            return new ThermopileRecipe(id, block, properties, buffer.readDouble(), buffer.readDouble());
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, ThermopileRecipe recipe) {
            buffer.writeResourceLocation(Objects.requireNonNull(ForgeRegistries.BLOCKS.getKey(recipe.block)));
            buffer.writeVarInt(recipe.stateProperties.size());
            recipe.stateProperties.forEach((key, value) -> {
                buffer.writeUtf(key);
                buffer.writeUtf(value);
            });
            buffer.writeDouble(recipe.temperatureKelvin);
            buffer.writeDouble(recipe.conductivity);
        }
    }
}
