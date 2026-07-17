package committee.nova.mods.magneticraft.system.nuclear.data;

import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.PreparableReloadListener;
import net.minecraft.server.packs.resources.Resource;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraft.util.profiling.ProfilerFiller;

import java.io.Reader;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

/** Reloads the single PWR parameter definition without replacing a valid live value on error. */
public final class ReactorParameterReloadListener implements PreparableReloadListener {
    public static final ReactorParameterReloadListener INSTANCE = new ReactorParameterReloadListener();
    static final String DIRECTORY = "magneticraft/nuclear/reactor_parameters";

    private ReactorParameterReloadListener() {
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

    private Prepared prepare(ResourceManager manager) {
        Map<ResourceLocation, Resource> resources = manager.listResources(
                DIRECTORY, id -> id.getPath().endsWith(".json"));
        ResourceLocation file = ResourceLocation.fromNamespaceAndPath(
                Magneticraft.MOD_ID, DIRECTORY + "/pressurized_water_reactor.json");
        Resource resource = resources.get(file);
        if (resource == null) {
            return new Prepared(null, "missing required definition " + file);
        }
        try (Reader reader = resource.openAsReader()) {
            JsonElement json = JsonParser.parseReader(reader);
            ReactorParameterParser.Result parsed = ReactorParameterParser.parse(ReactorParameters.PWR_ID, json);
            return parsed.parameters().map(value -> new Prepared(value, null))
                    .orElseGet(() -> new Prepared(null, String.join("; ", parsed.errors())));
        } catch (Exception exception) {
            return new Prepared(null, "cannot read JSON: " + exception.getMessage());
        }
    }

    private void apply(Prepared prepared) {
        if (prepared.parameters() == null) {
            Magneticraft.LOGGER.error(
                    "Rejected PWR reactor parameters; retaining generation {}: {}",
                    ReactorParameterRegistry.INSTANCE.current().generation(), prepared.error());
            return;
        }
        ReactorParameterRegistry.Snapshot snapshot =
                ReactorParameterRegistry.INSTANCE.apply(prepared.parameters());
        Magneticraft.LOGGER.info("Applied PWR reactor parameter snapshot {}", snapshot.generation());
    }

    private record Prepared(ReactorParameters parameters, String error) {
    }
}
