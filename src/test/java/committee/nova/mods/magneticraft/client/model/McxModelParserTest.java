package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.io.StringReader;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class McxModelParserTest {
    @Test
    void parsesOriginalModelLoaderSchemaAndPartRange() throws Exception {
        ModelScene scene = McxModelParser.parse("magneticraft:test.mcx", new StringReader(mcx(0, 1)));

        assertEquals(ModelScene.Format.MCX, scene.format());
        assertTrue(scene.ambientOcclusion());
        assertTrue(scene.gui3d());
        assertEquals("magneticraft:blocks/test", scene.particleTexture());
        assertEquals(1, scene.nodes().size());
        assertEquals("rotor", scene.node(0).name());
        assertEquals(1, scene.node(0).primitives().get(0).faceCount());
        assertEquals(ModelScene.AlphaMode.MASK, scene.alphaMode());
        assertEquals("minecraft:cutout", LegacySceneGeometry.inferredRenderType(scene.alphaMode()).toString());
        assertEquals(Set.of(0), ModelSelection.nodes(scene, ModelSelection.exact("rotor")));
    }

    @Test
    void rejectsPartRangeOutsideQuadIndexListWithResourceContext() {
        ModelParseException exception = assertThrows(
                ModelParseException.class,
                () -> McxModelParser.parse("magneticraft:broken.mcx", new StringReader(mcx(0, 2)))
        );

        assertTrue(exception.getMessage().contains("magneticraft:broken.mcx"));
        assertTrue(exception.getMessage().contains("$.parts[0]"));
    }

    private static String mcx(int from, int to) {
        return """
                {
                  "useAmbientOcclusion": true,
                  "use3dInGui": true,
                  "particleTexture": "magneticraft:blocks/test",
                  "parts": [
                    {
                      "name": "rotor",
                      "from": %d,
                      "to": %d,
                      "side": "north",
                      "texture": "magneticraft:blocks/test"
                    }
                  ],
                  "quads": {
                    "pos": [[0,0,0], [1,0,0], [1,1,0], [0,1,0]],
                    "tex": [[0,0], [1,0], [1,1], [0,1]],
                    "indices": [[[0,1,2,3], [0,1,2,3]]]
                  }
                }
                """.formatted(from, to);
    }
}
