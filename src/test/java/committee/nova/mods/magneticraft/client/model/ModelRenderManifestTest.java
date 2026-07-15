package committee.nova.mods.magneticraft.client.model;

import org.junit.jupiter.api.Test;

import java.io.StringReader;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ModelRenderManifestTest {
    @Test
    void nodeMappingOverridesMaterialMapping() throws Exception {
        ModelRenderManifest manifest = ModelRenderManifest.parse("test", new StringReader("""
                {
                  "schema_version": 1,
                  "tint_mappings": [
                    {"tint_index": 0, "materials": ["tier_band"]},
                    {"tint_index": 1, "nodes": ["fault_indicator"]}
                  ]
                }
                """));

        assertEquals(0, manifest.tintIndex("housing", "tier_band"));
        assertEquals(1, manifest.tintIndex("fault_indicator", "tier_band"));
        assertEquals(-1, manifest.tintIndex("housing", "housing"));
    }

    @Test
    void rejectsConflictsAndOutOfRangeIndices() {
        assertThrows(ModelParseException.class, () -> ModelRenderManifest.parse("conflict", new StringReader("""
                {
                  "schema_version": 1,
                  "tint_mappings": [
                    {"tint_index": 0, "nodes": ["band"]},
                    {"tint_index": 1, "nodes": ["band"]}
                  ]
                }
                """)));
        assertThrows(ModelParseException.class, () -> ModelRenderManifest.parse("range", new StringReader("""
                {
                  "schema_version": 1,
                  "tint_mappings": [{"tint_index": 16, "nodes": ["band"]}]
                }
                """)));
    }

    @Test
    void sidecarLocationDoesNotModifyStandardGltfPath() {
        assertEquals(
                "magneticraft:models/block/gltf/device.manifest.json",
                ModelRenderManifestRegistry.manifestLocation(
                        net.minecraft.resources.ResourceLocation.fromNamespaceAndPath(
                                "magneticraft",
                                "models/block/gltf/device.gltf"
                        )
                ).toString()
        );
    }
}
