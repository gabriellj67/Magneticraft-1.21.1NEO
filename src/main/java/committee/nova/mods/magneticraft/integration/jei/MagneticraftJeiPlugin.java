package committee.nova.mods.magneticraft.integration.jei;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.crushingtable.CrushingRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.FluidFuelRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.ThermopileRecipe;
import committee.nova.mods.magneticraft.content.multiblock.MultiblockDefinition;
import committee.nova.mods.magneticraft.content.multiblock.recipe.AdvancedProcessingRecipe;
import committee.nova.mods.magneticraft.content.multiblock.recipe.PolymerizerRecipe;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearProcessRecipe;
import committee.nova.mods.magneticraft.content.nuclear.fuel.NuclearFuelGrade;
import committee.nova.mods.magneticraft.content.item.TieredElectricalBlockItem;
import committee.nova.mods.magneticraft.content.item.ProtectionBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleTransformerBlockItem;
import committee.nova.mods.magneticraft.content.network.electric.TransformerBlockItem;
import committee.nova.mods.magneticraft.init.ModAdvancedBlocks;
import committee.nova.mods.magneticraft.init.ModComputerContent;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.init.ModNuclearItems;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.recipe.RecipeType;
import mezz.jei.api.registration.ISubtypeRegistration;
import mezz.jei.api.registration.IRecipeCatalystRegistration;
import mezz.jei.api.registration.IRecipeCategoryRegistration;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.RecipeManager;

import java.util.ArrayList;
import java.util.List;

/** Optional JEI entry point. JEI owns discovery, so the common mod never links these API types. */
@JeiPlugin
public final class MagneticraftJeiPlugin implements IModPlugin {
    public static final RecipeType<CrushingRecipe> CRUSHING = type("crushing_table", CrushingRecipe.class);
    public static final RecipeType<SluiceRecipe> SLUICE = type("sluice_box", SluiceRecipe.class);
    public static final RecipeType<GasificationRecipe> GASIFICATION = type("gasification_unit", GasificationRecipe.class);
    public static final RecipeType<ThermopileRecipe> THERMOPILE = type("thermopile", ThermopileRecipe.class);
    public static final RecipeType<FluidFuelRecipe> FLUID_FUEL = type("fluid_fuel", FluidFuelRecipe.class);
    public static final RecipeType<AdvancedProcessingRecipe> ADVANCED_PROCESSING =
            type("advanced_processing", AdvancedProcessingRecipe.class);
    public static final RecipeType<PolymerizerRecipe> POLYMERIZING =
            type("polymerizing", PolymerizerRecipe.class);
    public static final RecipeType<NuclearProcessRecipe> NUCLEAR_PROCESSING =
            type("nuclear_processing", NuclearProcessRecipe.class);

    @Override
    public ResourceLocation getPluginUid() {
        return Magneticraft.id("jei_plugin");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        registration.useNbtForSubtypes(
                ModAdvancedBlocks.OIL_DEPOSIT_ITEM.get(),
                ModMachineItems.LOW_BATTERY.get(),
                ModMachineItems.MEDIUM_BATTERY.get(),
                ModMachineItems.ELECTRIC_DRILL.get(),
                ModMachineItems.ELECTRIC_CHAINSAW.get(),
                ModMachineItems.ELECTRIC_PISTON.get(),
                ModMachineItems.OIL_PROSPECTOR.get(),
                ModMachineBlocks.BATTERY.get().asItem(),
                ModMachineBlocks.machine(SingleBlockMachineDefinition.INFINITE_ENERGY).get().asItem(),
                ModNetworkBlocks.ELECTRIC_CABLE.get().asItem(),
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get().asItem(),
                ModNetworkBlocks.ELECTRIC_POLE.get().asItem(),
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get().asItem(),
                ModNetworkBlocks.BOX_TRANSFORMER.get().asItem(),
                ModNetworkBlocks.FUSE_BOX.get().asItem(),
                ModNetworkBlocks.CIRCUIT_BREAKER.get().asItem(),
                ModNetworkBlocks.ELECTRIC_SWITCH.get().asItem(),
                ModNetworkBlocks.DIODE.get().asItem(),
                ModNetworkBlocks.RESISTOR.get().asItem(),
                ModNetworkItems.FUSE.get(),
                ModComputerContent.FLOPPY_DISK.get(),
                ModNuclearItems.fuelAssembly(NuclearFuelGrade.LOW_ENRICHMENT).get(),
                ModNuclearItems.fuelAssembly(NuclearFuelGrade.STANDARD_ENRICHMENT).get(),
                ModNuclearItems.fuelAssembly(NuclearFuelGrade.HIGH_ENRICHMENT).get(),
                ModNuclearItems.SEALED_SPENT_FUEL_CASK.get()
        );
    }

