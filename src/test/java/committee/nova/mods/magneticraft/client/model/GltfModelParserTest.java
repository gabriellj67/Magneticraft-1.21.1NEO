package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Base64;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GltfModelParserTest {
    @Test
    void parsesInterleavedAttributesUnsignedIndicesAndMaterialTexture() throws Exception {
        byte[] buffer = triangleBuffer();
        String encoded = Base64.getEncoder().encodeToString(buffer);
        ModelScene scene = GltfModelParser.parse(
                "magneticraft:models/block/gltf/test.gltf",
                new StringReader(gltf(encoded, buffer.length, 20)),
                uri -> {
                    throw new AssertionError("data URI must not use the external resolver");
                }
        );

        assertEquals(ModelScene.Format.GLTF, scene.format());
        assertEquals("magneticraft:blocks/test", scene.particleTexture());
        assertEquals(1, scene.rootNodes().size());
        ModelScene.Primitive primitive = scene.node(0).primitives().get(0);
        assertEquals(1, primitive.faceCount());
        assertEquals("tier_band", primitive.materialName());
        assertEquals(ModelScene.AlphaMode.OPAQUE, primitive.alphaMode());
        assertEquals(ModelScene.AlphaMode.OPAQUE, scene.alphaMode());
        assertEquals("minecraft:solid", LegacySceneGeometry.inferredRenderType(scene.alphaMode()).toString());
        assertArrayEquals(new float[]{0.0F, 0.0F, 0.0F}, first(primitive.positions(), 3));
        assertArrayEquals(new float[]{0.0F, 1.0F, 0.0F}, last(primitive.positions(), 3));
        assertArrayEquals(new float[]{0.0F, 0.0F, 1.0F}, first(primitive.normals(), 3));
    }

    @Test
    void mapsStandardMaterialAlphaModesToForgeRenderLayers() throws Exception {
        byte[] buffer = triangleBuffer();
        String encoded = Base64.getEncoder().encodeToString(buffer);

        ModelScene masked = parseWithAlphaMode(encoded, buffer.length, "MASK");
        assertEquals(ModelScene.AlphaMode.MASK, masked.alphaMode());
        assertEquals("minecraft:cutout", LegacySceneGeometry.inferredRenderType(masked.alphaMode()).toString());

        ModelScene blended = parseWithAlphaMode(encoded, buffer.length, "BLEND");
        assertEquals(ModelScene.AlphaMode.BLEND, blended.alphaMode());
        assertEquals("minecraft:translucent", LegacySceneGeometry.inferredRenderType(blended.alphaMode()).toString());

        ModelParseException exception = assertThrows(
                ModelParseException.class,
                () -> parseWithAlphaMode(encoded, buffer.length, "CUTOUT")
        );
        assertTrue(exception.getMessage().contains("$.materials[0].alphaMode"));
    }

    @Test
    void rejectsStrideSmallerThanAccessorElementWithContext() {
        byte[] buffer = triangleBuffer();
        String encoded = Base64.getEncoder().encodeToString(buffer);

        ModelParseException exception = assertThrows(
                ModelParseException.class,
                () -> GltfModelParser.parse(
                        "magneticraft:broken.gltf",
                        new StringReader(gltf(encoded, buffer.length, 8)),
                        uri -> null
                )
        );

        assertTrue(exception.getMessage().contains("magneticraft:broken.gltf"));
        assertTrue(exception.getMessage().contains("$.accessors[0]"));
    }

    private static byte[] triangleBuffer() {
        ByteBuffer buffer = ByteBuffer.allocate(63).order(ByteOrder.LITTLE_ENDIAN);
        vertex(buffer, 0.0F, 0.0F, 0.0F, 0.0F, 0.0F);
        vertex(buffer, 1.0F, 0.0F, 0.0F, 1.0F, 0.0F);
        vertex(buffer, 0.0F, 1.0F, 0.0F, 0.0F, 1.0F);
        buffer.put((byte) 0).put((byte) 1).put((byte) 2);
        return buffer.array();
    }

    private static void vertex(ByteBuffer buffer, float x, float y, float z, float u, float v) {
        buffer.putFloat(x).putFloat(y).putFloat(z).putFloat(u).putFloat(v);
    }

    private static String gltf(String encodedBuffer, int byteLength, int stride) {
        return """
                {
                  "asset": {"version": "2.0"},
                  "scene": 0,
                  "scenes": [{"nodes": [0]}],
                  "nodes": [{"name": "triangle", "mesh": 0}],
                  "buffers": [{
                    "uri": "data:application/octet-stream;base64,%s",
                    "byteLength": %d
                  }],
                  "bufferViews": [
                    {"buffer": 0, "byteOffset": 0, "byteLength": 60, "byteStride": %d},
                    {"buffer": 0, "byteOffset": 60, "byteLength": 3}
                  ],
                  "accessors": [
                    {"bufferView": 0, "byteOffset": 0, "componentType": 5126, "count": 3, "type": "VEC3"},
                    {"bufferView": 0, "byteOffset": 12, "componentType": 5126, "count": 3, "type": "VEC2"},
                    {"bufferView": 1, "componentType": 5121, "count": 3, "type": "SCALAR"}
                  ],
                  "images": [{"uri": "magneticraft:blocks/test"}],
                  "textures": [{"source": 0}],
                  "materials": [{"name": "tier_band", "pbrMetallicRoughness": {"baseColorTexture": {"index": 0}}}],
                  "meshes": [{"primitives": [{
                    "attributes": {"POSITION": 0, "TEXCOORD_0": 1},
                    "indices": 2,
                    "material": 0,
                    "mode": 4
                  }]}]
                }
                """.formatted(encodedBuffer, byteLength, stride);
    }

    private static ModelScene parseWithAlphaMode(String encodedBuffer, int byteLength, String alphaMode)
            throws Exception {
        String source = gltf(encodedBuffer, byteLength, 20).replace(
                "\"pbrMetallicRoughness\"",
                "\"alphaMode\": \"" + alphaMode + "\", \"pbrMetallicRoughness\""
        );
        return GltfModelParser.parse(
                "magneticraft:models/block/gltf/alpha_" + alphaMode.toLowerCase() + ".gltf",
                new StringReader(source),
                uri -> {
                    throw new AssertionError("data URI must not use the external resolver");
                }
        );
    }

    private static float[] first(float[] values, int width) {
        return java.util.Arrays.copyOf(values, width);
    }

    private static float[] last(float[] values, int width) {
        return java.util.Arrays.copyOfRange(values, values.length - width, values.length);
    }
}
