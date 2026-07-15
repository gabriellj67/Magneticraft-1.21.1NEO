package committee.nova.mods.magneticraft.client.model;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** glTF 2.0 reader mapped into the render-neutral legacy model scene. */
public final class GltfModelParser {
    private static final String MISSING_TEXTURE = "minecraft:missingno";
    private static final int MODE_TRIANGLES = 4;

    private GltfModelParser() {
    }

    @FunctionalInterface
    public interface ResourceResolver {
        InputStream open(String relativeUri) throws IOException;
    }

    public static ModelScene parse(String resource, Reader reader, ResourceResolver resolver)
            throws ModelParseException {
        JsonObject root = parseRoot(resource, reader);
        validateAsset(resource, root);
        List<byte[]> buffers = parseBuffers(resource, arrayOrEmpty(resource, root, "buffers", "$"), resolver);
        List<BufferView> bufferViews = parseBufferViews(
                resource,
                arrayOrEmpty(resource, root, "bufferViews", "$"),
                buffers
        );
        List<Accessor> accessors = parseAccessors(
                resource,
                arrayOrEmpty(resource, root, "accessors", "$"),
                buffers,
                bufferViews
        );
        List<MaterialDefinition> materials = parseMaterials(resource, root);
        List<List<ModelScene.Primitive>> meshes = parseMeshes(resource, root, accessors, materials);
        List<ModelScene.Node> nodes = parseNodes(resource, root, meshes);
        List<Integer> roots = parseSceneRoots(resource, root, nodes.size());
        validateTree(resource, roots, nodes);
        List<ModelScene.Animation> animations = parseAnimations(resource, root, accessors, nodes.size());
        String particle = materials.stream()
                .map(MaterialDefinition::texture)
                .filter(texture -> !MISSING_TEXTURE.equals(texture))
                .findFirst()
                .orElse(MISSING_TEXTURE);
        return new ModelScene(
                ModelScene.Format.GLTF,
                true,
                true,
                particle,
                roots,
                nodes,
                animations
        );
    }

    private static void validateAsset(String resource, JsonObject root) throws ModelParseException {
        JsonObject asset = McxModelParser.requiredObject(resource, root, "asset", "$" );
        String version = McxModelParser.requiredString(resource, asset, "version", "$.asset");
        if (!version.startsWith("2.")) {
            throw new ModelParseException(resource, "$.asset.version", "only glTF 2.x is supported, found " + version);
        }
    }

    private static List<byte[]> parseBuffers(
            String resource,
            JsonArray definitions,
            ResourceResolver resolver
    ) throws ModelParseException {
        List<byte[]> buffers = new ArrayList<>(definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            String path = "$.buffers[" + index + "]";
            JsonObject definition = McxModelParser.object(resource, definitions.get(index), path);
            String uri = McxModelParser.requiredString(resource, definition, "uri", path);
            int byteLength = McxModelParser.requiredInt(resource, definition, "byteLength", path);
            if (byteLength < 0) {
                throw new ModelParseException(resource, path + ".byteLength", "must not be negative");
            }
            byte[] bytes;
            try {
                bytes = uri.startsWith("data:") ? decodeDataUri(uri) : readAll(resolver, uri);
            } catch (IOException | IllegalArgumentException exception) {
                throw new ModelParseException(resource, path + ".uri", "cannot load buffer " + uri, exception);
            }
            if (bytes.length != byteLength) {
                throw new ModelParseException(
                        resource,
                        path + ".byteLength",
                        "declares " + byteLength + " bytes but resource contains " + bytes.length
                );
            }
            buffers.add(bytes);
        }
        return buffers;
    }

    private static byte[] readAll(ResourceResolver resolver, String uri) throws IOException {
        try (InputStream input = resolver.open(uri)) {
            if (input == null) {
                throw new IOException("resolver returned null");
            }
            return input.readAllBytes();
        }
    }

    private static byte[] decodeDataUri(String uri) {
        int comma = uri.indexOf(',');
        if (comma < 0) {
            throw new IllegalArgumentException("data URI has no payload");
        }
        String metadata = uri.substring(5, comma);
        String payload = uri.substring(comma + 1);
        if (metadata.endsWith(";base64")) {
            return Base64.getDecoder().decode(payload);
        }
        return java.net.URLDecoder.decode(payload, StandardCharsets.UTF_8).getBytes(StandardCharsets.ISO_8859_1);
    }

