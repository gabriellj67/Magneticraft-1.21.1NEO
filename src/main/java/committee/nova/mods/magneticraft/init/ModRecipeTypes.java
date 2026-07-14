package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.registries.RegistryObject;

import java.util.Collections;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Custom machine recipe registrations.
 */
public final class ModRecipeTypes {
    private static final Set<MultiblockDefinition> ADVANCED_PROCESSING_MACHINES = EnumSet.of(
            MultiblockDefinition.GRINDER,
            MultiblockDefinition.SIEVE,
            MultiblockDefinition.HYDRAULIC_PRESS,
            MultiblockDefinition.OIL_HEATER,
            MultiblockDefinition.REFINERY
    );
    private static final Map<MultiblockDefinition, RegistryObject<RecipeType<AdvancedProcessingRecipe>>>
            ADVANCED_PROCESSING_TYPES = new EnumMap<>(MultiblockDefinition.class);
    private static final Map<MultiblockDefinition, RegistryObject<RecipeSerializer<AdvancedProcessingRecipe>>>
            ADVANCED_PROCESSING_SERIALIZERS = new EnumMap<>(MultiblockDefinition.class);

    public static final RegistryObject<RecipeType<CrushingRecipe>> CRUSHING_TYPE =
            ModRegistries.RECIPE_TYPES.register("crushing_table", () -> new RecipeType<>() {
                @Override
                public String toString() {
                    return "magneticraft:crushing_table";
                }
            });
    public static final RegistryObject<RecipeSerializer<CrushingRecipe>> CRUSHING_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("crushing_table", CrushingRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<SluiceRecipe>> SLUICE_TYPE = type("sluice_box");
    public static final RegistryObject<RecipeSerializer<SluiceRecipe>> SLUICE_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("sluice_box", SluiceRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<GasificationRecipe>> GASIFICATION_TYPE = type("gasification_unit");
    public static final RegistryObject<RecipeSerializer<GasificationRecipe>> GASIFICATION_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("gasification_unit", GasificationRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<ThermopileRecipe>> THERMOPILE_TYPE = type("thermopile");
    public static final RegistryObject<RecipeSerializer<ThermopileRecipe>> THERMOPILE_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register("thermopile", ThermopileRecipe.Serializer::new);
    public static final RegistryObject<RecipeType<FluidFuelRecipe>> FLUID_FUEL_TYPE =
            type(MultiblockDefinition.BIG_COMBUSTION_CHAMBER.id());
    public static final RegistryObject<RecipeSerializer<FluidFuelRecipe>> FLUID_FUEL_SERIALIZER =
            ModRegistries.RECIPE_SERIALIZERS.register(
                    MultiblockDefinition.BIG_COMBUSTION_CHAMBER.id(),
                    FluidFuelRecipe.Serializer::new
            );
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

    public static RegistryObject<RecipeType<AdvancedProcessingRecipe>> advancedProcessingType(
            MultiblockDefinition definition
    ) {
        RegistryObject<RecipeType<AdvancedProcessingRecipe>> type = ADVANCED_PROCESSING_TYPES.get(definition);
        if (type == null) {
            throw new IllegalArgumentException("No processing recipe type registered for " + definition);
        }
        return type;
    }

    public static Map<MultiblockDefinition, RegistryObject<RecipeType<AdvancedProcessingRecipe>>>
    advancedProcessingTypes() {
        return Collections.unmodifiableMap(ADVANCED_PROCESSING_TYPES);
    }

    public static RegistryObject<RecipeSerializer<AdvancedProcessingRecipe>> advancedProcessingSerializer(
            MultiblockDefinition definition
    ) {
        RegistryObject<RecipeSerializer<AdvancedProcessingRecipe>> serializer =
                ADVANCED_PROCESSING_SERIALIZERS.get(definition);
        if (serializer == null) {
            throw new IllegalArgumentException("No processing recipe serializer registered for " + definition);
        }
        return serializer;
    }

    public static Map<MultiblockDefinition, RegistryObject<RecipeSerializer<AdvancedProcessingRecipe>>>
    advancedProcessingSerializers() {
        return Collections.unmodifiableMap(ADVANCED_PROCESSING_SERIALIZERS);
    }

    private static <T extends net.minecraft.world.item.crafting.Recipe<?>> RegistryObject<RecipeType<T>> type(String id) {
        return ModRegistries.RECIPE_TYPES.register(id, () -> new RecipeType<>() {
            @Override
            public String toString() {
                return "magneticraft:" + id;
            }
        });
    }
}
