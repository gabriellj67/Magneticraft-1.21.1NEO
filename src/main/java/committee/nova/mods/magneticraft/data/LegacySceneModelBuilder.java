package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.client.model.generators.CustomLoaderBuilder;
import net.neoforged.neoforge.client.model.generators.ModelBuilder;
import net.neoforged.neoforge.common.data.ExistingFileHelper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/** Datagen builder for Magneticraft's MCX/glTF scene geometry loader. */
final class LegacySceneModelBuilder<T extends ModelBuilder<T>> extends CustomLoaderBuilder<T> {
    private ResourceLocation model;
    private final List<String> includeNodes = new ArrayList<>();
    private final List<String> includeSubtrees = new ArrayList<>();
    private final List<String> excludeNodes = new ArrayList<>();
    private final List<String> excludeSubtrees = new ArrayList<>();
    private float[] translation;

    static <T extends ModelBuilder<T>> LegacySceneModelBuilder<T> begin(
            T parent,
            ExistingFileHelper existingFileHelper
    ) {
        return new LegacySceneModelBuilder<>(parent, existingFileHelper);
    }

    private LegacySceneModelBuilder(T parent, ExistingFileHelper existingFileHelper) {
        super(Magneticraft.id("legacy_scene"), parent, existingFileHelper, false);
    }

    LegacySceneModelBuilder<T> model(ResourceLocation model) {
        this.model = model;
        return this;
    }

    LegacySceneModelBuilder<T> includeNodes(String... names) {
        includeNodes.addAll(Arrays.asList(names));
        return this;
    }

    LegacySceneModelBuilder<T> includeSubtrees(String... names) {
        includeSubtrees.addAll(Arrays.asList(names));
        return this;
    }

    LegacySceneModelBuilder<T> excludeNodes(String... names) {
        excludeNodes.addAll(Arrays.asList(names));
        return this;
    }

    LegacySceneModelBuilder<T> excludeSubtrees(String... names) {
        excludeSubtrees.addAll(Arrays.asList(names));
        return this;
    }

    LegacySceneModelBuilder<T> translation(float x, float y, float z) {
        translation = new float[]{x, y, z};
        return this;
    }

    @Override
    public JsonObject toJson(JsonObject json) {
        if (model == null) {
            throw new IllegalStateException("Legacy scene model location is required");
        }
        JsonObject result = super.toJson(json);
        result.addProperty("model", model.toString());
        addStrings(result, "include_nodes", includeNodes);
        addStrings(result, "include_subtrees", includeSubtrees);
        addStrings(result, "exclude_nodes", excludeNodes);
        addStrings(result, "exclude_subtrees", excludeSubtrees);
        if (translation != null) {
            JsonArray values = new JsonArray();
            for (float value : translation) {
                values.add(value);
            }
            result.add("translation", values);
        }
        return result;
    }

    private static void addStrings(JsonObject json, String key, List<String> values) {
        if (values.isEmpty()) {
            return;
        }
        JsonArray array = new JsonArray();
        values.stream().sorted().forEach(array::add);
        json.add(key, array);
    }
}