    private static List<BufferView> parseBufferViews(
            String resource,
            JsonArray definitions,
            List<byte[]> buffers
    ) throws ModelParseException {
        List<BufferView> views = new ArrayList<>(definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            String path = "$.bufferViews[" + index + "]";
            JsonObject definition = McxModelParser.object(resource, definitions.get(index), path);
            int buffer = McxModelParser.requiredInt(resource, definition, "buffer", path);
            int offset = optionalInt(resource, definition, "byteOffset", path, 0);
            int length = McxModelParser.requiredInt(resource, definition, "byteLength", path);
            int stride = optionalInt(resource, definition, "byteStride", path, 0);
            checkIndex(resource, path + ".buffer", buffer, buffers.size());
            if (offset < 0 || length < 0 || offset > buffers.get(buffer).length - length) {
                throw new ModelParseException(resource, path, "buffer view exceeds buffer bounds");
            }
            if (stride != 0 && (stride < 4 || stride > 252 || stride % 4 != 0)) {
                throw new ModelParseException(resource, path + ".byteStride", "must be 4-byte aligned in range 4..252");
            }
            views.add(new BufferView(buffer, offset, length, stride));
        }
        return views;
    }

    private static List<Accessor> parseAccessors(
            String resource,
            JsonArray definitions,
            List<byte[]> buffers,
            List<BufferView> views
    ) throws ModelParseException {
        List<Accessor> accessors = new ArrayList<>(definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            String path = "$.accessors[" + index + "]";
            JsonObject definition = McxModelParser.object(resource, definitions.get(index), path);
            if (definition.has("sparse")) {
                throw new ModelParseException(resource, path + ".sparse", "sparse accessors are not supported");
            }
            int viewIndex = McxModelParser.requiredInt(resource, definition, "bufferView", path);
            checkIndex(resource, path + ".bufferView", viewIndex, views.size());
            int byteOffset = optionalInt(resource, definition, "byteOffset", path, 0);
            int componentType = McxModelParser.requiredInt(resource, definition, "componentType", path);
            int count = McxModelParser.requiredInt(resource, definition, "count", path);
            String typeName = McxModelParser.requiredString(resource, definition, "type", path);
            int width = typeWidth(resource, path + ".type", typeName);
            boolean normalized = optionalBoolean(resource, definition, "normalized", path, false);
            Component component = Component.of(resource, path + ".componentType", componentType);
            BufferView view = views.get(viewIndex);
            int elementSize = component.bytes * width;
            int stride = view.stride == 0 ? elementSize : view.stride;
            if (stride < elementSize) {
                throw new ModelParseException(resource, path, "buffer stride is smaller than accessor element");
            }
            if (byteOffset < 0 || count < 0) {
                throw new ModelParseException(resource, path, "accessor offset and count must not be negative");
            }
            long required = count == 0 ? byteOffset : (long) byteOffset + (long) (count - 1) * stride + elementSize;
            if (required > view.length) {
                throw new ModelParseException(resource, path, "accessor exceeds buffer view bounds");
            }
            accessors.add(new Accessor(
                    buffers.get(view.buffer),
                    view.offset + byteOffset,
                    stride,
                    count,
                    width,
                    component,
                    normalized,
                    resource,
                    path
            ));
        }
        return accessors;
    }

