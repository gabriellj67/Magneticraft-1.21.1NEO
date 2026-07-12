import mods.magneticraft.Recipes;

// Smoke-test the public facade: add one recipe and remove one generated recipe.
Recipes.add("crushing", "crafttweaker_integration_test", {
    "ingredient": {
        "item": "minecraft:cobblestone"
    },
    "result": {
        "item": "minecraft:gravel"
    },
    "required_level": 0
});
Recipes.remove("crushing", "magneticraft:crushing/stone");

// With enable_crafttweaker_runtime=true, runServer copies this file to run/scripts.
// Verify both actions with /ct recipes and the CraftTweaker server log.
