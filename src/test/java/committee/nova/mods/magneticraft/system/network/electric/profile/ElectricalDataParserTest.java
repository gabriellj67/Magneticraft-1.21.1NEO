package committee.nova.mods.magneticraft.system.network.electric.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ElectricalDataParserTest {
    private static final ResourceLocation LOW = id("low_voltage");
    private static final ResourceLocation MEDIUM = id("medium_voltage");

    @Test
    void builtInSnapshotAndDataPackFourthTierParseAsOneCompleteRegistry() throws IOException {
        Map<ResourceLocation, JsonElement> tiers = resources("voltage_tiers");
        Map<ResourceLocation, JsonElement> transformers = resources("transformer_profiles");
        Map<ResourceLocation, JsonElement> machines = resources("machine_electrical_profiles");

        ResourceLocation ultra = ResourceLocation.fromNamespaceAndPath("test_pack", "ultra_voltage");
        tiers.put(ultra, tierJson(3_840.0, 7_680.0, 8_000.0, "#A05CFF"));
        transformers.put(
                ResourceLocation.fromNamespaceAndPath("test_pack", "hv_to_uv"),
                transformerJson(id("high_voltage"), ultra, 12_800.0, 0.95)
        );
        machines.put(
                ResourceLocation.fromNamespaceAndPath("test_pack", "test_load"),
                machineJson(ultra, "consumer", 256_000.0, 2_000.0)
        );

        ElectricalDataLoadResult result = ElectricalDataParser.parse(tiers, transformers, machines);

        assertTrue(result.valid(), () -> result.errors().toString());
        ElectricalDataSnapshot snapshot = result.snapshot().orElseThrow();
        assertEquals(4, snapshot.voltageTiers().size());
        assertEquals(3, snapshot.transformerProfiles().size());
        assertEquals(21, snapshot.machineProfiles().size());
        assertEquals(120.0, snapshot.voltageTier(LOW).orElseThrow().nominalVoltage());
        assertEquals(0xD98245, snapshot.voltageTier(LOW).orElseThrow().colorRgb());
        assertEquals(16_000_000L, snapshot.voltageTier(id("high_voltage")).orElseThrow().batteryCapacityJoules());
    }

    @Test
    void invalidFilesAggregateFieldAndCrossReferenceErrorsWithoutPartialSnapshot() {
        JsonObject brokenTier = tierJson(60.0, 120.0, 125.0, "#D98245").getAsJsonObject();
        brokenTier.remove("translation_key");
        brokenTier.addProperty("minimum_operating_voltage", Double.NaN);
        brokenTier.addProperty("node_resistance_ohms", -1.0);

        ResourceLocation transformerId = id("broken_transformer");
        JsonObject brokenTransformer = transformerJson(LOW, MEDIUM, 100.0, 0.0).getAsJsonObject();
        ResourceLocation machineId = id("broken_machine");
        JsonObject brokenMachine = machineJson(MEDIUM, "not_a_role", -1.0, 0.0).getAsJsonObject();

        ElectricalDataLoadResult result = ElectricalDataParser.parse(
                Map.of(LOW, brokenTier),
                Map.of(transformerId, brokenTransformer),
                Map.of(machineId, brokenMachine)
        );

        assertFalse(result.valid());
        assertTrue(result.snapshot().isEmpty());
        assertTrue(result.errors().size() >= 7, result.errors().toString());
        Set<ResourceLocation> resources = result.errors().stream()
                .map(ElectricalDataValidationError::resourceId)
                .collect(Collectors.toSet());
        assertTrue(resources.contains(LOW));
        assertTrue(resources.contains(transformerId));
        assertTrue(resources.contains(machineId));
    }

    @Test
    void emptyAndOversizedRegistriesAreRejected() {
        ElectricalDataLoadResult empty = ElectricalDataParser.parse(Map.of(), Map.of(), Map.of());
        assertFalse(empty.valid());
        assertEquals(3, empty.errors().size());

        LinkedHashMap<ResourceLocation, JsonElement> tiers = new LinkedHashMap<>();
        for (int index = 0; index <= VoltageTier.MAX_SYNCED_TIERS; index++) {
            tiers.put(ResourceLocation.fromNamespaceAndPath("test", "tier_" + index), tierJson(1.0, 2.0, 3.0, "#010203"));
        }
        ElectricalDataLoadResult oversized = ElectricalDataParser.parse(
                tiers,
                Map.of(id("transformer"), transformerJson(LOW, MEDIUM, 1.0, 1.0)),
                Map.of(id("machine"), machineJson(LOW, "passive", 1.0, 1.0))
        );
        assertFalse(oversized.valid());
        assertTrue(oversized.errors().stream().anyMatch(error -> error.message().contains("maximum")));
    }

    @Test
    void failedAtomicApplyPreservesThePreviousSnapshot() {
        ElectricalDataRegistry registry = new ElectricalDataRegistry();
        ElectricalDataLoadResult valid = ElectricalDataParser.parse(
                Map.of(LOW, tierJson(60.0, 120.0, 125.0, "#D98245"), MEDIUM, tierJson(240.0, 480.0, 500.0, "#E5C84B")),
                Map.of(id("lv_to_mv"), transformerJson(LOW, MEDIUM, 800.0, 0.96)),
                Map.of(id("load"), machineJson(LOW, "consumer", 1_000.0, 40.0))
        );
        ElectricalDataSnapshot first = registry.apply(valid).current();

        ElectricalDataLoadResult invalid = ElectricalDataParser.parse(
                Map.of(LOW, tierJson(60.0, 120.0, 125.0, "#D98245")),
                Map.of(id("lv_to_mv"), transformerJson(LOW, MEDIUM, 800.0, 0.96)),
                Map.of(id("load"), machineJson(LOW, "consumer", 1_000.0, 40.0))
        );

        assertThrows(ElectricalDataReloadException.class, () -> registry.apply(invalid));
        assertSame(first, registry.currentOrThrow());
        assertEquals(1L, first.generation());
    }

    @Test
    void schemaOrderingEfficiencyAndFiniteNumberRulesAreStrict() {
        JsonObject tier = tierJson(60.0, 120.0, 125.0, "#D98245").getAsJsonObject();
        tier.addProperty("schema_version", 2);
        tier.addProperty("nominal_voltage", 60.0);
        tier.addProperty("battery_transfer_joules_per_tick", Double.POSITIVE_INFINITY);
        JsonObject transformer = transformerJson(LOW, LOW, 1.0, 1.1).getAsJsonObject();
        JsonObject machine = machineJson(LOW, "consumer", 1.0, 1.0).getAsJsonObject();
        machine.addProperty("terminal_rated_charge_per_tick", -1.0);

        ElectricalDataLoadResult result = ElectricalDataParser.parse(
                Map.of(LOW, tier),
                Map.of(id("same_tier"), transformer),
                Map.of(id("load"), machine)
        );

        assertFalse(result.valid());
        String messages = result.errors().toString();
        assertTrue(messages.contains("schema_version"));
        assertTrue(messages.contains("minimum_operating_voltage"));
        assertTrue(messages.contains("battery_transfer_joules_per_tick"));
        assertTrue(messages.contains("must differ"));
        assertTrue(messages.contains("efficiency"));
        assertTrue(messages.contains("terminal_rated_charge_per_tick"));
    }

    private static Map<ResourceLocation, JsonElement> resources(String registry) throws IOException {
        Path directory = Path.of("src/main/resources/data/magneticraft/magneticraft", registry);
        LinkedHashMap<ResourceLocation, JsonElement> values = new LinkedHashMap<>();
        try (var files = Files.walk(directory)) {
            for (Path file : files.filter(Files::isRegularFile).filter(path -> path.toString().endsWith(".json")).sorted().toList()) {
                String relative = directory.relativize(file).toString().replace('\\', '/');
                String path = relative.substring(0, relative.length() - ".json".length());
                values.put(id(path), JsonParser.parseString(Files.readString(file)));
            }
        }
        return values;
    }

    private static JsonElement tierJson(double minimum, double nominal, double maximum, String color) {
        JsonObject object = new JsonObject();
        object.addProperty("schema_version", 1);
        object.addProperty("translation_key", "voltage_tier.test.value");
        object.addProperty("minimum_operating_voltage", minimum);
        object.addProperty("nominal_voltage", nominal);
        object.addProperty("maximum_voltage", maximum);
        object.addProperty("generator_voltage", maximum);
        object.addProperty("machine_capacitance_farads", 1.0);
        object.addProperty("conductor_capacitance_farads", 0.25);
        object.addProperty("node_resistance_ohms", 0.005);
        object.addProperty("cable_rated_charge_per_tick", 8.0);
        object.addProperty("overhead_rated_charge_per_tick", 16.0);
        object.addProperty("standard_protection_charge_per_tick", 8.0);
        object.addProperty("heavy_protection_charge_per_tick", 16.0);
        object.addProperty("cable_thermal_capacity", 400.0);
        object.addProperty("cable_cooling_per_tick", 0.0025);
        object.addProperty("overhead_thermal_capacity", 800.0);
        object.addProperty("overhead_cooling_per_tick", 0.00125);
        object.addProperty("connector_range", 8);
        object.addProperty("pole_range", 16);
        object.addProperty("battery_capacity_joules", 1_000_000L);
        object.addProperty("battery_transfer_joules_per_tick", 640.0);
        object.addProperty("color", color);
        return object;
    }

    private static JsonElement transformerJson(
            ResourceLocation input,
            ResourceLocation output,
            double transfer,
            double efficiency
    ) {
        JsonObject object = new JsonObject();
        object.addProperty("schema_version", 1);
        object.addProperty("input_tier", input.toString());
        object.addProperty("output_tier", output.toString());
        object.addProperty("maximum_transfer_joules_per_tick", transfer);
        object.addProperty("efficiency", efficiency);
        return object;
    }

    private static JsonElement machineJson(
            ResourceLocation tier,
            String role,
            double buffer,
            double transfer
    ) {
        JsonObject object = new JsonObject();
        object.addProperty("schema_version", 1);
        object.addProperty("tier", tier.toString());
        object.addProperty("role", role);
        object.addProperty("buffer_capacity_joules", buffer);
        object.addProperty("maximum_transfer_joules_per_tick", transfer);
        object.addProperty("terminal_rated_charge_per_tick", 8.0);
        return object;
    }

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath("magneticraft", path);
    }
}