    private static List<MaterialDefinition> parseMaterials(String resource, JsonObject root)
            throws ModelParseException {
        JsonArray images = arrayOrEmpty(resource, root, "images", "$");
        List<String> imageUris = new ArrayList<>(images.size());
        for (int index = 0; index < images.size(); index++) {
            JsonObject image = McxModelParser.object(resource, images.get(index), "$.images[" + index + "]");
            if (image.has("bufferView")) {
                throw new ModelParseException(resource, "$.images[" + index + "]", "embedded images are not supported");
            }
            imageUris.add(McxModelParser.requiredString(resource, image, "uri", "$.images[" + index + "]"));
        }

        JsonArray textureDefinitions = arrayOrEmpty(resource, root, "textures", "$");
        List<String> textures = new ArrayList<>(textureDefinitions.size());
        for (int index = 0; index < textureDefinitions.size(); index++) {
            String path = "$.textures[" + index + "]";
            JsonObject texture = McxModelParser.object(resource, textureDefinitions.get(index), path);
            int source = McxModelParser.requiredInt(resource, texture, "source", path);
            checkIndex(resource, path + ".source", source, imageUris.size());
            textures.add(imageUris.get(source));
        }

        JsonArray materials = arrayOrEmpty(resource, root, "materials", "$");
        List<MaterialDefinition> result = new ArrayList<>(materials.size());
        for (int index = 0; index < materials.size(); index++) {
            String path = "$.materials[" + index + "]";
            JsonObject material = McxModelParser.object(resource, materials.get(index), path);
            String name = McxModelParser.optionalString(resource, material, "name", path);
            String texture = MISSING_TEXTURE;
            if (material.has("pbrMetallicRoughness")) {
                JsonObject pbr = McxModelParser.object(
                        resource,
                        material.get("pbrMetallicRoughness"),
                        path + ".pbrMetallicRoughness"
                );
                if (pbr.has("baseColorTexture")) {
                    JsonObject reference = McxModelParser.object(
                            resource,
                            pbr.get("baseColorTexture"),
                            path + ".pbrMetallicRoughness.baseColorTexture"
                    );
                    int textureIndex = McxModelParser.requiredInt(
                            resource,
                            reference,
                            "index",
                            path + ".pbrMetallicRoughness.baseColorTexture"
                    );
                    checkIndex(resource, path + ".pbrMetallicRoughness.baseColorTexture.index", textureIndex, textures.size());
                    texture = textures.get(textureIndex);
                }
            }
            result.add(new MaterialDefinition(name, texture));
        }
        return result;
    }

    private static List<List<ModelScene.Primitive>> parseMeshes(
            String resource,
            JsonObject root,
            List<Accessor> accessors,
            List<MaterialDefinition> materials
    ) throws ModelParseException {
        JsonArray meshes = arrayOrEmpty(resource, root, "meshes", "$");
        List<List<ModelScene.Primitive>> result = new ArrayList<>(meshes.size());
        for (int meshIndex = 0; meshIndex < meshes.size(); meshIndex++) {
            String meshPath = "$.meshes[" + meshIndex + "]";
            JsonObject mesh = McxModelParser.object(resource, meshes.get(meshIndex), meshPath);
            JsonArray primitives = McxModelParser.requiredArray(resource, mesh, "primitives", meshPath);
            List<ModelScene.Primitive> parsed = new ArrayList<>(primitives.size());
            for (int primitiveIndex = 0; primitiveIndex < primitives.size(); primitiveIndex++) {
                String path = meshPath + ".primitives[" + primitiveIndex + "]";
                JsonObject primitive = McxModelParser.object(resource, primitives.get(primitiveIndex), path);
                int mode = optionalInt(resource, primitive, "mode", path, MODE_TRIANGLES);
                if (mode != MODE_TRIANGLES) {
                    throw new ModelParseException(resource, path + ".mode", "only TRIANGLES (4) is supported");
                }
                JsonObject attributes = McxModelParser.requiredObject(resource, primitive, "attributes", path);
                int positionIndex = McxModelParser.requiredInt(resource, attributes, "POSITION", path + ".attributes");
                Accessor positions = accessor(resource, path + ".attributes.POSITION", accessors, positionIndex, 3);
                Accessor textureCoordinates = optionalAccessor(
                        resource,
                        path + ".attributes.TEXCOORD_0",
                        accessors,
                        attributes,
                        "TEXCOORD_0",
                        2
                );
                Accessor normals = optionalAccessor(
                        resource,
                        path + ".attributes.NORMAL",
                        accessors,
                        attributes,
                        "NORMAL",
                        3
                );
                int[] indices;
                if (primitive.has("indices")) {
                    int accessorIndex = McxModelParser.requiredInt(resource, primitive, "indices", path);
                    Accessor indexAccessor = accessor(resource, path + ".indices", accessors, accessorIndex, 1);
                    indices = indexAccessor.indices();
                } else {
                    indices = new int[positions.count];
                    for (int index = 0; index < indices.length; index++) {
                        indices[index] = index;
                    }
                }
                if (indices.length % 3 != 0) {
                    throw new ModelParseException(resource, path, "triangle index count must be divisible by three");
                }
                validateAttributeCounts(resource, path, positions, textureCoordinates, normals);
                String texture = MISSING_TEXTURE;
                String materialName = null;
                if (primitive.has("material")) {
                    int material = McxModelParser.requiredInt(resource, primitive, "material", path);
                    checkIndex(resource, path + ".material", material, materials.size());
                    MaterialDefinition definition = materials.get(material);
                    texture = definition.texture();
                    materialName = definition.name();
                }
                parsed.add(expandTriangles(
                        resource,
                        path,
                        positions,
                        textureCoordinates,
                        normals,
                        indices,
                        texture,
                        materialName
                ));
            }
            result.add(List.copyOf(parsed));
        }
        return result;
    }

