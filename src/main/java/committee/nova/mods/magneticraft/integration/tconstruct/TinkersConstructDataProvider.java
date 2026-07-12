package committee.nova.mods.magneticraft.integration.tconstruct;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;

/** Generates API-free Tinkers' Construct material data guarded by Forge load conditions. */
public final class TinkersConstructDataProvider implements DataProvider {
    private static final String MATERIAL_ID = Magneticraft.MOD_ID + ":tungsten";

    private final PackOutput.PathProvider definitions;
    private final PackOutput.PathProvider stats;
    private final PackOutput.PathProvider traits;
    private final PackOutput.PathProvider renderInfo;
    private final PackOutput.PathProvider recipes;

    public TinkersConstructDataProvider(PackOutput output) {
        definitions = output.createPathProvider(PackOutput.Target.DATA_PACK, "tinkering/materials/definition");
        stats = output.createPathProvider(PackOutput.Target.DATA_PACK, "tinkering/materials/stats");
        traits = output.createPathProvider(PackOutput.Target.DATA_PACK, "tinkering/materials/traits");
        renderInfo = output.createPathProvider(PackOutput.Target.RESOURCE_PACK, "tinkering/materials");
        recipes = output.createPathProvider(PackOutput.Target.DATA_PACK, "recipes/integration/tconstruct");
    }

    @Override
    public CompletableFuture<?> run(CachedOutput output) {
        List<CompletableFuture<?>> writes = new ArrayList<>(5);
        writes.add(save(output, definition(), definitions.json(Magneticraft.id("tungsten"))));
        writes.add(save(output, stats(), stats.json(Magneticraft.id("tungsten"))));
        writes.add(save(output, traits(), traits.json(Magneticraft.id("tungsten"))));
        writes.add(save(output, renderInfo(), renderInfo.json(Magneticraft.id("tungsten"))));
        writes.add(save(output, ingotRecipe(), recipes.json(Magneticraft.id("tungsten_ingot_material"))));
        return CompletableFuture.allOf(writes.toArray(CompletableFuture[]::new));
    }

    @Override
    public String getName() {
        return "Magneticraft Tinkers' Construct integration";
    }

    static JsonObject definition() {
        JsonObject root = new JsonObject();
        root.add("condition", modLoadedCondition());
        root.addProperty("craftable", true);
        root.addProperty("hidden", false);
        root.addProperty("sortOrder", 7);
        root.addProperty("tier", 3);
        return root;
    }

    static JsonObject stats() {
        JsonObject root = new JsonObject();
        JsonObject stats = new JsonObject();

        stats.add("tconstruct:binding", new JsonObject());
        stats.add("tconstruct:head", headStats());
        stats.add("tconstruct:handle", handleStats());
        stats.add("tconstruct:limb", limbStats());
        stats.add("tconstruct:grip", gripStats());
        root.add("stats", stats);
        return root;
    }

    static JsonObject traits() {
        JsonObject root = new JsonObject();
        JsonArray defaults = new JsonArray();
        JsonObject heavy = new JsonObject();
        heavy.addProperty("level", 1);
        heavy.addProperty("name", "tconstruct:heavy");
        defaults.add(heavy);
        root.add("default", defaults);
        return root;
    }

    static JsonObject renderInfo() {
        JsonObject root = new JsonObject();
        root.addProperty("color", "FFB7B7C7");
        JsonArray fallbacks = new JsonArray();
        fallbacks.add("metal");
        root.add("fallbacks", fallbacks);

        JsonObject generator = new JsonObject();
        JsonArray supportedStats = new JsonArray();
        List.of(
                "tconstruct:ingot",
                "tconstruct:head",
                "tconstruct:handle",
                "tconstruct:binding",
                "tconstruct:repair_kit",
                "tconstruct:limb",
                "tconstruct:grip"
        ).forEach(supportedStats::add);
        generator.add("supported_stats", supportedStats);
        generator.add("transformer", spriteTransformer());
        root.add("generator", generator);
        return root;
    }

    static JsonObject ingotRecipe() {
        JsonObject root = new JsonObject();
        root.addProperty("type", "tconstruct:material");
        JsonArray conditions = new JsonArray();
        conditions.add(modLoadedCondition());
        root.add("conditions", conditions);

        JsonObject ingredient = new JsonObject();
        ingredient.addProperty("tag", "forge:ingots/tungsten");
        root.add("ingredient", ingredient);
        root.addProperty("material", MATERIAL_ID);
        root.addProperty("needed", 1);
        root.addProperty("value", 1);
        return root;
    }

    private static JsonObject modLoadedCondition() {
        JsonObject condition = new JsonObject();
        condition.addProperty("type", "forge:mod_loaded");
        condition.addProperty("modid", "tconstruct");
        return condition;
    }

    private static JsonObject headStats() {
        JsonObject head = new JsonObject();
        head.addProperty("durability", 950);
        head.addProperty("melee_attack", 3.0F);
        head.addProperty("mining_speed", 5.0F);
        head.addProperty("mining_tier", "minecraft:diamond");
        return head;
    }

    private static JsonObject handleStats() {
        JsonObject handle = new JsonObject();
        handle.addProperty("durability", 0.15F);
        handle.addProperty("melee_damage", 0.10F);
        handle.addProperty("melee_speed", -0.10F);
        handle.addProperty("mining_speed", -0.05F);
        return handle;
    }

    private static JsonObject limbStats() {
        JsonObject limb = new JsonObject();
        limb.addProperty("accuracy", -0.10F);
        limb.addProperty("draw_speed", -0.20F);
        limb.addProperty("durability", 950);
        limb.addProperty("velocity", 0.20F);
        return limb;
    }

    private static JsonObject gripStats() {
        JsonObject grip = new JsonObject();
        grip.addProperty("accuracy", -0.10F);
        grip.addProperty("durability", 0.15F);
        grip.addProperty("melee_damage", 3.0F);
        return grip;
    }

    private static JsonObject spriteTransformer() {
        JsonObject transformer = new JsonObject();
        transformer.addProperty("type", "tconstruct:recolor_sprite");

        JsonObject mapping = new JsonObject();
        mapping.addProperty("type", "tconstruct:grey_to_color");
        JsonArray palette = new JsonArray();
        palette.add(paletteEntry("FF000000", 0));
        palette.add(paletteEntry("FF34343C", 63));
        palette.add(paletteEntry("FF555561", 102));
        palette.add(paletteEntry("FF777786", 140));
        palette.add(paletteEntry("FF9999AA", 178));
        palette.add(paletteEntry("FFB7B7C7", 216));
        palette.add(paletteEntry("FFD7D7E2", 255));
        mapping.add("palette", palette);
        transformer.add("color_mapping", mapping);
        return transformer;
    }

    private static JsonObject paletteEntry(String color, int grey) {
        JsonObject entry = new JsonObject();
        entry.addProperty("color", color);
        entry.addProperty("grey", grey);
        return entry;
    }

    private static CompletableFuture<?> save(CachedOutput output, JsonObject json, Path path) {
        return DataProvider.saveStable(output, json, path);
    }
}
