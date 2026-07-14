package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/** Strict reader for the ModelLoader 1.12.2 MCX JSON schema. */
public final class McxModelParser {
    private McxModelParser() {
    }

    public static ModelScene parse(String resource, Reader reader) throws ModelParseException {
        JsonObject root = parseRoot(resource, reader);
        boolean ambientOcclusion = requiredBoolean(resource, root, "useAmbientOcclusion", "$");
        boolean gui3d = requiredBoolean(resource, root, "use3dInGui", "$");
        String particle = requiredString(resource, root, "particleTexture", "$");
        JsonArray parts = requiredArray(resource, root, "parts", "$");
        JsonObject quads = requiredObject(resource, root, "quads", "$");
        List<float[]> positions = vectors(resource, requiredArray(resource, quads, "pos", "$.quads"), 3, "$.quads.pos");
        List<float[]> textureCoordinates = vectors(
                resource,
                requiredArray(resource, quads, "tex", "$.quads"),
                2,
                "$.quads.tex"
        );
        JsonArray indices = requiredArray(resource, quads, "indices", "$.quads");

        List<ModelScene.Node> nodes = new ArrayList<>(parts.size());
        for (int partIndex = 0; partIndex < parts.size(); partIndex++) {
            String partPath = "$.parts[" + partIndex + "]";
            JsonObject part = object(resource, parts.get(partIndex), partPath);
            String name = requiredString(resource, part, "name", partPath);
            int from = requiredInt(resource, part, "from", partPath);
            int to = requiredInt(resource, part, "to", partPath);
            String texture = requiredString(resource, part, "texture", partPath);
            String side = optionalString(resource, part, "side", partPath);
            if (from < 0 || to < from || to > indices.size()) {
                throw new ModelParseException(resource, partPath, "part range [" + from + ", " + to
                        + ") is outside " + indices.size() + " quads");
            }
            ModelScene.Primitive primitive = primitive(
                    resource,
                    indices,
                    from,
                    to,
                    positions,
                    textureCoordinates,
                    texture,
                    side
            );
            nodes.add(new ModelScene.Node(
                    partIndex,
                    name,
                    ModelTransform.IDENTITY,
                    List.of(),
                    List.of(primitive)
            ));
        }

        List<Integer> roots = new ArrayList<>(nodes.size());
        for (int index = 0; index < nodes.size(); index++) {
            roots.add(index);
        }
        return new ModelScene(
                ModelScene.Format.MCX,
                ambientOcclusion,
                gui3d,
                particle,
                roots,
                nodes,
                List.of()
        );
    }

    private static ModelScene.Primitive primitive(
            String resource,
            JsonArray indices,
            int from,
            int to,
            List<float[]> positions,
            List<float[]> textureCoordinates,
            String texture,
            String side
    ) throws ModelParseException {
        int faceCount = to - from;
        float[] outputPositions = new float[faceCount * 12];
        float[] outputTextureCoordinates = new float[faceCount * 8];
        float[] outputNormals = new float[faceCount * 12];
        for (int sourceFace = from; sourceFace < to; sourceFace++) {
            String path = "$.quads.indices[" + sourceFace + "]";
            JsonArray pair = array(resource, indices.get(sourceFace), path);
            if (pair.size() != 2) {
                throw new ModelParseException(resource, path, "MCX index entry must contain position and UV arrays");
            }
            int[] positionIndices = fourIndices(resource, pair.get(0), path + "[0]", positions.size());
            int[] uvIndices = fourIndices(resource, pair.get(1), path + "[1]", textureCoordinates.size());
            int targetFace = sourceFace - from;
            for (int vertex = 0; vertex < 4; vertex++) {
                copy(positions.get(positionIndices[vertex]), outputPositions, targetFace * 12 + vertex * 3);
                copy(textureCoordinates.get(uvIndices[vertex]), outputTextureCoordinates, targetFace * 8 + vertex * 2);
            }
            float[] normal = quadNormal(
                    positions.get(positionIndices[0]),
                    positions.get(positionIndices[1]),
                    positions.get(positionIndices[2]),
                    positions.get(positionIndices[3])
            );
            for (int vertex = 0; vertex < 4; vertex++) {
                copy(normal, outputNormals, targetFace * 12 + vertex * 3);
            }
        }
        return new ModelScene.Primitive(texture, side, outputPositions, outputTextureCoordinates, outputNormals);
    }