    private static void validateAttributeCounts(
            String resource,
            String path,
            Accessor positions,
            Accessor textureCoordinates,
            Accessor normals
    ) throws ModelParseException {
        if (textureCoordinates != null && textureCoordinates.count != positions.count) {
            throw new ModelParseException(resource, path, "TEXCOORD_0 count does not match POSITION");
        }
        if (normals != null && normals.count != positions.count) {
            throw new ModelParseException(resource, path, "NORMAL count does not match POSITION");
        }
    }

    private static ModelScene.Primitive expandTriangles(
            String resource,
            String path,
            Accessor positions,
            Accessor textureCoordinates,
            Accessor normals,
            int[] indices,
            String texture,
            String materialName
    ) throws ModelParseException {
        int faceCount = indices.length / 3;
        float[] outputPositions = new float[faceCount * 12];
        float[] outputTextureCoordinates = new float[faceCount * 8];
        float[] outputNormals = new float[faceCount * 12];
        for (int face = 0; face < faceCount; face++) {
            int a = indices[face * 3];
            int b = indices[face * 3 + 1];
            int c = indices[face * 3 + 2];
            checkVertexIndex(resource, path, a, positions.count);
            checkVertexIndex(resource, path, b, positions.count);
            checkVertexIndex(resource, path, c, positions.count);
            int[] vertices = {a, b, c, c};
            float[] faceNormal = null;
            if (normals == null) {
                float[] positionA = positions.floats(a);
                float[] positionB = positions.floats(b);
                float[] positionC = positions.floats(c);
                faceNormal = McxModelParser.normalizedCross(
                        positionB[0] - positionA[0],
                        positionB[1] - positionA[1],
                        positionB[2] - positionA[2],
                        positionC[0] - positionA[0],
                        positionC[1] - positionA[1],
                        positionC[2] - positionA[2]
                );
            }
            for (int vertex = 0; vertex < 4; vertex++) {
                copy(positions.floats(vertices[vertex]), outputPositions, face * 12 + vertex * 3);
                if (textureCoordinates != null) {
                    copy(textureCoordinates.floats(vertices[vertex]), outputTextureCoordinates, face * 8 + vertex * 2);
                }
                copy(normals == null ? faceNormal : normals.floats(vertices[vertex]), outputNormals, face * 12 + vertex * 3);
            }
        }
        return new ModelScene.Primitive(
                texture,
                null,
                materialName,
                outputPositions,
                outputTextureCoordinates,
                outputNormals
        );
    }

    private static List<ModelScene.Node> parseNodes(
            String resource,
            JsonObject root,
            List<List<ModelScene.Primitive>> meshes
    ) throws ModelParseException {
        JsonArray definitions = arrayOrEmpty(resource, root, "nodes", "$");
        List<ModelScene.Node> nodes = new ArrayList<>(definitions.size());
        for (int index = 0; index < definitions.size(); index++) {
            String path = "$.nodes[" + index + "]";
            JsonObject definition = McxModelParser.object(resource, definitions.get(index), path);
            String name = McxModelParser.optionalString(resource, definition, "name", path);
            List<Integer> children = integerList(resource, definition, "children", path);
            for (int child : children) {
                checkIndex(resource, path + ".children", child, definitions.size());
            }
            List<ModelScene.Primitive> primitives = List.of();
            if (definition.has("mesh")) {
                int mesh = McxModelParser.requiredInt(resource, definition, "mesh", path);
                checkIndex(resource, path + ".mesh", mesh, meshes.size());
                primitives = meshes.get(mesh);
            }
            nodes.add(new ModelScene.Node(index, name, parseTransform(resource, definition, path), children, primitives));
        }
        return nodes;
    }

