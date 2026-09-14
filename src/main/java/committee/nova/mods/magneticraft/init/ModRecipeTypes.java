package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.recipe.TieredShapedRecipe;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearProcessRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.PolymerizerRecipe;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom machine recipe registrations.
 */
public final class ModRecipeTypes {
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<TieredShapedRecipe>> TIERED_SHAPED_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("tiered_shaped", TieredShapedRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<NuclearProcessRecipe>> NUCLEAR_PROCESSING_TYPE =
            type("nuclear_processing");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<NuclearProcessRecipe>> NUCLEAR_PROCESSING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("nuclear_processing", NuclearProcessRecipe.Serializer::new);
    private static final Set<MultiblockDefinition> ADVANCED_PROCESSING_MACHINES = EnumSet.of(
            MultiblockDefinition.GRINDER,
            MultiblockDefinition.SIEVE,
            MultiblockDefinition.HYDRAULIC_PRESS,
            MultiblockDefinition.OIL_HEATER,
            MultiblockDefinition.REFINERY
    );
    private static final Map<MultiblockDefinition, DeferredHolder<RecipeType<?>, RecipeType<AdvancedProcessingRecipe>>>
            ADVANCED_PROCESSING_TYPES = new EnumMap<>(MultiblockDefinition.class);
    private static final Map<MultiblockDefinition, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedProcessingRecipe>>>
            ADVANCED_PROCESSING_SERIALIZERS = new EnumMap<>(MultiblockDefinition.class);

    public static final DeferredHolder<RecipeType<?>, RecipeType<CrushingRecipe>> CRUSHING_TYPE =
            ModRegistries.RECIPE_TYPES.register("crushing_table", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "magneticraft:crushing_table";
                }
            });
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("crushing_table", CrushingRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<SluiceRecipe>> SLUICE_TYPE = type("sluice_box");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<SluiceRecipe>> SLUICE_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("sluice_box", SluiceRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<GasificationRecipe>> GASIFICATION_TYPE = type("gasification_unit");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<GasificationRecipe>> GASIFICATION_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("gasification_unit", GasificationRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<ThermopileRecipe>> THERMOPILE_TYPE = type("thermopile");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<ThermopileRecipe>> THERMOPILE_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("thermopile", ThermopileRecipe.Serializer::new);
    public static final DeferredHolder<RecipeType<?>, RecipeType<FluidFuelRecipe>> FLUID_FUEL_TYPE =
            type(MultiblockDefinition.BIG_COMBUSTION_CHAMBER.id());
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<FluidFuelRecipe>> FLUID_FUEL_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register(
                    MultiblockDefinition.BIG_COMBUSTION_CHAMBER.id(),
                    FluidFuelRecipe.Serializer::new
            );
    public static final DeferredHolder<RecipeType<?>, RecipeType<PolymerizerRecipe>> POLYMERIZING_TYPE = type("polymerizing");
    public static final DeferredHolder<RecipeSerializer<?>, RecipeSerializer<PolymerizerRecipe>> POLYMERIZING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("polymerizing", PolymerizerRecipe.Serializer::new);
    static {
        for (MultiblockDefinition definition : ADVANCED_PROCESSING_MACHINES) {
            ADVANCED_PROCESSING_TYPES.put(definition, type(definition.id()));
            ADVANCED_PROCESSING_SERIALIZERS.put(definition, ModRegistries.RECIPE_SERIALIZERS.register(
                    definition.id(),
                    () -> new AdvancedProcessingRecipe.Serializer(definition)
            ));
        }
    }

    private ModRecipeTypes() {
    }

    public static void bootstrap() {
    }

    public static DeferredHolder<RecipeType<?>, RecipeType<AdvancedProcessingRecipe>> advancedProcessingType(
            MultiblockDefinition definition
    ) {
        DeferredHolder<RecipeType<?>, RecipeType<AdvancedProcessingRecipe>> type = ADVANCED_PROCESSING_TYPES.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No processing recipe type registered for " + definition);
        }
        return type;
    }

    public static Map<MultiblockDefinition, DeferredHolder<RecipeType<?>, RecipeType<AdvancedProcessingRecipe>>>
    advancedProcessingTypes() {
        return Collections.unmodifiableMap(ADVANCED_PROCESSING_TYPES);
    }

    public static DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedProcessingRecipe>> advancedProcessingSerializer(
            MultiblockDefinition definition
    ) {
        DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedProcessingRecipe>> serializer =
                ADVANCED_PROCESSING_SERIALIZERS.get(definition);
        if (serializer == null) {
            throw new IllegalArgumentException("No processing recipe serializer registered for " + definition);
        }
        return serializer;
    }

    public static Map<MultiblockDefinition, DeferredHolder<RecipeSerializer<?>, RecipeSerializer<AdvancedProcessingRecipe>>>
    advancedProcessingSerializers() {
        return Collections.unmodifiableMap(ADVANCED_PROCESSING_SERIALIZERS);
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> DeferredHolder<RecipeType<?>, RecipeType<T>> type(String id) {
        return ModRegistries.RECIPE_TYPES.register(id, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return "magneticraft:" + id;
            }
        });
    }
}
