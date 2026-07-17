package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityControllerBlock;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityControllerBlockEntity;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPartRole;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityPortBlock;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityType;
import committee.nova.mods.magneticraft.content.nuclear.facility.NuclearFacilityValidator;
import committee.nova.mods.magneticraft.init.ModNuclearBlocks;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

/** Loaded-world proof for formation and exact outward port exposure. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class NuclearFacilityGameTests {
    private static final String TEMPLATE = "base_content";

    private NuclearFacilityGameTests() {
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 80)
    public static void uraniumProcessorFormsAndExposesOnlyPortFaces(GameTestHelper helper) {
        NuclearFacilityType type = NuclearFacilityType.URANIUM_PROCESSOR;
        Direction facing = Direction.NORTH;
        BlockPos controller = helper.absolutePos(new BlockPos(3, 2, 3));
        int depth = type.minimumDepth();
        for (int y = 0; y < type.height(); y++) {
            for (int z = 0; z < depth; z++) {
                for (int x = 0; x < type.width(); x++) {
                    NuclearFacilityPartRole role = NuclearFacilityValidator.expectedRole(type, x, y, z, depth);
                    BlockPos position = NuclearFacilityValidator.worldPosition(type, controller, facing, x, y, z);
                    helper.getLevel().setBlock(position, state(role, type, facing), Block.UPDATE_ALL);
                }
            }
        }

        BlockPos inputPosition = NuclearFacilityValidator.worldPosition(type, controller, facing, 0, 1, 0);
        BlockPos electricalPosition = NuclearFacilityValidator.worldPosition(type, controller, facing, 1, 1, 2);
        helper.succeedWhen(() -> {
            helper.assertTrue(helper.getLevel().getBlockEntity(controller)
                            instanceof NuclearFacilityControllerBlockEntity facility && facility.formed(),
                    "Uranium processor did not form");
            var input = helper.getLevel().getBlockEntity(inputPosition);
            helper.assertTrue(input != null
                            && input.getCapability(ForgeCapabilities.ITEM_HANDLER, facing).isPresent(),
                    "Input port did not expose its outward face");
            helper.assertTrue(input != null
                            && !input.getCapability(ForgeCapabilities.ITEM_HANDLER, facing.getOpposite()).isPresent(),
                    "Input port exposed an internal face");
            var electrical = helper.getLevel().getBlockEntity(electricalPosition);
            helper.assertTrue(electrical instanceof committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost host
                            && host.supportsNetworkConnection(NetworkDomain.ELECTRICITY, facing.getOpposite()),
                    "Electrical port did not expose its outward face");
            helper.assertTrue(electrical instanceof committee.nova.mods.magneticraft.content.machine.framework.NetworkConnectionHost host
                            && !host.supportsNetworkConnection(NetworkDomain.ELECTRICITY, facing),
                    "Electrical port exposed an internal face");
        });
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 10)
    public static void completeFrontEndRecipeCatalogueLoads(GameTestHelper helper) {
        var recipes = helper.getLevel().getRecipeManager()
                .getAllRecipesFor(ModRecipeTypes.NUCLEAR_PROCESSING_TYPE.get());
        helper.assertTrue(recipes.size() == 7,
                "Expected 7 nuclear front-end recipes, loaded " + recipes.size());
        helper.assertTrue(recipes.stream().allMatch(recipe -> recipe.durationTicks() > 0
                        && recipe.joulesPerTick() > 0
                        && !recipe.ingredients().isEmpty()
                        && !recipe.results().isEmpty()),
                "Nuclear front-end recipe catalogue contains an incomplete entry");
        helper.succeed();
    }

    private static BlockState state(NuclearFacilityPartRole role, NuclearFacilityType type, Direction facing) {
        return switch (role) {
            case CONTROLLER -> ModNuclearBlocks.controller(type).get().defaultBlockState()
                    .setValue(NuclearFacilityControllerBlock.FACING, facing);
            case CASING -> ModNuclearBlocks.FACILITY_CASING.get().defaultBlockState();
            case PROCESS_CORE -> ModNuclearBlocks.PROCESS_CORE.get().defaultBlockState();
            case CENTRIFUGE_STAGE -> ModNuclearBlocks.CENTRIFUGE_STAGE.get().defaultBlockState();
            case ITEM_INPUT -> ModNuclearBlocks.ITEM_INPUT_PORT.get().defaultBlockState()
                    .setValue(NuclearFacilityPortBlock.FACING, facing);
            case ITEM_OUTPUT -> ModNuclearBlocks.ITEM_OUTPUT_PORT.get().defaultBlockState()
                    .setValue(NuclearFacilityPortBlock.FACING, facing);
            case ELECTRICAL -> ModNuclearBlocks.ELECTRICAL_PORT.get().defaultBlockState()
                    .setValue(NuclearFacilityPortBlock.FACING, facing.getOpposite());
            case AIR -> throw new IllegalArgumentException("Structure pattern cannot contain air");
        };
    }
}