    private static ModelTransform parseTransform(String resource, JsonObject node, String path) throws ModelParseException {
        if (node.has("matrix")) {
            if (node.has("translation") || node.has("rotation") || node.has("scale")) {
                throw new ModelParseException(resource, path, "node matrix cannot be combined with TRS properties");
            }
            return ModelTransform.fromMatrix(floatArray(resource, node.get("matrix"), path + ".matrix", 16));
        }
        float[] translation = optionalFloatArray(resource, node, "translation", path, 3, new float[]{0.0F, 0.0F, 0.0F});
        float[] rotation = optionalFloatArray(resource, node, "rotation", path, 4, new float[]{0.0F, 0.0F, 0.0F, 1.0F});
        float[] scale = optionalFloatArray(resource, node, "scale", path, 3, new float[]{1.0F, 1.0F, 1.0F});
        try {
            return ModelTransform.of(translation, rotation, scale);
        } catch (IllegalArgumentException exception) {
            throw new ModelParseException(resource, path, "invalid node transform", exception);
        }
    }

    private static List<Integer> parseSceneRoots(String resource, JsonObject root, int nodeCount)
            throws ModelParseException {
        JsonArray scenes = arrayOrEmpty(resource, root, "scenes", "$");
        if (scenes.isEmpty()) {
            throw new ModelParseException(resource, "$.scenes", "at least one scene is required");
        }
        int sceneIndex = optionalInt(resource, root, "scene", "$", 0);
        checkIndex(resource, "$.scene", sceneIndex, scenes.size());
        JsonObject scene = McxModelParser.object(resource, scenes.get(sceneIndex), "$.scenes[" + sceneIndex + "]");
        List<Integer> roots = integerList(resource, scene, "nodes", "$.scenes[" + sceneIndex + "]");
        for (int rootNode : roots) {
            checkIndex(resource, "$.scenes[" + sceneIndex + "].nodes", rootNode, nodeCount);
        }
        return roots;
    }

    private static void validateTree(String resource, List<Integer> roots, List<ModelScene.Node> nodes)
            throws ModelParseException {
        Set<Integer> active = new HashSet<>();
        Set<Integer> visited = new HashSet<>();
        for (int root : roots) {
            visitNode(resource, root, nodes, active, visited);
        }
    }

    private static void visitNode(
            String resource,
            int index,
            List<ModelScene.Node> nodes,
            Set<Integer> active,
            Set<Integer> visited
    ) throws ModelParseException {
        if (!active.add(index)) {
            throw new ModelParseException(resource, "$.nodes[" + index + "]", "node hierarchy contains a cycle");
        }
        if (visited.add(index)) {
            for (int child : nodes.get(index).children()) {
                visitNode(resource, child, nodes, active, visited);
            }
        }
        active.remove(index);
    }

