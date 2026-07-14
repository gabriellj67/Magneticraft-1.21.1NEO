package committee.nova.mods.magneticraft.client.model;

import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.EnumMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertTrue;

class LegacyMultiblockModelBoundsContractTest {
    private static final Path MODEL_ROOT = Path.of(
            "src/main/resources/assets/magneticraft/models/block"
    );

    @Test
    void everyFormedMachineSceneFitsItsDeclaredStructureVolume() throws Exception {
        for (Map.Entry<MultiblockDefinition, Source> entry : sources().entrySet()) {
            ModelScene scene = parse(entry.getValue());
            ModelSceneBounds bounds = ModelSceneBounds.calculate(
                    scene, ModelSceneSelection.ALL.select(scene)
            );
            float x = bounds.maxX() - bounds.minX();
            float y = bounds.maxY() - bounds.minY();
            float z = bounds.maxZ() - bounds.minZ();
            assertTrue(Float.isFinite(x) && Float.isFinite(y) && Float.isFinite(z));
            assertTrue(x > 0.0F && y > 0.0F && z > 0.0F, entry.getKey()::name);
            float modelHorizontalMin = Math.min(x, z);
            float modelHorizontalMax = Math.max(x, z);
            float structureHorizontalMin = Math.min(
                    entry.getKey().size().x(), entry.getKey().size().z()
            );
            float structureHorizontalMax = Math.max(
                    entry.getKey().size().x(), entry.getKey().size().z()
            );
            assertCloseToStructure(entry.getKey(), modelHorizontalMin, structureHorizontalMin, 1.0F);
            assertCloseToStructure(entry.getKey(), modelHorizontalMax, structureHorizontalMax, 1.0F);
            assertCloseToStructure(
                    entry.getKey(), y, entry.getKey().size().y(), 1.25F,
                    entry.getKey() == MultiblockDefinition.SHELVING_UNIT ? 0.75F : 0.125F
            );
        }
    }

    private static void assertCloseToStructure(
            MultiblockDefinition definition,
            float modelSpan,
            float structureSpan,
            float allowedInset
    ) {
        assertCloseToStructure(definition, modelSpan, structureSpan, allowedInset, 0.125F);
    }

    private static void assertCloseToStructure(
            MultiblockDefinition definition,
            float modelSpan,
            float structureSpan,
            float allowedInset,
            float allowedOverhang
    ) {
        assertTrue(modelSpan <= structureSpan + allowedOverhang, () ->
                definition + " model span " + modelSpan + " exceeds structure span " + structureSpan);
        assertTrue(modelSpan >= structureSpan - allowedInset, () ->
                definition + " model span " + modelSpan + " is too small for structure span " + structureSpan);
    }

    private static ModelScene parse(Source source) throws Exception {
        Path path = MODEL_ROOT.resolve(source.format() + "/" + source.name() + "." + source.format());
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            if ("mcx".equals(source.format())) {
                return McxModelParser.parse(path.toString(), reader);
            }
            return GltfModelParser.parse(
                    path.toString(), reader, uri -> Files.newInputStream(path.getParent().resolve(uri))
            );
        }
    }

    private static Map<MultiblockDefinition, Source> sources() {
        EnumMap<MultiblockDefinition, Source> result = new EnumMap<>(MultiblockDefinition.class);
        result.put(MultiblockDefinition.BIG_COMBUSTION_CHAMBER, gltf("big_combustion_chamber"));
        result.put(MultiblockDefinition.BIG_ELECTRIC_FURNACE, gltf("big_electric_furnace"));
        result.put(MultiblockDefinition.BIG_STEAM_BOILER, gltf("big_steam_boiler"));
        result.put(MultiblockDefinition.CONTAINER, mcx("container"));
        result.put(MultiblockDefinition.GRINDER, gltf("grinder"));
        result.put(MultiblockDefinition.HYDRAULIC_PRESS, gltf("hydraulic_press"));
        result.put(MultiblockDefinition.OIL_HEATER, mcx("oil_heater"));
        result.put(MultiblockDefinition.PUMPJACK, mcx("pumpjack"));
        result.put(MultiblockDefinition.REFINERY, mcx("refinery"));
        result.put(MultiblockDefinition.SHELVING_UNIT, mcx("shelving_unit"));
        result.put(MultiblockDefinition.SIEVE, gltf("sieve"));
        result.put(MultiblockDefinition.SOLAR_MIRROR, mcx("solar_mirror"));
        result.put(MultiblockDefinition.SOLAR_PANEL, mcx("solar_panel"));
        result.put(MultiblockDefinition.SOLAR_TOWER, mcx("solar_tower"));
        result.put(MultiblockDefinition.STEAM_ENGINE, gltf("steam_engine"));
        result.put(MultiblockDefinition.STEAM_TURBINE, gltf("steam_turbine"));
        return Map.copyOf(result);
    }

    private static Source mcx(String name) {
        return new Source("mcx", name);
    }

    private static Source gltf(String name) {
        return new Source("gltf", name);
    }

    private record Source(String format, String name) {
    }
}
