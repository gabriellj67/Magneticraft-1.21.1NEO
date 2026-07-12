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
            return forge("ores/" + material);
        }

        public static TagKey<Block> storageBlock(String material) {
            return forge("storage_blocks/" + material);
        }

        private static TagKey<Block> mod(String path) {
            return BlockTags.create(Magneticraft.id(path));
        }

        private static TagKey<Block> forge(String path) {
            return BlockTags.create(ResourceLocation.fromNamespaceAndPath("forge", path));
        }
    }

    public static final class Items {
        public static final TagKey<Item> LIGHT_PLATES = mod("light_plates");
        public static final TagKey<Item> HEAVY_PLATES = mod("heavy_plates");
        public static final TagKey<Item> CHUNKS = mod("chunks");
        public static final TagKey<Item> ROCKY_CHUNKS = mod("rocky_chunks");
        public static final TagKey<Item> HAMMERS = mod("hammers");
        public static final TagKey<Item> WRENCHES = forge("tools/wrenches");
        public static final TagKey<Item> SULFUR = mod("sulfur");
        public static final TagKey<Item> SULFUR_DUST = forge("dusts/sulfur");

        private Items() {
        }

        public static TagKey<Item> ingot(Metal metal) {
            return forge("ingots/" + metal.id());
        }

        public static TagKey<Item> nugget(Metal metal) {
            return forge("nuggets/" + metal.id());
        }

        public static TagKey<Item> dust(Metal metal) {
            return forge("dusts/" + metal.id());
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
            return forge("ores/" + material);
        }

        public static TagKey<Item> storageBlock(String material) {
            return forge("storage_blocks/" + material);
        }

        public static TagKey<Item> aluminiumAlias(String form) {
            return forge(form + "/aluminum");
        }

        public static TagKey<Item> aluminiumChunkAlias(boolean rocky) {
            return mod((rocky ? "rocky_chunks/" : "chunks/") + "aluminum");
        }

        private static TagKey<Item> mod(String path) {
            return ItemTags.create(Magneticraft.id(path));
        }

        private static TagKey<Item> forge(String path) {
            return ItemTags.create(ResourceLocation.fromNamespaceAndPath("forge", path));
        }
    }

    public static final class Fluids {
        private Fluids() {
        }

        public static TagKey<Fluid> magneticraft(FluidDefinition definition) {
            return FluidTags.create(Magneticraft.id(definition.id()));
        }

        public static TagKey<Fluid> forge(FluidDefinition definition) {
            return FluidTags.create(ResourceLocation.fromNamespaceAndPath("forge", definition.id()));
        }
    }
}