    @Override
    public void registerCategories(IRecipeCategoryRegistration registration) {
        var guiHelper = registration.getJeiHelpers().getGuiHelper();
        registration.addRecipeCategories(
                new CrushingRecipeCategory(guiHelper),
                new SluiceRecipeCategory(guiHelper),
                new GasificationRecipeCategory(guiHelper),
                new ThermopileRecipeCategory(guiHelper),
                new FluidFuelRecipeCategory(guiHelper),
                new AdvancedProcessingRecipeCategory(guiHelper),
                new PolymerizerRecipeCategory(guiHelper),
                new NuclearProcessingRecipeCategory(guiHelper)
        );
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        registerElectricalInformation(registration);
        ClientLevel level = Minecraft.getInstance().level;
        if (level == null) {
            return;
        }
        RecipeManager recipes = level.getRecipeManager();
        registration.addRecipes(CRUSHING, recipes.getAllRecipesFor(ModRecipeTypes.CRUSHING_TYPE.get()));
        registration.addRecipes(SLUICE, recipes.getAllRecipesFor(ModRecipeTypes.SLUICE_TYPE.get()));
        registration.addRecipes(GASIFICATION, recipes.getAllRecipesFor(ModRecipeTypes.GASIFICATION_TYPE.get()));
        registration.addRecipes(THERMOPILE, recipes.getAllRecipesFor(ModRecipeTypes.THERMOPILE_TYPE.get()));
        registration.addRecipes(FLUID_FUEL, recipes.getAllRecipesFor(ModRecipeTypes.FLUID_FUEL_TYPE.get()));
        registration.addRecipes(POLYMERIZING, recipes.getAllRecipesFor(ModRecipeTypes.POLYMERIZING_TYPE.get()));
        registration.addRecipes(NUCLEAR_PROCESSING,
                recipes.getAllRecipesFor(ModRecipeTypes.NUCLEAR_PROCESSING_TYPE.get()));
        registration.addRecipes(
                ADVANCED_PROCESSING,
                ModRecipeTypes.advancedProcessingTypes().values().stream()
                        .flatMap(type -> recipes.getAllRecipesFor(type.get()).stream())
                        .toList()
        );
    }

