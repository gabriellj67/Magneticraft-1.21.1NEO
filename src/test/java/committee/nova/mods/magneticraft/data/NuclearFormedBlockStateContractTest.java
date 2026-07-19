package committee.nova.mods.magneticraft.data;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NuclearFormedBlockStateContractTest {
    private static final Path ASSETS = Path.of("src/generated/resources/assets/magneticraft");

    @Test
    void formedControllersMembersAndPortsResolveToEmptyModels() throws Exception {
        assertFormedVariants("pressurized_water_reactor_controller");
        assertFormedVariants("reactor_containment_casing");
        assertFormedVariants("reactor_pressure_vessel");
        assertFormedVariants("reactor_main_coolant_port");
        assertFormedVariants("reactor_fuel_standard");
        assertFormedVariants("spent_fuel_pool_controller");
        assertFormedVariants("spent_fuel_pool_port");
        assertFormedVariants("nuclear_facility_casing");
    }

    @Test
    void formedControllerModelsContainOnlyAParticleTexture() throws Exception {
        assertEmptyModel("pressurized_water_reactor_controller_formed");
        assertEmptyModel("spent_fuel_pool_controller_formed");
    }

    private static void assertFormedVariants(String id) throws Exception {
        JsonObject variants = read(ASSETS.resolve("blockstates/" + id + ".json"))
                .getAsJsonObject("variants");
        boolean formed = false;
        boolean unformed = false;
        for (Map.Entry<String, com.google.gson.JsonElement> entry : variants.entrySet()) {
            String model = entry.getValue().getAsJsonObject().get("model").getAsString();
            if (entry.getKey().contains("formed=true")) {
                formed = true;
                assertTrue(model.endsWith("_formed"), id + " formed variant did not use an empty model");
            } else if (entry.getKey().contains("formed=false")) {
                unformed = true;
                assertFalse(model.endsWith("_formed"), id + " unformed variant lost its placement model");
            }
        }
        assertTrue(formed && unformed, id + " did not generate both visual states");
    }

    private static void assertEmptyModel(String id) throws Exception {
        JsonObject model = read(ASSETS.resolve("models/block/" + id + ".json"));
        assertTrue(model.has("textures") && model.getAsJsonObject("textures").has("particle"));
        assertFalse(model.has("parent"));
        assertFalse(model.has("elements"));
        assertFalse(model.has("loader"));
    }

    private static JsonObject read(Path path) throws Exception {
        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            return JsonParser.parseReader(reader).getAsJsonObject();
        }
    }
}
