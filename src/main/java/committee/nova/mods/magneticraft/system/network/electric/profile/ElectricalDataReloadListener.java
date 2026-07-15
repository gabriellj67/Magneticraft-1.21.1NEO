package committee.nova.mods.magneticraft.system.network.electric.profile;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.config.MagneticraftConfig;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/**
 * Loads all three directories as one candidate and publishes them in one atomic apply step.
 */
public final class ElectricalDataReloadListener implements PreparableReloadListener {
    public static final ElectricalDataReloadListener INSTANCE = new ElectricalDataReloadListener();

    static final String VOLTAGE_DIRECTORY = "magneticraft/voltage_tiers";
    static final String TRANSFORMER_DIRECTORY = "magneticraft/transformer_profiles";
    static final String MACHINE_DIRECTORY = "magneticraft/machine_electrical_profiles";

    private ElectricalDataReloadListener() {
    }

    @Override
    public CompletableFuture<Void> reload(
            PreparationBarrier barrier,
            ResourceManager resourceManager,
            ProfilerFiller preparationProfiler,
            ProfilerFiller reloadProfiler,
            Executor backgroundExecutor,
            Executor gameExecutor
    ) {
        return CompletableFuture.supplyAsync(() -> prepare(resourceManager), backgroundExecutor)
                .thenCompose(barrier::wait)
                .thenAcceptAsync(this::apply, gameExecutor);
    }

    private PreparedData prepare(ResourceManager resourceManager) {
        ArrayList<ElectricalDataValidationError> errors = new ArrayList<>();
        Map<ResourceLocation, JsonElement> tiers = readDirectory(
                resourceManager,
                VOLTAGE_DIRECTORY,
                ElectricalDataValidationError.RegistryKind.VOLTAGE_TIER,
                errors
        );
        Map<ResourceLocation, JsonElement> transformers = readDirectory(
                resourceManager,
                TRANSFORMER_DIRECTORY,
                ElectricalDataValidationError.RegistryKind.TRANSFORMER_PROFILE,
                errors
        );
        Map<ResourceLocation, JsonElement> machines = readDirectory(
                resourceManager,
                MACHINE_DIRECTORY,
                ElectricalDataValidationError.RegistryKind.MACHINE_PROFILE,
                errors
        );
        return new PreparedData(ElectricalDataParser.parse(tiers, transformers, machines, errors));
    }

    private void apply(PreparedData prepared) {
        ElectricalDataLoadResult result = prepared.result();
        if (!result.valid()) {
            StringBuilder details = new StringBuilder();
            for (ElectricalDataValidationError error : result.errors()) {
                details.append(System.lineSeparator())
                        .append(" - [")
                        .append(error.registry())
                        .append("] ")
                        .append(error.resourceId())
                        .append(": ")
                        .append(error.message());
            }
            Magneticraft.LOGGER.error(
                    "Rejected electrical data-pack snapshot with {} error(s); keeping the previous snapshot:{}",
                    result.errors().size(),
                    details
            );
        }

        ElectricalDataRegistry.ApplyResult applied = ElectricalDataRegistry.INSTANCE.apply(result);
        PhysicalNetworkService.onElectricalProfilesReloaded(
                applied.current(),
                MagneticraftConfig.ELECTRICAL_RELOAD_GRACE_TICKS.get()
        );
        Magneticraft.LOGGER.info(
                "Applied electrical data snapshot {} ({} tiers, {} transformers, {} machines)",
                applied.current().generation(),
                applied.current().voltageTiers().size(),
                applied.current().transformerProfiles().size(),
                applied.current().machineProfiles().size()
        );
    }

    private static Map<ResourceLocation, JsonElement> readDirectory(
            ResourceManager resourceManager,
            String directory,
            ElectricalDataValidationError.RegistryKind kind,
            List<ElectricalDataValidationError> errors
    ) {
        LinkedHashMap<ResourceLocation, JsonElement> values = new LinkedHashMap<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                directory,
                id -> id.getPath().endsWith(".json")
        );
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((first, second) -> first.toString().compareTo(second.toString())))
                .forEach(entry -> {
                    ResourceLocation logicalId;
                    try {
                        logicalId = logicalId(directory, entry.getKey());
                    } catch (IllegalArgumentException exception) {
                        errors.add(new ElectricalDataValidationError(kind, entry.getKey(), exception.getMessage()));
                        return;
                    }
                    try (Reader reader = entry.getValue().openAsReader()) {
                        JsonElement previous = values.put(logicalId, JsonParser.parseReader(reader));
                        if (previous != null) {
                            errors.add(new ElectricalDataValidationError(
                                    kind,
                                    logicalId,
                                    "multiple resources resolve to the same logical id"
                            ));
                        }
                    } catch (IOException | RuntimeException exception) {
                        errors.add(new ElectricalDataValidationError(
                                kind,
                                logicalId,
                                "cannot read JSON: " + exception.getMessage()
                        ));
                    }
                });
        return values;
    }

    static ResourceLocation logicalId(String directory, ResourceLocation fileId) {
        String prefix = directory + "/";
        String path = fileId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("resource is outside " + directory + " or is not JSON");
        }
        String logicalPath = path.substring(prefix.length(), path.length() - ".json".length());
        if (logicalPath.isBlank()) {
            throw new IllegalArgumentException("resource has an empty logical id");
        }
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), logicalPath);
    }

    private record PreparedData(ElectricalDataLoadResult result) {
    }
}