    private static void registerElectricalInformation(IRecipeRegistration registration) {
        registration.addItemStackInfo(
                tieredStacks(
                        ModNetworkBlocks.ELECTRIC_CABLE.get(),
                        ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                        ModNetworkBlocks.ELECTRIC_POLE.get()
                ),
                Component.translatable("jei.magneticraft.electrical.lines")
        );
        registration.addItemStackInfo(
                tieredStacks(
                        ModMachineBlocks.BATTERY.get(),
                        ModMachineBlocks.machine(SingleBlockMachineDefinition.INFINITE_ENERGY).get()
                ),
                Component.translatable("jei.magneticraft.electrical.storage")
        );

        List<ItemStack> protections = new ArrayList<>();
        ProtectionBlockItem breaker = (ProtectionBlockItem) ModNetworkBlocks.CIRCUIT_BREAKER.get().asItem();
        VoltageTierIds.BUILT_IN.forEach(tier -> {
            protections.add(TieredElectricalBlockItem.stackForTier(ModNetworkBlocks.FUSE_BOX.get(), tier));
            ElectricalRatingIds.BUILT_IN.forEach(rating -> {
                protections.add(breaker.stackFor(tier, rating));
                protections.add(ModNetworkItems.FUSE.get().stackFor(tier, rating));
            });
        });
        registration.addItemStackInfo(
                protections,
                Component.translatable("jei.magneticraft.electrical.protection")
        );

        TransformerBlockItem box = (TransformerBlockItem) ModNetworkBlocks.BOX_TRANSFORMER.get().asItem();
        ElectricPoleTransformerBlockItem pole =
                (ElectricPoleTransformerBlockItem) ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get().asItem();
        registration.addItemStackInfo(
                List.of(
                        box.stackForProfile(TransformerProfileIds.LV_TO_MV, VoltageTierIds.LOW),
                        box.stackForProfile(TransformerProfileIds.MV_TO_HV, VoltageTierIds.MEDIUM),
                        pole.stackForProfile(TransformerProfileIds.LV_TO_MV, VoltageTierIds.LOW),
                        pole.stackForProfile(TransformerProfileIds.MV_TO_HV, VoltageTierIds.MEDIUM)
                ),
                Component.translatable("jei.magneticraft.electrical.transformers")
        );
        registration.addItemStackInfo(
                new ItemStack(ModMachineItems.VOLTMETER.get()),
                Component.translatable("jei.magneticraft.electrical.voltmeter")
        );
    }

    private static List<ItemStack> tieredStacks(net.minecraft.world.level.block.Block... blocks) {
        List<ItemStack> stacks = new ArrayList<>(blocks.length * VoltageTierIds.BUILT_IN.size());
        for (var block : blocks) {
            VoltageTierIds.BUILT_IN.forEach(tier ->
                    stacks.add(TieredElectricalBlockItem.stackForTier(block, tier))
            );
        }
        return List.copyOf(stacks);
    }

    @Override
    public void registerRecipeCatalysts(IRecipeCatalystRegistration registration) {
        registration.addRecipeCatalysts(CRUSHING, ModMachineBlocks.CRUSHING_TABLE.get());
        registration.addRecipeCatalysts(
                SLUICE,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.SLUICE_BOX).get()
        );
        registration.addRecipeCatalysts(
                GASIFICATION,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.GASIFICATION_UNIT).get()
        );
        registration.addRecipeCatalysts(
                THERMOPILE,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.THERMOPILE).get()
        );
        registration.addRecipeCatalysts(
                FLUID_FUEL,
                ModAdvancedBlocks.controller(MultiblockDefinition.BIG_COMBUSTION_CHAMBER).get(),
                ModMachineBlocks.machine(SingleBlockMachineDefinition.INTERNAL_COMBUSTION_ENGINE).get()
        );
        registration.addRecipeCatalysts(
                ADVANCED_PROCESSING,
                ModAdvancedBlocks.controller(MultiblockDefinition.GRINDER).get(),
                ModAdvancedBlocks.controller(MultiblockDefinition.SIEVE).get(),
                ModAdvancedBlocks.controller(MultiblockDefinition.HYDRAULIC_PRESS).get(),
                ModAdvancedBlocks.controller(MultiblockDefinition.OIL_HEATER).get(),
                ModAdvancedBlocks.controller(MultiblockDefinition.REFINERY).get()
        );
        registration.addRecipeCatalysts(
                POLYMERIZING,
                ModAdvancedBlocks.controller(MultiblockDefinition.POLYMERIZER).get()
        );
        registration.addRecipeCatalysts(
                NUCLEAR_PROCESSING,
                ModNuclearBlocks.controller(
                        committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType.URANIUM_PROCESSOR
                ).get(),
                ModNuclearBlocks.controller(
                        committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType.CENTRIFUGE_CASCADE
                ).get(),
                ModNuclearBlocks.controller(
                        committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType.FUEL_FABRICATOR
                ).get()
        );
    }

    private static <T> RecipeType<T> type(String path, Class<? extends T> recipeClass) {
        return RecipeType.create(Magneticraft.MOD_ID, path, recipeClass);
    }
}
