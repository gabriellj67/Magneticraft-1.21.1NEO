package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.item.CopperWireCoilItem;
import committee.nova.mods.magneticraft.content.item.TieredElectricalBlockItem;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlock;
import committee.nova.mods.magneticraft.content.machine.windturbine.WindTurbineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.singleblock.SingleBlockMachineDefinition;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.BoxTransformerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricConnectorBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlock;
import committee.nova.mods.magneticraft.content.network.electric.ElectricPoleBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.PoleSegment;
import committee.nova.mods.magneticraft.content.network.electric.TeslaTowerBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.WirelessEnergyReceiverBlockEntity;
import committee.nova.mods.magneticraft.content.network.block.ConduitBlock;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.system.network.longdistance.LongDistanceElectricityService;
import committee.nova.mods.magneticraft.system.network.electric.item.ElectricalRatingIds;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import committee.nova.mods.magneticraft.system.network.runtime.NetworkDomain;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.registries.ForgeRegistries;

import java.util.List;
import java.util.Optional;

/** Runtime contracts for long-distance electricity, Tesla transfer and wind generation. */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class LongDistanceElectricGameTests {
    private static final String TEMPLATE = "base_content";
    private static final String ADVANCED_TEMPLATE = "advanced_systems";

    private LongDistanceElectricGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void everyTierPayloadItemDisplaysItsVoltageTier(GameTestHelper helper) {
        ItemStack cable = TieredElectricalBlockItem.stackForTier(
                ModNetworkBlocks.ELECTRIC_CABLE.get(),
                VoltageTierIds.LOW
        );
        ItemStack fuse = ModNetworkItems.FUSE.get().stackFor(
                VoltageTierIds.LOW,
                ElectricalRatingIds.STANDARD
        );
        ItemStack transformer = new ItemStack(ModNetworkBlocks.BOX_TRANSFORMER.get());
        new TieredElectricalItemData(
                VoltageTierIds.LOW,
                Optional.empty(),
                Optional.of(TransformerProfileIds.LV_TO_MV)
        ).write(transformer);
        ItemStack transformerPole = new ItemStack(ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get());
        new TieredElectricalItemData(
                VoltageTierIds.MEDIUM,
                Optional.empty(),
                Optional.of(TransformerProfileIds.MV_TO_HV)
        ).write(transformerPole);

        for (ItemStack stack : List.of(cable, fuse, transformer, transformerPole)) {
            String serializedName = Component.Serializer.toJson(stack.getItem().getName(stack));
            helper.assertTrue(serializedName.contains("item.magneticraft.tiered_name"),
                    "Tiered item did not expose its voltage tier in the display name: " + stack.getItem());
            helper.assertTrue(serializedName.contains("voltage_tier.magneticraft."),
                    "Tiered item display name did not resolve a voltage-tier translation: " + stack.getItem());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void windTurbinePreservesReleasedAllSideElectricalPorts(GameTestHelper helper) {
        BlockPos position = new BlockPos(1, 1, 1);
        helper.setBlock(position, ModNetworkBlocks.WIND_TURBINE.get().defaultBlockState()
                .setValue(WindTurbineBlock.FACING, Direction.NORTH));
        WindTurbineBlockEntity turbine = (WindTurbineBlockEntity) helper.getBlockEntity(position);
        for (Direction side : Direction.values()) {
            helper.assertTrue(turbine.electricity().connectionSides().contains(side),
                    "Wind turbine lost its released electrical port on " + side.getName());
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void restoredBlocksAndBlockEntitiesAreRegistered(GameTestHelper helper) {
        assertBlockId(helper, "electric_connector", ModNetworkBlocks.ELECTRIC_CONNECTOR.get());
        assertBlockId(helper, "electric_pole", ModNetworkBlocks.ELECTRIC_POLE.get());
        assertBlockId(helper, "electric_pole_transformer", ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get());
        assertBlockId(helper, "box_transformer", ModNetworkBlocks.BOX_TRANSFORMER.get());
        assertBlockId(helper, "tesla_tower", ModNetworkBlocks.TESLA_TOWER.get());
        assertBlockId(helper, "wireless_energy_receiver", ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get());
        assertBlockId(helper, "wind_turbine", ModNetworkBlocks.WIND_TURBINE.get());

        helper.assertTrue(
                "electric_connector".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.ELECTRIC_CONNECTOR.get()).getPath()),
                "Electric connector block entity has the wrong registry id"
        );
        helper.assertTrue(
                "electric_pole".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.ELECTRIC_POLE.get()).getPath()),
                "Electric pole block entity has the wrong registry id"
        );
        helper.assertTrue(
                "electric_pole_transformer".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.ELECTRIC_POLE_TRANSFORMER.get()).getPath()),
                "Transformer pole block entity has the wrong registry id"
        );
        helper.assertTrue(
                "box_transformer".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.BOX_TRANSFORMER.get()).getPath()),
                "Box transformer block entity has the wrong registry id"
        );
        helper.assertTrue(
                "tesla_tower".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.TESLA_TOWER.get()).getPath()),
                "Tesla tower block entity has the wrong registry id"
        );
        helper.assertTrue(
                "wireless_energy_receiver".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.WIRELESS_ENERGY_RECEIVER.get()).getPath()),
                "Wireless receiver block entity has the wrong registry id"
        );
        helper.assertTrue(
                "wind_turbine".equals(ForgeRegistries.BLOCK_ENTITY_TYPES
                        .getKey(ModBlockEntities.WIND_TURBINE.get()).getPath()),
                "Wind turbine block entity has the wrong registry id"
        );

        require(helper, new BlockPos(1, 4, 1), ElectricConnectorBlockEntity.class,
                () -> placeConnector(helper, new BlockPos(1, 4, 1)));
        placePole(helper, new BlockPos(20, 8, 1), false);
        placePole(helper, new BlockPos(40, 8, 1), true);
        require(helper, new BlockPos(45, 8, 1), BoxTransformerBlockEntity.class,
                () -> helper.setBlock(new BlockPos(45, 8, 1), ModNetworkBlocks.BOX_TRANSFORMER.get()));
        require(helper, new BlockPos(1, 4, 5), TeslaTowerBlockEntity.class,
                () -> helper.setBlock(new BlockPos(1, 4, 5), ModNetworkBlocks.TESLA_TOWER.get()));
        require(helper, new BlockPos(5, 4, 5), WirelessEnergyReceiverBlockEntity.class,
                () -> placeReceiver(helper, new BlockPos(5, 4, 5)));
        require(helper, new BlockPos(9, 12, 5), WindTurbineBlockEntity.class,
                () -> helper.setBlock(new BlockPos(9, 12, 5), ModNetworkBlocks.WIND_TURBINE.get()));
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void tieredCraftingRecipesExposeVersionedResultIdentity(GameTestHelper helper) {
        assertTieredRecipe(helper, "crafting/battery_box", ModMachineBlocks.BATTERY.get().asItem(),
                VoltageTierIds.LOW);
        assertTieredRecipe(helper, "crafting/battery_box_medium_voltage", ModMachineBlocks.BATTERY.get().asItem(),
                VoltageTierIds.MEDIUM);
        assertTieredRecipe(helper, "crafting/battery_box_high_voltage", ModMachineBlocks.BATTERY.get().asItem(),
                VoltageTierIds.HIGH);
        assertTieredRecipe(helper, "crafting/electric_cable", ModNetworkBlocks.ELECTRIC_CABLE.get().asItem(),
                VoltageTierIds.LOW);
        assertTieredRecipe(helper, "crafting/electric_cable_medium_voltage",
                ModNetworkBlocks.ELECTRIC_CABLE.get().asItem(), VoltageTierIds.MEDIUM);
        assertTieredRecipe(helper, "crafting/electric_cable_high_voltage",
                ModNetworkBlocks.ELECTRIC_CABLE.get().asItem(), VoltageTierIds.HIGH);
        assertTieredRecipe(helper, "crafting/electric_connector", ModNetworkBlocks.ELECTRIC_CONNECTOR.get().asItem(),
                VoltageTierIds.LOW);
        assertTieredRecipe(helper, "crafting/electric_connector_medium_voltage",
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get().asItem(), VoltageTierIds.MEDIUM);
        assertTieredRecipe(helper, "crafting/electric_connector_high_voltage",
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get().asItem(), VoltageTierIds.HIGH);
        assertTieredRecipe(helper, "crafting/electric_pole", ModNetworkBlocks.ELECTRIC_POLE.get().asItem(),
                VoltageTierIds.LOW);
        assertTieredRecipe(helper, "crafting/electric_pole_medium_voltage",
                ModNetworkBlocks.ELECTRIC_POLE.get().asItem(), VoltageTierIds.MEDIUM);
        assertTieredRecipe(helper, "crafting/electric_pole_high_voltage",
                ModNetworkBlocks.ELECTRIC_POLE.get().asItem(), VoltageTierIds.HIGH);
        assertTransformerRecipe(
                helper,
                "crafting/electric_pole_transformer",
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get().asItem(),
                VoltageTierIds.LOW,
                TransformerProfileIds.LV_TO_MV
        );
        assertTransformerRecipe(
                helper,
                "crafting/electric_pole_transformer_high_voltage",
                ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get().asItem(),
                VoltageTierIds.MEDIUM,
                TransformerProfileIds.MV_TO_HV
        );
        assertTransformerRecipe(
                helper,
                "crafting/box_transformer",
                ModNetworkBlocks.BOX_TRANSFORMER.get().asItem(),
                VoltageTierIds.LOW,
                TransformerProfileIds.LV_TO_MV
        );
        assertTransformerRecipe(
                helper,
                "crafting/box_transformer_high_voltage",
                ModNetworkBlocks.BOX_TRANSFORMER.get().asItem(),
                VoltageTierIds.MEDIUM,
                TransformerProfileIds.MV_TO_HV
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void boxTransformerKeepsTerminalsIsolatedAndTransfersByProfile(GameTestHelper helper) {
        BlockPos position = new BlockPos(5, 4, 5);
        helper.setBlock(position, ModNetworkBlocks.BOX_TRANSFORMER.get());
        BoxTransformerBlockEntity transformer = require(helper, position, BoxTransformerBlockEntity.class);
        helper.assertFalse(
                transformer.input().nodeKey().equals(transformer.output().nodeKey()),
                "Box transformer terminals collapsed to one physical node key"
        );
        helper.assertTrue(transformer.input().tierId().equals(VoltageTierIds.LOW),
                "Box transformer input did not bind to low voltage");
        helper.assertTrue(transformer.output().tierId().equals(VoltageTierIds.MEDIUM),
                "Box transformer output did not bind to medium voltage");

        var manager = PhysicalNetworkService.manager(helper.getLevel());
        transformer.input().node().setVoltage(120.0D);
        transformer.output().node().setVoltage(0.0D);
        double before = transformer.input().node().energyJoules() + transformer.output().node().energyJoules();
        transformer.transformerCoupler().coupler().setEnabled(false);
        manager.tick(helper.getLevel().getGameTime() + 2_000L);
        helper.assertTrue(transformer.output().node().energyJoules() == 0.0D,
                "Isolated transformer terminals exchanged energy without their coupler");

        transformer.transformerCoupler().coupler().setEnabled(true);
        manager.tick(helper.getLevel().getGameTime() + 2_001L);
        double delivered = transformer.output().node().energyJoules();
        double after = transformer.input().node().energyJoules() + delivered;
        helper.assertTrue(Math.abs(delivered - 768.0D) < 1.0E-6D,
                "LV-to-MV transformer did not apply its 800 J/t and 96% profile");
        helper.assertTrue(Math.abs((before - after) - 32.0D) < 1.0E-6D,
                "Transformer efficiency loss did not remain energy-conserving telemetry loss");

        ItemStack drop = Block.getDrops(
                        transformer.getBlockState(),
                        helper.getLevel(),
                        transformer.getBlockPos(),
                        transformer
                ).stream()
                .filter(stack -> stack.is(ModNetworkBlocks.BOX_TRANSFORMER.get().asItem()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Box transformer did not produce its block drop"));
        helper.assertTrue(
                TieredElectricalItemData.read(drop)
                        .flatMap(TieredElectricalItemData::transformerProfileId)
                        .filter(TransformerProfileIds.LV_TO_MV::equals)
                        .isPresent(),
                "Box transformer drop lost its transformer profile"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void electricPoleSegmentsUseReleasedNarrowCollision(GameTestHelper helper) {
        BlockPos bottom = new BlockPos(4, 2, 4);
        PoleSegment[] segments = {
                PoleSegment.DOWN_4,
                PoleSegment.DOWN_3,
                PoleSegment.DOWN_2,
                PoleSegment.DOWN_1,
                PoleSegment.BASE
        };
        for (int index = 0; index < segments.length; index++) {
            BlockPos position = bottom.above(index);
            var state = ModNetworkBlocks.ELECTRIC_POLE.get().defaultBlockState()
                    .setValue(ElectricPoleBlock.SEGMENT, segments[index]);
            helper.setBlock(position, state);
            var bounds = state.getCollisionShape(
                    helper.getLevel(), helper.absolutePos(position), CollisionContext.empty()
            ).bounds();
            helper.assertTrue(Math.abs(bounds.minX - 5.0D / 16.0D) < 1.0E-9D,
                    "Pole segment minimum X diverged from the released shape");
            helper.assertTrue(Math.abs(bounds.minZ - 5.0D / 16.0D) < 1.0E-9D,
                    "Pole segment minimum Z diverged from the released shape");
            helper.assertTrue(Math.abs(bounds.maxX - 11.0D / 16.0D) < 1.0E-9D,
                    "Pole segment maximum X diverged from the released shape");
            helper.assertTrue(Math.abs(bounds.maxZ - 11.0D / 16.0D) < 1.0E-9D,
                    "Pole segment maximum Z diverged from the released shape");
        }
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void connectorMountsOnCableAndJoinsThroughBackFace(GameTestHelper helper) {
        BlockPos cablePosition = new BlockPos(4, 4, 4);
        BlockPos connectorPosition = cablePosition.relative(Direction.NORTH);
        BlockPos cableWorldPosition = helper.absolutePos(cablePosition);
        BlockPos connectorWorldPosition = helper.absolutePos(connectorPosition);
        helper.setBlock(cablePosition, ModNetworkBlocks.ELECTRIC_CABLE.get());

        Player player = helper.makeMockSurvivalPlayer();
        ItemStack connectorStack = new ItemStack(ModNetworkBlocks.ELECTRIC_CONNECTOR.get());
        TieredElectricalItemData.forTier(VoltageTierIds.LOW).write(connectorStack);
        player.setItemInHand(InteractionHand.MAIN_HAND, connectorStack);
        InteractionResult placement = connectorStack.getItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(
                        Vec3.atCenterOf(cableWorldPosition).add(0.0D, 0.0D, -0.5D),
                        Direction.NORTH,
                        cableWorldPosition,
                        false
                )
        ));
        helper.assertTrue(placement.consumesAction(), "Electric connector could not be placed on an electric cable");
        helper.assertBlockPresent(ModNetworkBlocks.ELECTRIC_CONNECTOR.get(), connectorPosition);

        ElectricCableBlockEntity cable = require(helper, cablePosition, ElectricCableBlockEntity.class);
        ElectricConnectorBlockEntity connector = require(helper, connectorPosition, ElectricConnectorBlockEntity.class);
        helper.assertTrue(connector.getBlockState().getValue(BlockStateProperties.FACING) == Direction.NORTH,
                "Cable-mounted connector did not face away from its support");
        helper.assertTrue(connector.electricity().isSideEnabled(Direction.SOUTH),
                "Cable-mounted connector did not expose its electrical back face");
        helper.assertTrue(connector.electricity().isSideEnabled(Direction.NORTH),
                "Cable-mounted connector did not expose its outward native-J face");

        double cableShapeMinZ = helper.getLevel().getBlockState(cableWorldPosition)
                .getShape(helper.getLevel(), cableWorldPosition, CollisionContext.empty())
                .bounds().minZ;
        helper.assertTrue(Math.abs(cableShapeMinZ) < 1.0E-9D,
                "Electric cable did not render an arm toward the connector");

        helper.runAfterDelay(2, () -> {
            var manager = PhysicalNetworkService.manager(helper.getLevel());
            helper.assertTrue(manager.neighbors(NetworkDomain.ELECTRICITY, connectorWorldPosition)
                            .contains(cableWorldPosition.asLong()),
                    "Electric connector did not join the adjacent cable through its back face");
            connector.electricity().node().setVoltage(120.0D);
            cable.electricity().node().setVoltage(0.0D);
            manager.tick(helper.getLevel().getGameTime() + 1_000L);
            helper.assertTrue(cable.electricity().node().energyJoules() > 0.0D,
                    "Electric connector did not transfer energy into its supporting cable");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void tieredPlacementPreservesDropIdentityAndRejectsUnknownTier(GameTestHelper helper) {
        BlockPos position = new BlockPos(3, 4, 3);
        BlockPos support = position.relative(Direction.SOUTH);
        helper.setBlock(support, Blocks.STONE);

        Player player = helper.makeMockSurvivalPlayer();
        ItemStack mediumConnector = TieredElectricalBlockItem.stackForTier(
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                VoltageTierIds.MEDIUM
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, mediumConnector);
        InteractionResult placed = mediumConnector.getItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                connectorPlacementHit(helper, support)
        ));
        helper.assertTrue(placed.consumesAction(), "Valid medium-voltage connector was not placed");
        ElectricConnectorBlockEntity connector = require(helper, position, ElectricConnectorBlockEntity.class);
        helper.assertTrue(connector.electricity().tierId().equals(VoltageTierIds.MEDIUM),
                "Placed connector did not receive its item voltage tier");

        ItemStack tieredDrop = Block.getDrops(
                        connector.getBlockState(),
                        helper.getLevel(),
                        connector.getBlockPos(),
                        connector
                ).stream()
                .filter(stack -> stack.is(ModNetworkBlocks.ELECTRIC_CONNECTOR.get().asItem()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Connector did not produce its block drop"));
        helper.assertTrue(
                TieredElectricalItemData.read(tieredDrop)
                        .map(TieredElectricalItemData::tierId)
                        .filter(VoltageTierIds.MEDIUM::equals)
                        .isPresent(),
                "Connector drop did not preserve its voltage tier"
        );

        BlockPos invalidPosition = new BlockPos(7, 4, 3);
        BlockPos invalidSupport = invalidPosition.relative(Direction.SOUTH);
        helper.setBlock(invalidSupport, Blocks.STONE);
        ResourceLocation unknownTier = ResourceLocation.fromNamespaceAndPath("test", "unknown_voltage");
        ItemStack unknownConnector = TieredElectricalBlockItem.stackForTier(
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get(),
                unknownTier
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, unknownConnector);
        InteractionResult rejected = unknownConnector.getItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                connectorPlacementHit(helper, invalidSupport)
        ));
        helper.assertTrue(rejected == InteractionResult.FAIL, "Unknown voltage tier placement was not rejected");
        helper.assertBlockPresent(Blocks.AIR, invalidPosition);
        helper.assertTrue(unknownConnector.getCount() == 1, "Rejected tier placement consumed the item");

        BlockPos fixedTierMachinePosition = new BlockPos(10, 4, 3);
        var fixedTierMachine = ModMachineBlocks.machine(SingleBlockMachineDefinition.BRICK_FURNACE).get();
        helper.setBlock(fixedTierMachinePosition, fixedTierMachine);
        BlockEntity fixedTierMachineEntity = helper.getBlockEntity(fixedTierMachinePosition);
        ItemStack fixedTierDrop = Block.getDrops(
                        helper.getBlockState(fixedTierMachinePosition),
                        helper.getLevel(),
                        helper.absolutePos(fixedTierMachinePosition),
                        fixedTierMachineEntity
                ).stream()
                .filter(stack -> stack.is(fixedTierMachine.asItem()))
                .findFirst()
                .orElseThrow(() -> new AssertionError("Fixed-tier machine did not produce its block drop"));
        helper.assertTrue(TieredElectricalItemData.read(fixedTierDrop).isEmpty(),
                "Fixed-profile machine leaked a craft-selectable tier payload into its drop");

        BlockPos poleBottom = new BlockPos(14, 2, 3);
        BlockPos poleSupport = poleBottom.below();
        clearPoleVolume(helper, poleBottom);
        helper.setBlock(poleSupport, Blocks.STONE);
        ItemStack mediumPole = TieredElectricalBlockItem.stackForTier(
                ModNetworkBlocks.ELECTRIC_POLE.get(),
                VoltageTierIds.MEDIUM
        );
        player.setItemInHand(InteractionHand.MAIN_HAND, mediumPole);
        InteractionResult polePlaced = mediumPole.getItem().useOn(new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                polePlacementHit(helper, poleSupport)
        ));
        helper.assertTrue(polePlaced.consumesAction(),
                "Valid medium-voltage pole was not placed: result=" + polePlaced
                        + ", support=" + helper.getBlockState(poleSupport)
                        + ", bottom=" + helper.getBlockState(poleBottom)
                        + ", remaining=" + mediumPole.getCount());
        ElectricPoleBlockEntity pole = require(helper, poleBottom.above(4), ElectricPoleBlockEntity.class);
        helper.assertTrue(pole.electricity().tierId().equals(VoltageTierIds.MEDIUM),
                "Five-block pole did not apply its item tier to the top endpoint");
        clearPoleVolume(helper, poleBottom);
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void differentVoltageTiersDoNotFormEdgesOrVisualArms(GameTestHelper helper) {
        BlockPos lowPosition = new BlockPos(3, 4, 3);
        BlockPos mediumPosition = lowPosition.relative(Direction.EAST);
        helper.setBlock(lowPosition, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(mediumPosition, ModNetworkBlocks.ELECTRIC_CABLE.get());
        ElectricCableBlockEntity low = require(helper, lowPosition, ElectricCableBlockEntity.class);
        ElectricCableBlockEntity medium = require(helper, mediumPosition, ElectricCableBlockEntity.class);
        medium.electricity().applyTierFromPlacementData(VoltageTierIds.MEDIUM);
        ConduitBlock.refreshAround(helper.getLevel(), medium.getBlockPos());

        BlockPos firstConnector = new BlockPos(3, 4, 8);
        BlockPos secondConnector = new BlockPos(7, 4, 8);
        ElectricConnectorBlockEntity lowConnector = placeConnector(helper, firstConnector);
        ElectricConnectorBlockEntity mediumConnector = placeConnector(helper, secondConnector);
        mediumConnector.electricity().applyTierFromPlacementData(VoltageTierIds.MEDIUM);

        helper.runAfterDelay(2, () -> {
            BlockPos lowWorld = helper.absolutePos(lowPosition);
            BlockPos mediumWorld = helper.absolutePos(mediumPosition);
            var manager = PhysicalNetworkService.manager(helper.getLevel());
            helper.assertFalse(
                    manager.neighbors(NetworkDomain.ELECTRICITY, lowWorld).contains(mediumWorld.asLong()),
                    "Different voltage tiers joined the adjacent electrical graph"
            );
            helper.assertFalse(
                    helper.getLevel().getBlockState(lowWorld).getValue(ConduitBlock.EAST),
                    "Low-voltage cable rendered an arm toward a medium-voltage cable"
            );
            helper.assertFalse(
                    helper.getLevel().getBlockState(mediumWorld).getValue(ConduitBlock.WEST),
                    "Medium-voltage cable rendered an arm toward a low-voltage cable"
            );

            low.electricity().node().setVoltage(120.0D);
            medium.electricity().node().setVoltage(0.0D);
            manager.tick(helper.getLevel().getGameTime() + 2_000L);
            helper.assertTrue(medium.electricity().node().energyJoules() == 0.0D,
                    "Different voltage tiers exchanged adjacent electrical energy");

            LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
            helper.assertTrue(
                    service.connect(lowConnector.getBlockPos(), mediumConnector.getBlockPos()).result()
                            == LongDistanceElectricityService.ConnectionResult.INCOMPATIBLE_TIER,
                    "Different voltage tiers accepted a long-distance wire"
            );
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void copperWireCoilConnectsAtEightBlocksAndRejectsNine(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 4, 2);
        BlockPos atLimit = new BlockPos(9, 4, 2);
        BlockPos tooFar = new BlockPos(10, 4, 2);
        placeConnector(helper, first);
        placeConnector(helper, atLimit);
        placeConnector(helper, tooFar);

        Player player = helper.makeMockSurvivalPlayer();
        ItemStack coilStack = new ItemStack(ModNetworkItems.COPPER_WIRE_COIL.get(), 2);
        player.setItemInHand(InteractionHand.MAIN_HAND, coilStack);
        CopperWireCoilItem coil = (CopperWireCoilItem) coilStack.getItem();

        player.setShiftKeyDown(true);
        InteractionResult selected = coil.useOn(useContext(helper, player, first));
        helper.assertTrue(selected.consumesAction(), "Copper wire coil did not store its first endpoint");
        player.setShiftKeyDown(false);
        InteractionResult connected = coil.useOn(useContext(helper, player, atLimit));
        helper.assertTrue(connected.consumesAction(), "Copper wire coil did not handle its second endpoint");

        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        BlockPos firstWorld = helper.absolutePos(first);
        BlockPos limitWorld = helper.absolutePos(atLimit);
        helper.assertTrue(service.connectionsAt(firstWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(limitWorld)),
                "Connector wire did not persist at the eight-block limit");
        helper.assertTrue(service.connect(firstWorld, helper.absolutePos(tooFar)).result()
                        == LongDistanceElectricityService.ConnectionResult.TOO_FAR,
                "Connector wire accepted a nine-block span");
        helper.assertTrue(coilStack.getCount() == 1, "Successful wire connection did not consume exactly one coil");
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void polesUseSixteenBlockRangeAndTransformerBridgesPortDomains(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 8, 1);
        BlockPos atLimit = new BlockPos(17, 8, 1);
        BlockPos tooFar = new BlockPos(18, 8, 1);
        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        service.removeConnectionsAt(helper.absolutePos(first));
        service.removeConnectionsAt(helper.absolutePos(atLimit));
        service.removeConnectionsAt(helper.absolutePos(tooFar));
        placePole(helper, first, false);
        placePole(helper, atLimit, false);

        BlockPos firstWorld = helper.absolutePos(first);
        BlockPos limitWorld = helper.absolutePos(atLimit);
        helper.assertTrue(service.connectionsAt(firstWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(limitWorld)),
                "Electric poles did not auto-connect at sixteen blocks");
        helper.assertTrue(service.removeConnectionsAt(firstWorld) >= 1,
                "Pole fixture did not remove its automatic connection");
        helper.assertTrue(service.connect(firstWorld, limitWorld).result()
                        == LongDistanceElectricityService.ConnectionResult.SUCCESS,
                "Electric pole wire rejected a sixteen-block span");
        placePole(helper, tooFar, false);
        helper.assertTrue(service.connect(firstWorld, helper.absolutePos(tooFar)).result()
                        == LongDistanceElectricityService.ConnectionResult.TOO_FAR,
                "Electric pole wire accepted a seventeen-block span");

        BlockPos connector = new BlockPos(3, 8, 30);
        BlockPos transformer = new BlockPos(5, 8, 30);
        BlockPos pole = new BlockPos(7, 8, 30);
        service.removeConnectionsAt(helper.absolutePos(connector));
        service.removeConnectionsAt(helper.absolutePos(transformer));
        service.removeConnectionsAt(helper.absolutePos(pole));
        placeConnector(helper, connector);
        ElectricPoleBlockEntity mediumPole = placePole(helper, pole, false);
        mediumPole.electricity().applyTierFromPlacementData(VoltageTierIds.MEDIUM);
        helper.assertTrue(mediumPole.electricity().tierId().equals(VoltageTierIds.MEDIUM),
                "Transformer test pole could not bind its medium-voltage tier");
        placePole(helper, transformer, true);
        helper.assertTrue(service.connect(helper.absolutePos(connector), helper.absolutePos(pole)).result()
                        == LongDistanceElectricityService.ConnectionResult.INCOMPATIBLE_PORT,
                "Connector wire directly connected to a three-wire pole port");
        helper.assertTrue(service.connect(helper.absolutePos(connector), helper.absolutePos(transformer)).result()
                        == LongDistanceElectricityService.ConnectionResult.SUCCESS,
                "Transformer pole did not accept the connector wire domain");
        BlockPos transformerWorld = helper.absolutePos(transformer);
        LongDistanceElectricityService.ConnectionResult poleBridge = service.connect(
                transformerWorld,
                helper.absolutePos(pole)
        ).result();
        helper.assertTrue(
                poleBridge == LongDistanceElectricityService.ConnectionResult.SUCCESS
                        || poleBridge == LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                "Transformer pole did not accept the pole wire domain"
        );
        helper.assertTrue(service.connectionsAt(transformerWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(helper.absolutePos(connector))),
                "Transformer pole did not retain its connector edge");
        helper.assertTrue(service.connectionsAt(transformerWorld).stream()
                        .anyMatch(connection -> connection.containsPosition(helper.absolutePos(pole))),
                "Transformer pole did not retain its pole edge");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void repeatedTransformerConnectionPrefersExistingPoleEdgeOverConnectorDistance(GameTestHelper helper) {
        BlockPos first = new BlockPos(2, 8, 2);
        BlockPos second = new BlockPos(14, 8, 2);
        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        BlockPos firstWorld = helper.absolutePos(first);
        BlockPos secondWorld = helper.absolutePos(second);
        service.removeConnectionsAt(firstWorld);
        service.removeConnectionsAt(secondWorld);
        placePole(helper, first, true);
        placePole(helper, second, true);

        LongDistanceElectricityService.ConnectionResult initial = service.connect(firstWorld, secondWorld).result();
        helper.assertTrue(
                initial == LongDistanceElectricityService.ConnectionResult.SUCCESS
                        || initial == LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                "Transformer fixtures could not establish their twelve-block pole edge"
        );
        helper.assertTrue(
                service.connect(firstWorld, secondWorld).result()
                        == LongDistanceElectricityService.ConnectionResult.ALREADY_CONNECTED,
                "Existing pole edge did not take priority over the out-of-range connector port"
        );
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void wireTransferPausesOnUnloadResumesAndCleansOnBreak(GameTestHelper helper) {
        BlockPos first = new BlockPos(1, 4, 2);
        BlockPos second = new BlockPos(9, 4, 2);
        ElectricConnectorBlockEntity source = placeConnector(helper, first);
        ElectricConnectorBlockEntity target = placeConnector(helper, second);
        LongDistanceElectricityService service = LongDistanceElectricityService.get(helper.getLevel());
        helper.assertTrue(service.connect(helper.absolutePos(first), helper.absolutePos(second)).result()
                        == LongDistanceElectricityService.ConnectionResult.SUCCESS,
                "Connector fixture could not create its wire");

        source.electricity().node().setVoltage(120.0D);
        target.electricity().node().setVoltage(0.0D);
        double energyBefore = electricalEnergy(source, target);
        service.tick(helper.getLevel().getGameTime() + 1_000L);
        double energyAfter = electricalEnergy(source, target);
        helper.assertTrue(source.electricity().node().energyJoules() < energyBefore,
                "Long-distance source did not discharge");
        helper.assertTrue(target.electricity().node().energyJoules() > 0.0D,
                "Long-distance target did not receive energy");
        helper.assertTrue(energyAfter <= energyBefore + 1.0E-6D,
                "Long-distance transfer created energy");

        source.electricity().node().setVoltage(120.0D);
        target.electricity().node().setVoltage(0.0D);
        target.setRemoved();
        double pausedBefore = electricalEnergy(source, target);
        service.tick(helper.getLevel().getGameTime() + 1_001L);
        helper.assertTrue(Math.abs(electricalEnergy(source, target) - pausedBefore) < 1.0E-6D,
                "Unloaded long-distance endpoint did not pause its edge");
        helper.assertTrue(service.hasConnections(helper.absolutePos(first)),
                "Runtime endpoint unload deleted the durable edge");

        target.clearRemoved();
        target.onLoad();
        service.tick(helper.getLevel().getGameTime() + 1_002L);
        helper.assertTrue(target.electricity().node().energyJoules() > 0.0D,
                "Reloaded long-distance endpoint did not resume transfer");
        helper.destroyBlock(first);
        helper.assertFalse(service.hasConnections(helper.absolutePos(second)),
                "Breaking an endpoint left a durable wire edge");
        helper.succeed();
    }

    @GameTest(template = ADVANCED_TEMPLATE)
    public static void teslaTowerUsesMediumVoltageAndIsolatedOneToOneTransfer(GameTestHelper helper) {
        BlockPos towerPosition = new BlockPos(20, 5, 20);
        BlockPos receiverPosition = new BlockPos(24, 5, 20);
        helper.setBlock(towerPosition, ModNetworkBlocks.TESLA_TOWER.get());
        TeslaTowerBlockEntity tower = require(helper, towerPosition, TeslaTowerBlockEntity.class);
        WirelessEnergyReceiverBlockEntity receiver = placeReceiver(helper, receiverPosition);

        double[] sourceBefore = new double[1];
        tower.electricity().node().setVoltage(239.0D);
        helper.runAfterDelay(2, () -> {
            helper.assertTrue(receiver.electricity().node().energyJoules() == 0.0D,
                    "Tesla tower transferred below its medium-voltage minimum");

            tower.electricity().node().setVoltage(240.0D);
            sourceBefore[0] = tower.electricity().node().energyJoules();
        });
        helper.runAfterDelay(3, () -> {
            double received = receiver.electricity().node().energyJoules();
            helper.assertTrue(Math.abs(received - 400.0D) < 1.0E-6D,
                    "Tesla receiver did not receive exactly 400 J in one tick; received " + received + " J");
            helper.assertTrue(
                    Math.abs(tower.electricity().node().energyJoules() - (sourceBefore[0] - 400.0D)) < 1.0E-6D,
                    "Tesla transfer did not conserve its 1 J to 1 J boundary"
            );
            helper.succeed();
        });
    }

    @GameTest(template = ADVANCED_TEMPLATE)
    public static void onlyLowVoltageConnectorExportsForgeEnergyOutward(GameTestHelper helper) {
        BlockPos connectorPosition = new BlockPos(20, 5, 20);
        BlockPos receiverPosition = connectorPosition.north();
        helper.setBlock(
                receiverPosition,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.RF_HEATER).get().defaultBlockState()
        );
        helper.setBlock(
                connectorPosition,
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
        );
        ElectricConnectorBlockEntity connector = require(
                helper, connectorPosition, ElectricConnectorBlockEntity.class
        );
        SingleBlockMachineBlockEntity receiver = require(
                helper, receiverPosition, SingleBlockMachineBlockEntity.class
        );

        connector.electricity().node().setVoltage(120.0D);
        double sourceBefore = connector.electricity().node().energyJoules();
        connector.serverTick();
        helper.assertTrue(receiver.forgeEnergy().getEnergyStored() == 400,
                "Low-voltage connector did not export 400 FE through its outward face");
        helper.assertTrue(Math.abs(sourceBefore - connector.electricity().node().energyJoules() - 400.0D) < 1.0E-6D,
                "Low-voltage connector violated the 1 J = 1 FE boundary");

        receiver.forgeEnergy().setEnergyStored(0);
        connector.electricity().applyTierFromPlacementData(VoltageTierIds.MEDIUM);
        helper.assertTrue(connector.electricity().tierId().equals(VoltageTierIds.MEDIUM),
                "Connector rejected its medium-voltage test tier");
        connector.electricity().node().setVoltage(480.0D);
        connector.serverTick();
        helper.assertTrue(receiver.forgeEnergy().getEnergyStored() == 0,
                "Medium-voltage connector exposed a direct Forge Energy output");
        helper.succeed();
    }

    @GameTest(template = ADVANCED_TEMPLATE)
    public static void wirelessReceiverExportsForgeEnergyOnlyToFeOnlyTarget(GameTestHelper helper) {
        BlockPos receiverPosition = new BlockPos(20, 5, 20);
        BlockPos targetPosition = receiverPosition.north();
        helper.setBlock(
                targetPosition,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.RF_HEATER).get().defaultBlockState()
        );
        helper.setBlock(
                receiverPosition,
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
        );
        WirelessEnergyReceiverBlockEntity receiver = require(
                helper, receiverPosition, WirelessEnergyReceiverBlockEntity.class
        );
        SingleBlockMachineBlockEntity target = require(
                helper, targetPosition, SingleBlockMachineBlockEntity.class
        );

        receiver.electricity().node().setVoltage(120.0D);
        double sourceBefore = receiver.electricity().node().energyJoules();
        receiver.serverTick();

        helper.assertTrue(target.forgeEnergy().getEnergyStored() == 400,
                "Wireless receiver did not export 400 FE to an FE-only target");
        helper.assertTrue(
                Math.abs(sourceBefore - receiver.electricity().node().energyJoules() - 400.0D) < 1.0E-6D,
                "Wireless receiver violated its transactional 1 J = 1 FE boundary"
        );
        helper.succeed();
    }

    @GameTest(template = ADVANCED_TEMPLATE)
    public static void connectorAndWirelessReceiverPreferNativeJForHybridTargets(GameTestHelper helper) {
        BlockPos connectorPosition = new BlockPos(20, 5, 20);
        BlockPos connectorTargetPosition = connectorPosition.north();
        SingleBlockMachineBlockEntity connectorTarget = placeHybridTransformer(
                helper, connectorTargetPosition
        );
        helper.setBlock(
                connectorPosition,
                ModNetworkBlocks.ELECTRIC_CONNECTOR.get().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
        );
        ElectricConnectorBlockEntity connector = require(
                helper, connectorPosition, ElectricConnectorBlockEntity.class
        );

        BlockPos wirelessPosition = new BlockPos(30, 5, 20);
        BlockPos wirelessTargetPosition = wirelessPosition.north();
        SingleBlockMachineBlockEntity wirelessTarget = placeHybridTransformer(
                helper, wirelessTargetPosition
        );
        helper.setBlock(
                wirelessPosition,
                ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get().defaultBlockState()
                        .setValue(BlockStateProperties.FACING, Direction.NORTH)
        );
        WirelessEnergyReceiverBlockEntity wireless = require(
                helper, wirelessPosition, WirelessEnergyReceiverBlockEntity.class
        );

        for (SingleBlockMachineBlockEntity target : List.of(connectorTarget, wirelessTarget)) {
            helper.assertTrue(target.supportsNetworkConnection(NetworkDomain.ELECTRICITY, Direction.SOUTH),
                    "Hybrid target did not expose its native electrical port");
            helper.assertTrue(target.getCapability(ForgeCapabilities.ENERGY, Direction.SOUTH).isPresent(),
                    "Hybrid target did not expose its FE input for the priority regression fixture");
            target.electricity().node().setEnergyJoules(0.0D);
        }

        connector.electricity().node().setVoltage(120.0D);
        wireless.electricity().node().setVoltage(120.0D);
        double connectorBefore = connector.electricity().node().energyJoules();
        double wirelessBefore = wireless.electricity().node().energyJoules();

        connector.serverTick();
        wireless.serverTick();
        helper.assertTrue(connectorTarget.electricity().node().energyJoules() == 0.0D,
                "Connector selected FE before the native J path");
        helper.assertTrue(wirelessTarget.electricity().node().energyJoules() == 0.0D,
                "Wireless receiver selected FE before the native J path");
        helper.assertTrue(connector.electricity().node().energyJoules() == connectorBefore,
                "Connector consumed J while only probing the hybrid target");
        helper.assertTrue(wireless.electricity().node().energyJoules() == wirelessBefore,
                "Wireless receiver consumed J while only probing the hybrid target");

        helper.runAfterDelay(2, () -> {
            assertNativeTransferConserved(helper, connector.electricity().node().energyJoules(),
                    connectorTarget.electricity().node().energyJoules(), connectorBefore, "Connector");
            assertNativeTransferConserved(helper, wireless.electricity().node().energyJoules(),
                    wirelessTarget.electricity().node().energyJoules(), wirelessBefore, "Wireless receiver");
            helper.succeed();
        });
    }

    @GameTest(template = ADVANCED_TEMPLATE, timeoutTicks = 40)
    public static void windTurbineStopsForBlockedRotorAndProducesBoundedPowerWhenOpen(GameTestHelper helper) {
        BlockPos templateOrigin = helper.absolutePos(BlockPos.ZERO);
        BlockPos turbinePosition = new BlockPos(20, 128 - templateOrigin.getY(), 20);
        Direction facing = Direction.EAST;
        try {
            clearWindTurbineTestArea(helper, turbinePosition, facing);
            helper.setBlock(
                    turbinePosition,
                    ModNetworkBlocks.WIND_TURBINE.get().defaultBlockState().setValue(WindTurbineBlock.FACING, facing)
            );
            WindTurbineBlockEntity turbine = require(helper, turbinePosition, WindTurbineBlockEntity.class);
            Direction horizontal = facing.getClockWise();
            BlockPos planeCenter = turbinePosition.relative(facing);
            BlockPos obstruction = planeCenter.relative(horizontal, -5).below(3);
            helper.setBlock(obstruction, Blocks.STONE);

            turbine.wind().load(new CompoundTag());
            turbine.serverTick();
            helper.assertFalse(turbine.wind().operational(), "Blocked wind-turbine rotor remained operational");
            helper.assertTrue(turbine.wind().productionJoulesPerTick() == 0.0D,
                    "Blocked wind turbine generated energy");

            helper.setBlock(obstruction, Blocks.AIR);
            CompoundTag wind = new CompoundTag();
            wind.putDouble("current_wind", 1.0D);
            wind.putDouble("target_wind", 1.0D);
            turbine.wind().load(wind);
            turbine.serverTick();

            helper.assertTrue(turbine.wind().operational(), "Open wind-turbine rotor did not become operational");
            helper.assertTrue(turbine.wind().openSpace() >= 0.0D && turbine.wind().openSpace() <= 1.0D,
                    "Wind-turbine open-space factor escaped its bounded range");
            helper.assertTrue(turbine.wind().productionJoulesPerTick() > 0.0D,
                    "Open wind turbine did not generate energy");
            helper.assertTrue(turbine.wind().productionJoulesPerTick() <= 200.0D,
                    "Wind turbine exceeded its 200 J/t rating");
        } finally {
            clearWindTurbineTestArea(helper, turbinePosition, facing);
        }
        helper.succeed();
    }

    private static void clearWindTurbineTestArea(
            GameTestHelper helper,
            BlockPos turbinePosition,
            Direction facing
    ) {
        helper.setBlock(turbinePosition, Blocks.AIR);
        Direction horizontal = facing.getClockWise();
        BlockPos planeCenter = turbinePosition.relative(facing);
        for (int horizontalOffset = -5; horizontalOffset <= 5; horizontalOffset++) {
            for (int verticalOffset = -5; verticalOffset <= 5; verticalOffset++) {
                BlockPos bladePosition = planeCenter
                        .relative(horizontal, horizontalOffset)
                        .above(verticalOffset);
                for (int depth = 0; depth <= 16; depth++) {
                    helper.setBlock(bladePosition.relative(facing, depth), Blocks.AIR);
                }
            }
        }
    }

    private static UseOnContext useContext(GameTestHelper helper, Player player, BlockPos position) {
        BlockPos worldPosition = helper.absolutePos(position);
        return new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(worldPosition), Direction.UP, worldPosition, false)
        );
    }

    private static BlockHitResult connectorPlacementHit(GameTestHelper helper, BlockPos support) {
        BlockPos worldSupport = helper.absolutePos(support);
        return new BlockHitResult(
                Vec3.atCenterOf(worldSupport).add(0.0D, 0.0D, -0.5D),
                Direction.NORTH,
                worldSupport,
                false
        );
    }

    private static BlockHitResult polePlacementHit(GameTestHelper helper, BlockPos support) {
        BlockPos worldSupport = helper.absolutePos(support);
        return new BlockHitResult(
                Vec3.atCenterOf(worldSupport).add(0.0D, 0.5D, 0.0D),
                Direction.UP,
                worldSupport,
                false
        );
    }

    private static ElectricConnectorBlockEntity placeConnector(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position.relative(Direction.SOUTH), Blocks.STONE);
        helper.setBlock(position, ModNetworkBlocks.ELECTRIC_CONNECTOR.get());
        return require(helper, position, ElectricConnectorBlockEntity.class);
    }

    private static ElectricPoleBlockEntity placePole(GameTestHelper helper, BlockPos position, boolean transformer) {
        var block = transformer
                ? ModNetworkBlocks.ELECTRIC_POLE_TRANSFORMER.get()
                : ModNetworkBlocks.ELECTRIC_POLE.get();
        helper.setBlock(
                position,
                block.defaultBlockState().setValue(ElectricPoleBlock.SEGMENT, PoleSegment.BASE)
        );
        return require(helper, position, ElectricPoleBlockEntity.class);
    }

    private static void clearPoleVolume(GameTestHelper helper, BlockPos bottom) {
        for (int height = 0; height < 5; height++) {
            helper.setBlock(bottom.above(height), Blocks.AIR);
        }
    }

    private static WirelessEnergyReceiverBlockEntity placeReceiver(GameTestHelper helper, BlockPos position) {
        helper.setBlock(position.relative(Direction.SOUTH), Blocks.STONE);
        helper.setBlock(position, ModNetworkBlocks.WIRELESS_ENERGY_RECEIVER.get());
        return require(helper, position, WirelessEnergyReceiverBlockEntity.class);
    }

    private static SingleBlockMachineBlockEntity placeHybridTransformer(
            GameTestHelper helper,
            BlockPos position
    ) {
        helper.setBlock(
                position,
                ModMachineBlocks.machine(SingleBlockMachineDefinition.RF_TRANSFORMER).get().defaultBlockState()
        );
        return require(helper, position, SingleBlockMachineBlockEntity.class);
    }

    private static void assertNativeTransferConserved(
            GameTestHelper helper,
            double sourceJoules,
            double targetJoules,
            double beforeJoules,
            String label
    ) {
        helper.assertTrue(targetJoules > 0.0D, label + " did not transmit native J to the hybrid target");
        helper.assertTrue(sourceJoules < beforeJoules, label + " native source did not discharge");
        helper.assertTrue(sourceJoules + targetJoules <= beforeJoules + 1.0E-6D,
                label + " native transfer created energy");
    }

    private static double electricalEnergy(
            ElectricConnectorBlockEntity first,
            ElectricConnectorBlockEntity second
    ) {
        return first.electricity().node().energyJoules() + second.electricity().node().energyJoules();
    }

    private static void assertBlockId(
            GameTestHelper helper,
            String expected,
            net.minecraft.world.level.block.Block block
    ) {
        helper.assertTrue(expected.equals(ForgeRegistries.BLOCKS.getKey(block).getPath()),
                "Block has the wrong registry id: " + expected);
    }

    private static void assertTieredRecipe(
            GameTestHelper helper,
            String recipePath,
            net.minecraft.world.item.Item expectedItem,
            ResourceLocation expectedTier
    ) {
        ItemStack result = helper.getLevel().getRecipeManager()
                .byKey(Magneticraft.id(recipePath))
                .orElseThrow(() -> new AssertionError("Missing tiered recipe " + recipePath))
                .getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(expectedItem), "Tiered recipe returned the wrong item: " + recipePath);
        helper.assertTrue(
                TieredElectricalItemData.read(result)
                        .map(TieredElectricalItemData::tierId)
                        .filter(expectedTier::equals)
                        .isPresent(),
                "Tiered recipe result lost its electrical identity: " + recipePath
        );
    }

    private static void assertTransformerRecipe(
            GameTestHelper helper,
            String recipePath,
            net.minecraft.world.item.Item expectedItem,
            ResourceLocation expectedTier,
            ResourceLocation expectedProfile
    ) {
        ItemStack result = helper.getLevel().getRecipeManager()
                .byKey(Magneticraft.id(recipePath))
                .orElseThrow(() -> new AssertionError("Missing transformer recipe " + recipePath))
                .getResultItem(helper.getLevel().registryAccess());
        helper.assertTrue(result.is(expectedItem), "Transformer recipe returned the wrong item: " + recipePath);
        helper.assertTrue(
                TieredElectricalItemData.read(result)
                        .filter(data -> data.tierId().equals(expectedTier))
                        .flatMap(TieredElectricalItemData::transformerProfileId)
                        .filter(expectedProfile::equals)
                        .isPresent(),
                "Transformer recipe result lost its complete electrical identity: " + recipePath
        );
    }

    private static <T extends BlockEntity> T require(
            GameTestHelper helper,
            BlockPos position,
            Class<T> type,
            Runnable placement
    ) {
        placement.run();
        return require(helper, position, type);
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos position, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!type.isInstance(blockEntity)) {
            throw new AssertionError("Expected " + type.getSimpleName() + " at " + position + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }
}