    private static List<ModelScene.Animation> parseAnimations(
            String resource,
            JsonObject root,
            List<Accessor> accessors,
            int nodeCount
    ) throws ModelParseException {
        JsonArray definitions = arrayOrEmpty(resource, root, "animations", "$");
        List<ModelScene.Animation> animations = new ArrayList<>(definitions.size());
        for (int animationIndex = 0; animationIndex < definitions.size(); animationIndex++) {
            String path = "$.animations[" + animationIndex + "]";
            JsonObject definition = McxModelParser.object(resource, definitions.get(animationIndex), path);
            String name = McxModelParser.optionalString(resource, definition, "name", path);
            if (name == null) {
                name = Integer.toString(animationIndex);
            }
            JsonArray samplers = McxModelParser.requiredArray(resource, definition, "samplers", path);
            JsonArray channels = McxModelParser.requiredArray(resource, definition, "channels", path);
            List<ModelScene.Channel> parsedChannels = new ArrayList<>(channels.size());
            float duration = 0.0F;
            for (int channelIndex = 0; channelIndex < channels.size(); channelIndex++) {
                String channelPath = path + ".channels[" + channelIndex + "]";
                JsonObject channel = McxModelParser.object(resource, channels.get(channelIndex), channelPath);
                int samplerIndex = McxModelParser.requiredInt(resource, channel, "sampler", channelPath);
                checkIndex(resource, channelPath + ".sampler", samplerIndex, samplers.size());
                JsonObject sampler = McxModelParser.object(
                        resource,
                        samplers.get(samplerIndex),
                        path + ".samplers[" + samplerIndex + "]"
                );
                int inputIndex = McxModelParser.requiredInt(resource, sampler, "input", path + ".samplers[" + samplerIndex + "]");
                int outputIndex = McxModelParser.requiredInt(resource, sampler, "output", path + ".samplers[" + samplerIndex + "]");
                Accessor input = accessor(resource, channelPath + ".sampler.input", accessors, inputIndex, 1);
                Accessor output = accessorAnyWidth(resource, channelPath + ".sampler.output", accessors, outputIndex);
                String interpolationName = sampler.has("interpolation")
                        ? McxModelParser.requiredString(resource, sampler, "interpolation", path + ".samplers[" + samplerIndex + "]")
                        : "LINEAR";
                ModelScene.Interpolation interpolation;
                try {
                    interpolation = ModelScene.Interpolation.valueOf(interpolationName.toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException exception) {
                    throw new ModelParseException(
                            resource,
                            path + ".samplers[" + samplerIndex + "].interpolation",
                            "unsupported interpolation " + interpolationName
                    );
                }
                JsonObject target = McxModelParser.requiredObject(resource, channel, "target", channelPath);
                int node = McxModelParser.requiredInt(resource, target, "node", channelPath + ".target");
                checkIndex(resource, channelPath + ".target.node", node, nodeCount);
                String targetPath = McxModelParser.requiredString(resource, target, "path", channelPath + ".target");
                ModelScene.Path animationPath = parseAnimationPath(resource, channelPath + ".target.path", targetPath);
                if (output.width != animationPath.width()) {
                    throw new ModelParseException(resource, channelPath, "animation output width does not match " + targetPath);
                }
                if (input.count != output.count) {
                    throw new ModelParseException(resource, channelPath, "animation input and output keyframe counts differ");
                }
                float[] times = input.allFloats();
                validateTimes(resource, channelPath, times);
                float[] values = output.allFloats();
                duration = Math.max(duration, times[times.length - 1]);
                parsedChannels.add(new ModelScene.Channel(
                        node,
                        animationPath,
                        interpolation,
                        times,
                        values,
                        animationPath.width()
                ));
            }
            animations.add(new ModelScene.Animation(name, duration, parsedChannels));
        }
        return animations;
    }

    private static void validateTimes(String resource, String path, float[] times) throws ModelParseException {
        for (int index = 0; index < times.length; index++) {
            if (times[index] < 0.0F || (index > 0 && times[index] <= times[index - 1])) {
                throw new ModelParseException(resource, path, "animation times must be non-negative and strictly increasing");
            }
        }
    }

    private static ModelScene.Path parseAnimationPath(String resource, String path, String value)
            throws ModelParseException {
        return switch (value) {
            case "translation" -> ModelScene.Path.TRANSLATION;
            case "rotation" -> ModelScene.Path.ROTATION;
            case "scale" -> ModelScene.Path.SCALE;
            case "weights" -> throw new ModelParseException(resource, path, "morph target weights are not supported");
            default -> throw new ModelParseException(resource, path, "unknown animation path " + value);
        };
    }

    private static Accessor accessor(
            String resource,
            String path,
            List<Accessor> accessors,
            int index,
            int width
    ) throws ModelParseException {
        Accessor accessor = accessorAnyWidth(resource, path, accessors, index);
        if (accessor.width != width) {
            throw new ModelParseException(resource, path, "expected accessor width " + width + " but found " + accessor.width);
        }
        return accessor;
    }

    private static Accessor accessorAnyWidth(String resource, String path, List<Accessor> accessors, int index)
            throws ModelParseException {
        checkIndex(resource, path, index, accessors.size());
        return accessors.get(index);
    }

    private static Accessor optionalAccessor(
            String resource,
            String path,
            List<Accessor> accessors,
            JsonObject attributes,
            String key,
            int width
    ) throws ModelParseException {
        if (!attributes.has(key)) {
            return null;
        }
        int index = McxModelParser.requiredInt(resource, attributes, key, path.substring(0, path.lastIndexOf('.')));
        return accessor(resource, path, accessors, index, width);
    }

    private static int typeWidth(String resource, String path, String type) throws ModelParseException {
        return switch (type) {
            case "SCALAR" -> 1;
            case "VEC2" -> 2;
            case "VEC3" -> 3;
            case "VEC4" -> 4;
            default -> throw new ModelParseException(resource, path, "unsupported accessor type " + type);
        };
    }

    private static JsonObject parseRoot(String resource, Reader reader) throws ModelParseException {
        try {
            return McxModelParser.object(resource, JsonParser.parseReader(reader), "$");
        } catch (RuntimeException exception) {
            throw new ModelParseException(resource, "$", "invalid glTF JSON", exception);
        }
    }

    private static JsonArray arrayOrEmpty(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        if (!object.has(key)) {
            return new JsonArray();
        }
        return McxModelParser.array(resource, object.get(key), path + "." + key);
    }

    private static List<Integer> integerList(String resource, JsonObject object, String key, String path)
            throws ModelParseException {
        if (!object.has(key)) {
            return List.of();
        }
        JsonArray array = McxModelParser.array(resource, object.get(key), path + "." + key);
        List<Integer> values = new ArrayList<>(array.size());
        for (int index = 0; index < array.size(); index++) {
            values.add(McxModelParser.integer(resource, array.get(index), path + "." + key + "[" + index + "]"));
        }
        return List.copyOf(values);
    }

    private static int optionalInt(String resource, JsonObject object, String key, String path, int fallback)
            throws ModelParseException {
        return object.has(key) ? McxModelParser.requiredInt(resource, object, key, path) : fallback;
    }

    private static boolean optionalBoolean(
            String resource,
            JsonObject object,
            String key,
            String path,
            boolean fallback
    ) throws ModelParseException {
        if (!object.has(key)) {
            return fallback;
        }
        JsonElement element = object.get(key);
        if (!element.isJsonPrimitive() || !element.getAsJsonPrimitive().isBoolean()) {
            throw new ModelParseException(resource, path + "." + key, "expected boolean");
        }
        return element.getAsBoolean();
    }

    private static float[] optionalFloatArray(
            String resource,
            JsonObject object,
            String key,
            String path,
            int width,
            float[] fallback
    ) throws ModelParseException {
        return object.has(key) ? floatArray(resource, object.get(key), path + "." + key, width) : fallback;
    }

    private static float[] floatArray(String resource, JsonElement element, String path, int width)
            throws ModelParseException {
        JsonArray array = McxModelParser.array(resource, element, path);
        if (array.size() != width) {
            throw new ModelParseException(resource, path, "expected " + width + " values");
        }
        float[] values = new float[width];
        for (int index = 0; index < width; index++) {
            values[index] = McxModelParser.number(resource, array.get(index), path + "[" + index + "]");
        }
        return values;
    }

    private static void checkIndex(String resource, String path, int index, int size) throws ModelParseException {
        if (index < 0 || index >= size) {
            throw new ModelParseException(resource, path, "index " + index + " is outside 0.." + (size - 1));
        }
    }

    private static void checkVertexIndex(String resource, String path, int index, int count)
            throws ModelParseException {
        if (index < 0 || index >= count) {
            throw new ModelParseException(resource, path, "vertex index " + index + " is outside 0.." + (count - 1));
        }
    }

    private static void copy(float[] source, float[] target, int offset) {
        System.arraycopy(source, 0, target, offset, source.length);
    }

    private record BufferView(int buffer, int offset, int length, int stride) {
    }

    private record MaterialDefinition(String name, String texture) {
    }

    private static final class Accessor {
        private final byte[] buffer;
        private final int offset;
        private final int stride;
        private final int count;
        private final int width;
        private final Component component;
        private final boolean normalized;
        private final String resource;
        private final String path;

        private Accessor(
                byte[] buffer,
                int offset,
                int stride,
                int count,
                int width,
                Component component,
                boolean normalized,
                String resource,
                String path
        ) {
            this.buffer = buffer;
            this.offset = offset;
            this.stride = stride;
            this.count = count;
            this.width = width;
            this.component = component;
            this.normalized = normalized;
            this.resource = resource;
            this.path = path;
        }

        private float[] floats(int element) throws ModelParseException {
            float[] values = new float[width];
            for (int componentIndex = 0; componentIndex < width; componentIndex++) {
                values[componentIndex] = component.readFloat(
                        buffer,
                        offset + element * stride + componentIndex * component.bytes,
                        normalized
                );
            }
            return values;
        }

        private float[] allFloats() throws ModelParseException {
            float[] values = new float[count * width];
            for (int element = 0; element < count; element++) {
                copy(floats(element), values, element * width);
            }
            return values;
        }

        private int[] indices() throws ModelParseException {
            if (width != 1 || !component.integer || component.signed) {
                throw new ModelParseException(resource, path, "indices must use an unsigned scalar accessor");
            }
            int[] values = new int[count];
            for (int index = 0; index < count; index++) {
                values[index] = component.readIndex(buffer, offset + index * stride, resource, path);
            }
            return values;
        }
    }

    private enum Component {
        BYTE(5120, 1, true, true),
        UNSIGNED_BYTE(5121, 1, true, false),
        SHORT(5122, 2, true, true),
        UNSIGNED_SHORT(5123, 2, true, false),
        UNSIGNED_INT(5125, 4, true, false),
        FLOAT(5126, 4, false, true);

        private final int id;
        private final int bytes;
        private final boolean integer;
        private final boolean signed;

        Component(int id, int bytes, boolean integer, boolean signed) {
            this.id = id;
            this.bytes = bytes;
            this.integer = integer;
            this.signed = signed;
        }

        private static Component of(String resource, String path, int id) throws ModelParseException {
            for (Component component : values()) {
                if (component.id == id) {
                    return component;
                }
            }
            throw new ModelParseException(resource, path, "unsupported component type " + id);
        }

        private float readFloat(byte[] bytes, int offset, boolean normalized) {
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            return switch (this) {
                case FLOAT -> buffer.getFloat(offset);
                case BYTE -> normalized ? Math.max(buffer.get(offset) / 127.0F, -1.0F) : buffer.get(offset);
                case UNSIGNED_BYTE -> normalized
                        ? Byte.toUnsignedInt(buffer.get(offset)) / 255.0F
                        : Byte.toUnsignedInt(buffer.get(offset));
                case SHORT -> normalized ? Math.max(buffer.getShort(offset) / 32767.0F, -1.0F) : buffer.getShort(offset);
                case UNSIGNED_SHORT -> normalized
                        ? Short.toUnsignedInt(buffer.getShort(offset)) / 65535.0F
                        : Short.toUnsignedInt(buffer.getShort(offset));
                case UNSIGNED_INT -> {
                    long value = Integer.toUnsignedLong(buffer.getInt(offset));
                    yield normalized ? (float) (value / 4294967295.0D) : (float) value;
                }
            };
        }

        private int readIndex(byte[] bytes, int offset, String resource, String path) throws ModelParseException {
            ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
            return switch (this) {
                case UNSIGNED_BYTE -> Byte.toUnsignedInt(buffer.get(offset));
                case UNSIGNED_SHORT -> Short.toUnsignedInt(buffer.getShort(offset));
                case UNSIGNED_INT -> {
                    long value = Integer.toUnsignedLong(buffer.getInt(offset));
                    if (value > Integer.MAX_VALUE) {
                        throw new ModelParseException(resource, path, "index exceeds Java integer range");
                    }
                    yield (int) value;
                }
                default -> throw new ModelParseException(resource, path, "invalid index component type " + id);
            };
        }
    }
}
