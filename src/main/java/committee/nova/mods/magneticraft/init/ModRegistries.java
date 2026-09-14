package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.item.PortableEnergyComponents;
import committee.nova.mods.magneticraft.content.nuclear.radiation.NuclearProtectionMaterial;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.ArmorMaterial;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.inventory.MenuType;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.sounds.SoundEvent;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;

/**
 * Owns the root NeoForge registries used by Magneticraft content tasks.
 */
public final class ModRegistries {
    public static final DeferredRegister<Block> BLOCKS =
            DeferredRegister.create(Registries.BLOCK, Magneticraft.MOD_ID);
    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(Registries.ITEM, Magneticraft.MOD_ID);
    public static final DeferredRegister<FluidType> FLUID_TYPES =
            DeferredRegister.create(NeoForgeRegistries.Keys.FLUID_TYPES, Magneticraft.MOD_ID);
    public static final DeferredRegister<Fluid> FLUIDS =
            DeferredRegister.create(Registries.FLUID, Magneticraft.MOD_ID);
    public static final DeferredRegister<CreativeModeTab> CREATIVE_MODE_TABS =
            DeferredRegister.create(Registries.CREATIVE_MODE_TAB, Magneticraft.MOD_ID);
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITY_TYPES =
            DeferredRegister.create(Registries.BLOCK_ENTITY_TYPE, Magneticraft.MOD_ID);
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(Registries.MENU, Magneticraft.MOD_ID);
    public static final DeferredRegister<RecipeType<?>> RECIPE_TYPES =
            DeferredRegister.create(Registries.RECIPE_TYPE, Magneticraft.MOD_ID);
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Magneticraft.MOD_ID);
    public static final DeferredRegister<SoundEvent> SOUND_EVENTS =
            DeferredRegister.create(Registries.SOUND_EVENT, Magneticraft.MOD_ID);
    public static final DeferredRegister<Feature<?>> FEATURES =
            DeferredRegister.create(Registries.FEATURE, Magneticraft.MOD_ID);
    public static final DeferredRegister<DataComponentType<?>> DATA_COMPONENT_TYPES =
            DeferredRegister.create(Registries.DATA_COMPONENT_TYPE, Magneticraft.MOD_ID);
    public static final DeferredRegister<ArmorMaterial> ARMOR_MATERIALS =
            DeferredRegister.create(Registries.ARMOR_MATERIAL, Magneticraft.MOD_ID);

    private ModRegistries() {
    }

    public static void register(IEventBus modBus) {
        PortableEnergyComponents.bootstrap();
        NuclearProtectionMaterial.bootstrap();
        ModBlocks.bootstrap();
        ModItems.bootstrap();
        ModNuclearItems.bootstrap();
        ModNuclearBlocks.bootstrap();
        ModFluids.bootstrap();
        ModMachineBlocks.bootstrap();
        ModMachineItems.bootstrap();
        ModNetworkBlocks.bootstrap();
        ModNetworkItems.bootstrap();
        ModAdvancedBlocks.bootstrap();
        ModComputerContent.bootstrap();
        ModBlockEntities.bootstrap();
        ModMenus.bootstrap();
        ModRecipeTypes.bootstrap();
        ModSounds.bootstrap();
        ModFeatures.bootstrap();
        ModCreativeTabs.bootstrap();

        modBus.addListener(ModBlockEntities::registerCapabilities);
        modBus.addListener(ModComputerContent::registerCapabilities);

        FLUID_TYPES.register(modBus);
        FLUIDS.register(modBus);
        BLOCKS.register(modBus);
        ITEMS.register(modBus);
        BLOCK_ENTITY_TYPES.register(modBus);
        MENU_TYPES.register(modBus);
        RECIPE_TYPES.register(modBus);
        RECIPE_SERIALIZERS.register(modBus);
        SOUND_EVENTS.register(modBus);
        FEATURES.register(modBus);
        CREATIVE_MODE_TABS.register(modBus);
        DATA_COMPONENT_TYPES.register(modBus);
        ARMOR_MATERIALS.register(modBus);
    }
}
