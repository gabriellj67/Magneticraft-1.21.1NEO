package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.material.Metal;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.material.Fluid;

/**
 * Stable tag keys used by registration consumers and generated data.
 *
 * <p>NeoForge's cross-loader "common" tag convention moved from Forge's
 * {@code forge:} namespace to {@code c:} (confirmed by reading
 * {@code net.neoforged.neoforge.common.Tags} in the sources jar - e.g.
 * {@code Tags.Items.INGOTS_COPPER} resolves to {@code c:ingots/copper}).
 * Path segments are otherwise unchanged (still {@code ores/x}, {@code
 * ingots/x}, {@code storage_blocks/x}, ...) with one confirmed exception:
 * the wrench tool tag is singular ({@code tools/wrench}, matching
 * {@code Tags.Items.TOOLS_WRENCH}), not the Forge original's plural
 * {@code tools/wrenches}.</p>
 */
public final class ModTags {
    private ModTags() {
    }

    public static final class Blocks {
        public static final TagKey<Block> LIMESTONE_BLOCKS = mod("limestone_blocks");
        public static final TagKey<Block> BURNT_LIMESTONE_BLOCKS = mod("burnt_limestone_blocks");
        public static final TagKey<Block> LIMESTONE_TILES = mod("limestone_tiles");

        private Blocks() {
        }

        public static TagKey<Block> ore(String material) {
            return common("ores/" + material);
        }

        public static TagKey<Block> storageBlock(String material) {
            return common("storage_blocks/" + material);
        }

        private static TagKey<Block> mod(String path) {
            return BlockTags.create(Magneticraft.id(path));
        }

        private static TagKey<Block> common(String path) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> LIGHT_PLATES = mod("light_plates");
        public static final TagKey<Item> HEAVY_PLATES = mod("heavy_plates");
        public static final TagKey<Item> CHUNKS = mod("chunks");
        public static final TagKey<Item> ROCKY_CHUNKS = mod("rocky_chunks");
        public static final TagKey<Item> HAMMERS = mod("hammers");
        public static final TagKey<Item> WRENCHES = common("tools/wrench");
        public static final TagKey<Item> SULFUR = mod("sulfur");
        public static final TagKey<Item> SULFUR_DUST = common("dusts/sulfur");
        public static final TagKey<Item> PLASTIC_SHEETS = common("plates/plastic");
        public static final TagKey<Item> RUBBER = common("rubber");
        public static final TagKey<Item> NUCLEAR_FUEL_ASSEMBLIES = mod("nuclear_fuel_assemblies");
        public static final TagKey<Item> URANIUM_DUSTS = common("dusts/uranium");
        public static final TagKey<Item> ZIRCONIUM_DUSTS = common("dusts/zirconium");
        public static final TagKey<Item> BORON_DUSTS = common("dusts/boron");

        private Items() {
        }

        public static TagKey<Item> ingot(Metal metal) {
            return common("ingots/" + metal.id());
        }

        public static TagKey<Item> nugget(Metal metal) {
            return common("nuggets/" + metal.id());
        }

        public static TagKey<Item> dust(Metal metal) {
            return common("dusts/" + metal.id());
        }

        public static TagKey<Item> lightPlate(Metal metal) {
            return mod("light_plates/" + metal.id());
        }

        public static TagKey<Item> heavyPlate(Metal metal) {
            return mod("heavy_plates/" + metal.id());
        }

        public static TagKey<Item> chunk(Metal metal) {
            return mod("chunks/" + metal.id());
        }

        public static TagKey<Item> rockyChunk(Metal metal) {
            return mod("rocky_chunks/" + metal.id());
        }

        public static TagKey<Item> ore(String material) {
            return common("ores/" + material);
        }

        public static TagKey<Item> storageBlock(String material) {
            return common("storage_blocks/" + material);
        }

        public static TagKey<Item> aluminiumAlias(String form) {
            return common(form + "/aluminum");
        }

        public static TagKey<Item> aluminiumChunkAlias(boolean rocky) {
            return mod((rocky ? "rocky_chunks/" : "chunks/") + "aluminum");
        }

        private static TagKey<Item> mod(String path) {
            return ItemTags.create(Magneticraft.id(path));
        }

        private static TagKey<Item> common(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("c", path));
        }
    }

    public static final class Fluids {
        private Fluids() {
        }

        public static TagKey<Fluid> magneticraft(FluidDefinition definition) {
            return FluidTags.create(Magneticraft.id(definition.id()));
        }

        public static TagKey<Fluid> common(FluidDefinition definition) {
            return FluidTags.create(ResourceLocation.fromNamespaceAndPath("c", definition.id()));
        }
    }
}
