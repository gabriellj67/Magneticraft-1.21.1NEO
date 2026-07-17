package committee.nova.mods.magneticraft.client;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeneratedTextureAssetContractTest {
    private static final Path GENERATED_MODELS = Path.of(
            "src/generated/resources/assets/magneticraft/models"
    );
    private static final Path TEXTURES = Path.of(
            "src/main/resources/assets/magneticraft/textures"
    );

    @Test
    void texturesUseCanonicalPurposeDirectories() throws IOException {
        try (var files = Files.walk(TEXTURES)) {
            var relativeFiles = files
                    .filter(Files::isRegularFile)
                    .map(TEXTURES::relativize)
                    .toList();
            Set<String> roots = relativeFiles.stream()
                    .map(path -> path.getName(0).toString())
                    .collect(java.util.stream.Collectors.toUnmodifiableSet());

            assertEquals(Set.of("block", "fluid", "item"), roots);
            assertTrue(
                    relativeFiles.stream().noneMatch(path ->
                            path.toString().replace('\\', '/').contains("/unsorted/")),
                    "Texture assets must be assigned to a semantic directory"
            );
        }
    }

    @Test
    void multiblockPartsUseTheRetainedNovaTexturesInsteadOfVanillaPlaceholders() throws IOException {
        assertBlockTextures("machine_casing", Set.of(
                "magneticraft:block/multiblock_parts/base_bottom",
                "magneticraft:block/multiblock_parts/base_side",
                "magneticraft:block/multiblock_parts/base_top"
        ));
        assertBlockTextures("corrugated_iron", Set.of(
                "magneticraft:block/multiblock_parts/corrugated_iron",
                "magneticraft:block/multiblock_parts/corrugated_iron_side"
        ));
        assertBlockTextures("copper_coil", Set.of(
                "magneticraft:block/multiblock_parts/copper_coil",
                "magneticraft:block/multiblock_parts/copper_coil_side"
        ));
        assertBlockTextures("machine_support_column", Set.of(
                "magneticraft:block/multiblock_parts/column_end",
                "magneticraft:block/multiblock_parts/column_side"
        ));
        assertBlockTextures("striped_machine_casing", Set.of(
                "magneticraft:block/multiblock_parts/striped"
        ));
        assertBlockTextures("electrical_machine_casing", Set.of(
                "magneticraft:block/multiblock_parts/electric"
        ));
        assertBlockTextures("pumpjack_drill", Set.of(
                "magneticraft:block/multiblock_parts/pumpjack_drill",
                "magneticraft:block/multiblock_parts/pumpjack_drill_side"
        ));
        assertBlockTextures("multiblock_gap", Set.of(
                "magneticraft:block/multiblocks/multiblock_gap"
        ));
    }

    @Test
    void utilityAndWorldBlocksUseTheirRetainedNovaTexturesInsteadOfVanillaPlaceholders() throws IOException {
        assertBlockTextures("air_bubble", Set.of("magneticraft:block/machines/air_bubble"));
        assertBlockTextures("oil_deposit", Set.of("magneticraft:block/ore_block/oil_source_1"));
    }

    @Test
    void redesignedElectricalModelsAndItemsUseGeneratedDedicatedTextures() throws IOException {
        assertBlockTextures("box_transformer", Set.of("magneticraft:block/electrical_enclosure"));
        assertBlockTextures("fuse_box", Set.of("magneticraft:block/electrical_enclosure"));
        assertBlockTextures("circuit_breaker", Set.of("magneticraft:block/electrical_breaker_housing"));
        assertBlockTextures("burnt_electric_cable", Set.of("magneticraft:block/burnt_electric_cable"));
        assertItemTextures("electrical_fuse", Set.of("magneticraft:item/electrical_fuse"));
        assertItemTextures(
                "electrical_fuse_heavy_low_voltage",
                Set.of("magneticraft:item/electrical_fuse_heavy_low_voltage")
        );
        assertItemTextures(
                "electrical_fuse_heavy_medium_voltage",
                Set.of("magneticraft:item/electrical_fuse_heavy_medium_voltage")
        );
        assertItemTextures(
                "electrical_fuse_heavy_high_voltage",
                Set.of("magneticraft:item/electrical_fuse_heavy_high_voltage")
        );
        assertItemTextures("electrical_repair_tool", Set.of("magneticraft:item/electrical_repair_tool"));
    }

    @Test
    void generatedPixelArtHasTheRequiredResolutionAndItemTransparency() throws IOException {
        assertTexture("block/multiblocks/unmounted_multiblock", false);
        assertTexture("block/electrical_enclosure", false);
        assertTexture("block/electrical_breaker_housing", false);
        assertTexture("block/burnt_electric_cable", false);
        assertTexture("block/electrical_indicator", false);
        assertTexture("item/electrical_fuse", true);
        assertTexture("item/electrical_fuse_heavy_low_voltage", true);
        assertTexture("item/electrical_fuse_heavy_medium_voltage", true);
        assertTexture("item/electrical_fuse_heavy_high_voltage", true);
        assertTexture("item/electrical_repair_tool", true);
    }

    private static void assertBlockTextures(String name, Set<String> expected) throws IOException {
        assertModelTextures("block", name, expected);
    }

    private static void assertItemTextures(String name, Set<String> expected) throws IOException {
        assertModelTextures("item", name, expected);
    }

    private static void assertModelTextures(String type, String name, Set<String> expected) throws IOException {
        Path model = GENERATED_MODELS.resolve(type).resolve(name + ".json");
        assertTrue(Files.isRegularFile(model), model.toString());
        JsonObject json = JsonParser.parseString(Files.readString(model)).getAsJsonObject();
        JsonObject textures = json.getAsJsonObject("textures");
        assertNotNull(textures, model.toString());
        Set<String> actual = textures.entrySet().stream()
                .map(Map.Entry::getValue)
                .map(JsonElement::getAsString)
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        assertEquals(expected, actual, model.toString());
    }

    private static void assertTexture(String name, boolean requiresTransparency) throws IOException {
        Path path = TEXTURES.resolve(name + ".png");
        assertTrue(Files.isRegularFile(path), path.toString());
        BufferedImage image = ImageIO.read(path.toFile());
        assertNotNull(image, path.toString());
        assertEquals(16, image.getWidth(), path.toString());
        assertEquals(16, image.getHeight(), path.toString());
        if (requiresTransparency) {
            boolean hasTransparentPixel = false;
            boolean hasOpaquePixel = false;
            for (int y = 0; y < image.getHeight(); y++) {
                for (int x = 0; x < image.getWidth(); x++) {
                    int alpha = image.getRGB(x, y) >>> 24;
                    hasTransparentPixel |= alpha == 0;
                    hasOpaquePixel |= alpha == 0xFF;
                }
            }
            assertTrue(hasTransparentPixel, path + " must keep a transparent item background");
            assertTrue(hasOpaquePixel, path + " must keep visible opaque pixels");
        }
    }
}
