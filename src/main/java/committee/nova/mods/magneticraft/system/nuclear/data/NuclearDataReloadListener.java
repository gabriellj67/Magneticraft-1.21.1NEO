package committee.nova.mods.magneticraft.system.nuclear.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Loads all nuclear fuel definitions in one preparation/apply transaction. */
public final class NuclearDataReloadListener implements PreparableReloadListener {
    public static final NuclearDataReloadListener INSTANCE = new NuclearDataReloadListener();
    static final String FUEL_DIRECTORY = "magneticraft/nuclear_fuels";

    private NuclearDataReloadListener() {
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

    private NuclearDataLoadResult prepare(ResourceManager resourceManager) {
        LinkedHashMap<ResourceLocation, JsonElement> values = new LinkedHashMap<>();
        ArrayList<NuclearDataValidationError> readErrors = new ArrayList<>();
        Map<ResourceLocation, Resource> resources = resourceManager.listResources(
                FUEL_DIRECTORY,
                id -> id.getPath().endsWith(".json")
        );
        resources.entrySet().stream()
                .sorted(Map.Entry.comparingByKey((first, second) -> first.toString().compareTo(second.toString())))
                .forEach(entry -> {
                    ResourceLocation logicalId;
                    try {
                        logicalId = logicalId(entry.getKey());
                    } catch (IllegalArgumentException exception) {
                        readErrors.add(new NuclearDataValidationError(entry.getKey(), exception.getMessage()));
                        return;
                    }
                    try (Reader reader = entry.getValue().openAsReader()) {
                        values.put(logicalId, JsonParser.parseReader(reader));
                    } catch (IOException | RuntimeException exception) {
                        readErrors.add(new NuclearDataValidationError(
                                logicalId,
                                "cannot read JSON: " + exception.getMessage()
                        ));
                    }
                });
        NuclearDataLoadResult parsed = NuclearDataParser.parse(values);
        if (readErrors.isEmpty()) {
            return parsed;
        }
        ArrayList<NuclearDataValidationError> errors = new ArrayList<>(readErrors);
        errors.addAll(parsed.errors());
        var rejected = new java.util.LinkedHashSet<>(parsed.rejectedFuelIds());
        readErrors.forEach(error -> rejected.add(error.resourceId()));
        return new NuclearDataLoadResult(parsed.fuelDefinitions(), errors, rejected);
    }

    private void apply(NuclearDataLoadResult result) {
        result.errors().forEach(error -> Magneticraft.LOGGER.warn(
                "Rejected nuclear fuel definition {}: {}",
                error.resourceId(),
                error.message()
        ));
        NuclearDataRegistry.ApplyResult applied = NuclearDataRegistry.INSTANCE.apply(result);
        Magneticraft.LOGGER.info(
                "Applied nuclear data snapshot {} ({} fuels, {} rejected IDs)",
                applied.current().generation(),
                applied.current().fuelDefinitions().size(),
                result.rejectedFuelIds().size()
        );
    }

    static ResourceLocation logicalId(ResourceLocation fileId) {
        String prefix = FUEL_DIRECTORY + "/";
        String path = fileId.getPath();
        if (!path.startsWith(prefix) || !path.endsWith(".json")) {
            throw new IllegalArgumentException("resource is outside " + FUEL_DIRECTORY + " or is not JSON");
        }
        String logicalPath = path.substring(prefix.length(), path.length() - ".json".length());
        if (logicalPath.isBlank()) {
            throw new IllegalArgumentException("resource has an empty logical id");
        }
        return ResourceLocation.fromNamespaceAndPath(fileId.getNamespace(), logicalPath);
    }
}
