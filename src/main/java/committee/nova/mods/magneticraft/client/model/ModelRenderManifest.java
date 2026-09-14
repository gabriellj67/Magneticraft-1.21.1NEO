package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.HashMap;
import java.util.Map;

/** Sidecar metadata that maps standard glTF node/material names to Forge tint indices. */
public record ModelRenderManifest(Map<String, Integer> nodeTints, Map<String, Integer> materialTints) {
    public static final int SCHEMA_VERSION = 1;
    public static final int MAX_TINT_INDEX = 15;
    public static final ModelRenderManifest EMPTY = new ModelRenderManifest(Map.of(), Map.of());

    public ModelRenderManifest {
        nodeTints = Map.copyOf(nodeTints);
        materialTints = Map.copyOf(materialTints);
    }

    public int tintIndex(String nodeName, String materialName) {
        Integer nodeTint = nodeName == null ? null : nodeTints.get(nodeName);
        if (nodeTint != null) {
            return nodeTint;
        }
        Integer materialTint = materialName == null ? null : materialTints.get(materialName);
        return materialTint == null ? -1 : materialTint;
    }

    public static ModelRenderManifest parse(String resource, Reader reader) throws ModelParseException {
        JsonObject root;
        try {
            JsonElement element = JsonParser.parseReader(reader);
            root = McxModelParser.object(resource, element, "$" );
        } catch (RuntimeException exception) {
            throw new ModelParseException(resource, "$", "invalid model render manifest", exception);
        }
        int schemaVersion = McxModelParser.requiredInt(resource, root, "schema_version", "$" );
        if (schemaVersion != SCHEMA_VERSION) {
            throw new ModelParseException(
                    resource,
                    "$.schema_version",
                    "expected schema version " + SCHEMA_VERSION + ", found " + schemaVersion
            );
        }
        JsonArray mappings = McxModelParser.requiredArray(resource, root, "tint_mappings", "$" );
        Map<String, Integer> nodes = new HashMap<>();
        Map<String, Integer> materials = new HashMap<>();
        for (int index = 0; index < mappings.size(); index++) {
            String path = "$.tint_mappings[" + index + "]";
            JsonObject mapping = McxModelParser.object(resource, mappings.get(index), path);
            int tintIndex = McxModelParser.requiredInt(resource, mapping, "tint_index", path);
            if (tintIndex < 0 || tintIndex > MAX_TINT_INDEX) {
                throw new ModelParseException(
                        resource,
                        path + ".tint_index",
                        "must be in [0, " + MAX_TINT_INDEX + "]"
                );
            }
            int names = addNames(resource, mapping, "nodes", path, tintIndex, nodes);
            names += addNames(resource, mapping, "materials", path, tintIndex, materials);
            if (names == 0) {
                throw new ModelParseException(resource, path, "mapping must name at least one node or material");
            }
        }
        return new ModelRenderManifest(nodes, materials);
    }

    private static int addNames(
            String resource,
            JsonObject mapping,
            String key,
            String path,
            int tintIndex,
            Map<String, Integer> target
    ) throws ModelParseException {
        if (!mapping.has(key)) {
            return 0;
        }
        JsonArray names = McxModelParser.array(resource, mapping.get(key), path + "." + key);
        for (int index = 0; index < names.size(); index++) {
            JsonElement element = names.get(index);
            if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
                throw new ModelParseException(resource, path + "." + key + "[" + index + "]", "expected string");
            }
            String name = element.getAsString();
            if (name.isBlank()) {
                throw new ModelParseException(resource, path + "." + key + "[" + index + "]", "must not be blank");
            }
            Integer previous = target.putIfAbsent(name, tintIndex);
            if (previous != null && previous != tintIndex) {
                throw new ModelParseException(
                        resource,
                        path + "." + key + "[" + index + "]",
                        "name " + name + " already maps to tint index " + previous
                );
            }
        }
        return names.size();
    }
}
