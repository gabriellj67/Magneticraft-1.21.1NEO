package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.fluid.FluidDefinition;
import committee.nova.mods.magneticraft.content.fluid.MagneticraftFluidType;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.FlowingFluid;
import net.neoforged.neoforge.fluids.BaseFlowingFluid;
import net.neoforged.neoforge.fluids.FluidType;
import net.neoforged.neoforge.registries.DeferredHolder;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Registers each fluid as a complete type/source/flowing/block/bucket family.
 */
public final class ModFluids {
    private static final Map<FluidDefinition, FluidFamily> FAMILIES =
            new EnumMap<>(FluidDefinition.class);
    private static final List<DeferredHolder<Item, ? extends Item>> BUCKETS = new ArrayList<>();

    static {
        for (FluidDefinition definition : FluidDefinition.values()) {
            FluidFamily family = new FluidFamily(definition);
            FAMILIES.put(definition, family);
            BUCKETS.add(family.bucket());
        }
    }

    private ModFluids() {
    }

    public static void bootstrap() {
    }

    public static FluidFamily get(FluidDefinition definition) {
        FluidFamily family = FAMILIES.get(definition);
        if (family == null) {
            throw new IllegalArgumentException("No fluid family registered for " + definition);
        }
        return family;
    }

    public static List<DeferredHolder<Item, ? extends Item>> buckets() {
        return List.copyOf(BUCKETS);
    }

    public static final class FluidFamily {
        private final DeferredHolder<FluidType, FluidType> type;
        private final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> source;
        private final DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> flowing;
        private final DeferredHolder<net.minecraft.world.level.block.Block, LiquidBlock> block;
        private final DeferredHolder<Item, Item> bucket;

        private FluidFamily(FluidDefinition definition) {
            this.type = ModRegistries.FLUID_TYPES.register(
                    definition.id(),
                    () -> new MagneticraftFluidType(definition)
            );
            this.source = ModRegistries.FLUIDS.register(
                    definition.id(),
                    () -> new BaseFlowingFluid.Source(properties())
            );
            this.flowing = ModRegistries.FLUIDS.register(
                    definition.id() + "_flowing",
                    () -> new BaseFlowingFluid.Flowing(properties())
            );
            this.block = ModRegistries.BLOCKS.register(
                    definition.id(),
                    () -> new LiquidBlock(
                            source.get(),
                            BlockBehaviour.Properties.of()
                                    .noCollission()
                                    .strength(100.0F)
                                    .noLootTable()
                    )
            );
            this.bucket = ModRegistries.ITEMS.register(
                    definition.id() + "_bucket",
                    () -> new BucketItem(
                            source.get(),
                            new Item.Properties().craftRemainder(Items.BUCKET).stacksTo(1)
                    )
            );
        }

        public DeferredHolder<FluidType, FluidType> type() {
            return type;
        }

        public DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> source() {
            return source;
        }

        public DeferredHolder<net.minecraft.world.level.material.Fluid, FlowingFluid> flowing() {
            return flowing;
        }

        public DeferredHolder<net.minecraft.world.level.block.Block, LiquidBlock> block() {
            return block;
        }

        public DeferredHolder<Item, Item> bucket() {
            return bucket;
        }

        private BaseFlowingFluid.Properties properties() {
            return new BaseFlowingFluid.Properties(type, source, flowing)
                    .block(block)
                    .bucket(bucket);
        }
    }
}