    private static float[] quadNormal(float[] a, float[] b, float[] c, float[] d) {
        float acX = c[0] - a[0];
        float acY = c[1] - a[1];
        float acZ = c[2] - a[2];
        float bdX = d[0] - b[0];
        float bdY = d[1] - b[1];
        float bdZ = d[2] - b[2];
        return normalizedCross(acX, acY, acZ, bdX, bdY, bdZ);
    }

    static float[] normalizedCross(float ax, float ay, float az, float bx, float by, float bz) {
        float x = ay * bz - az * by;
        float y = az * bx - ax * bz;
        float z = ax * by - ay * bx;
        float length = (float) Math.sqrt(x * x + y * y + z * z);
        if (length == 0.0F) {
            return new float[]{0.0F, 1.0F, 0.0F};
        }
        return new float[]{x / length, y / length, z / length};
    }

    private static int[] fourIndices(String resource, JsonElement element, String path, int limit)
            throws ModelParseException {
        JsonArray array = array(resource, element, path);
        if (array.size() != 4) {
            throw new ModelParseException(resource, path, "MCX quad index array must contain four entries");
        }
        int[] result = new int[4];
        for (int index = 0; index < 4; index++) {
            result[index] = integer(resource, array.get(index), path + "[" + index + "]");
            if (result[index] < 0 || result[index] >= limit) {
                throw new ModelParseException(resource, path + "[" + index + "]", "index is outside 0.." + (limit - 1));
            }
        }
        return result;
    }

    private static List<float[]> vectors(String resource, JsonArray array, int width, String path)
            throws ModelParseException {
        List<float[]> result = new ArrayList<>(array.size());
        for (int vectorIndex = 0; vectorIndex < array.size(); vectorIndex++) {
            JsonArray vector = array(resource, array.get(vectorIndex), path + "[" + vectorIndex + "]");
            if (vector.size() != width) {
                throw new ModelParseException(resource, path + "[" + vectorIndex + "]", "expected " + width + " values");
            }
            float[] values = new float[width];
            for (int component = 0; component < width; component++) {
                values[component] = number(resource, vector.get(component), path + "[" + vectorIndex + "][" + component + "]");
            }
            result.add(values);
        }
        return result;
    }

    private static JsonObject parseRoot(String resource, Reader reader) throws ModelParseException {
        try {
            JsonElement element = JsonParser.parseReader(reader);
            return object(resource, element, "$");
        } catch (RuntimeException exception) {
            throw new ModelParseException(resource, "$", "invalid MCX JSON", exception);
        }
    }

    static JsonObject object(String resource, JsonElement element, String path) throws ModelParseException {
        if (element == null || !element.isJsonObject()) {
            throw new ModelParseException(resource, path, "expected object");
        }
        return element.getAsJsonObject();
    }

    static JsonArray array(String resource, JsonElement element, String path) throws ModelParseException {
        if (element == null || !element.isJsonArray()) {
            throw new ModelParseException(resource, path, "expected array");
        }
        return element.getAsJsonArray();
    }

    static JsonObject requiredObject(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        return object(resource, object.get(key), path + "." + key);
    }

    static JsonArray requiredArray(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        return array(resource, object.get(key), path + "." + key);
    }

    static String requiredString(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isString()) {
            throw new ModelParseException(resource, path + "." + key, "expected string");
        }
        return element.getAsString();
    }

    static String optionalString(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        if (!object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        return requiredString(resource, object, key, path);
    }

    static boolean requiredBoolean(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        JsonElement element = object.get(key);
        if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new ModelParseException(resource, path + "." + key, "expected boolean");
        }
        return element.getAsBoolean();
    }

    static int requiredInt(String resource, JsonObject object, String key, String path) throws ModelParseException {
        return integer(resource, object.get(key), path + "." + key);
    }

    static int integer(String resource, JsonElement element, String path) throws ModelParseException {
        float value = number(resource, element, path);
        int integer = (int) value;
        if (value != integer) {
            throw new ModelParseException(resource, path, "expected integer");
        }
        return integer;
    }

    static float number(String resource, JsonElement element, String path) throws ModelParseException {
        try {
            if (element == null || !element.isJsonPrimitive() || !element.getAsJsonPrimitive().isNumber()) {
                throw new ModelParseException(resource, path, "expected number");
            }
            float value = element.getAsFloat();
            if (!Float.isFinite(value)) {
                throw new ModelParseException(resource, path, "number must be finite");
            }
            return value;
        } catch (NumberFormatException exception) {
            throw new ModelParseException(resource, path, "invalid number", exception);
        }
    }

    private static void copy(float[] source, float[] target, int offset) {
        System.arraycopy(source, 0, target, offset, source.length);
    }
}
