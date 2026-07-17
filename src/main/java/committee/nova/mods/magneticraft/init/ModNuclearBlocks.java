package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPartRole;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraftforge.registries.RegistryObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

/** Blocks shared by the compressed nuclear fuel front end. */
public final class ModNuclearBlocks {
    private static final List<RegistryObject<? extends Item>> BLOCK_ITEMS = new ArrayList<>();
    private static final Map<NuclearFacilityType, RegistryObject<Block>> CONTROLLERS =
            new EnumMap<>(NuclearFacilityType.class);

    public static final RegistryObject<Block> FACILITY_CASING = register(
            "nuclear_facility_casing", () -> new Block(partProperties()));
    public static final RegistryObject<Block> PROCESS_CORE = register(
            "nuclear_process_core", () -> new Block(partProperties()));
    public static final RegistryObject<Block> CENTRIFUGE_STAGE = register(
            "centrifuge_stage", () -> new Block(partProperties()));
    public static final RegistryObject<Block> ITEM_INPUT_PORT = register(
            "nuclear_item_input_port",
            () -> new NuclearFacilityPortBlock(NuclearFacilityPartRole.ITEM_INPUT, partProperties()));
    public static final RegistryObject<Block> ITEM_OUTPUT_PORT = register(
            "nuclear_item_output_port",
            () -> new NuclearFacilityPortBlock(NuclearFacilityPartRole.ITEM_OUTPUT, partProperties()));
    public static final RegistryObject<Block> ELECTRICAL_PORT = register(
            "nuclear_electrical_port",
            () -> new NuclearFacilityPortBlock(NuclearFacilityPartRole.ELECTRICAL, controllerProperties()));

    static {
        for (NuclearFacilityType type : NuclearFacilityType.values()) {
            RegistryObject<Block> block = register(
                    type.id(),
                    () -> new NuclearFacilityControllerBlock(type, controllerProperties())
            );
            CONTROLLERS.put(type, block);
        }
    }

    private ModNuclearBlocks() {
    }

    public static void bootstrap() {
    }

    public static RegistryObject<Block> controller(NuclearFacilityType type) {
        RegistryObject<Block> result = CONTROLLERS.get(type);
        if (result == null) {
            throw new IllegalArgumentException("No nuclear facility controller registered for " + type);
        }
        return result;
    }

    public static Map<NuclearFacilityType, RegistryObject<Block>> controllers() {
        return Collections.unmodifiableMap(CONTROLLERS);
    }

    public static List<RegistryObject<? extends Item>> blockItems() {
        return List.copyOf(BLOCK_ITEMS);
    }

    private static RegistryObject<Block> register(String id, Supplier<? extends Block> factory) {
        RegistryObject<Block> block = ModRegistries.BLOCKS.register(id, factory);
        BLOCK_ITEMS.add(ModRegistries.ITEMS.register(id, () -> new BlockItem(block.get(), new Item.Properties())));
        return block;
    }

    private static BlockBehaviour.Properties partProperties() {
        return BlockBehaviour.Properties.of()
                .mapColor(MapColor.METAL)
                .requiresCorrectToolForDrops()
                .strength(4.0F, 12.0F)
                .sound(SoundType.METAL);
    }

    private static BlockBehaviour.Properties controllerProperties() {
        return partProperties().noOcclusion();
    }
}
