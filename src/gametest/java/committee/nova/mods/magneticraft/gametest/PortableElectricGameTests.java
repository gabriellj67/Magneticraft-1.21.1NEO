package committee.nova.mods.magneticraft.gametest;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.item.DiagnosticInteractionEvents;
import committee.nova.mods.magneticraft.content.item.ElectricChainsawItem;
import committee.nova.mods.magneticraft.content.item.ElectricDrillItem;
import committee.nova.mods.magneticraft.content.item.ElectricPistonItem;
import committee.nova.mods.magneticraft.content.item.PortableEnergyItem;
import committee.nova.mods.magneticraft.content.machine.battery.BatteryBlockEntity;
import committee.nova.mods.magneticraft.content.network.electric.ElectricCableBlockEntity;
import committee.nova.mods.magneticraft.content.network.heat.HeatPipeBlockEntity;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModCreativeTabs;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.diagnostic.ElectricalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.diagnostic.ThermalDiagnosticSource;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkManager;
import committee.nova.mods.magneticraft.system.network.runtime.PhysicalNetworkService;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.energy.IEnergyStorage;
import net.minecraftforge.event.entity.player.PlayerInteractEvent;
import net.minecraftforge.eventbus.api.Event;
import net.minecraftforge.gametest.GameTestHolder;
import net.minecraftforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime contracts for restored portable electrical equipment and diagnostics.
 */
@GameTestHolder(Magneticraft.MOD_ID)
@PrefixGameTestTemplate(false)
public final class PortableElectricGameTests {
    private static final String TEMPLATE = "base_content";
    private static final BlockPos FIRST = new BlockPos(0, 1, 1);
    private static final BlockPos SECOND = new BlockPos(1, 1, 1);
    private static final BlockPos THIRD = new BlockPos(2, 1, 1);

    private PortableElectricGameTests() {
    }

    @GameTest(template = TEMPLATE)
    public static void portableEnergyTransferAndSchemaResetRemainStable(GameTestHelper helper) {
        ItemStack medium = new ItemStack(ModMachineItems.MEDIUM_BATTERY.get());
        IEnergyStorage mediumEnergy = energy(medium);
        helper.assertTrue(mediumEnergy.getMaxEnergyStored() == 2_500_000, "Medium battery has the wrong capacity");
        helper.assertTrue(mediumEnergy.receiveEnergy(10_000, true) == 10_000,
                "Portable energy simulation retained an external transfer limit");
        helper.assertTrue(mediumEnergy.getEnergyStored() == 0, "Energy simulation mutated the battery");
        helper.assertTrue(mediumEnergy.receiveEnergy(10_000, false) == 10_000,
                "Portable energy insertion retained an external transfer limit");
        helper.assertTrue(mediumEnergy.extractEnergy(10_000, true) == 10_000,
                "Portable energy extraction simulation retained an external transfer limit");
        helper.assertTrue(mediumEnergy.getEnergyStored() == 10_000, "Energy extraction simulation mutated the battery");
        helper.assertTrue(mediumEnergy.extractEnergy(10_000, false) == 10_000,
                "Portable energy extraction retained an external transfer limit");
        helper.assertTrue(mediumEnergy.getEnergyStored() == 0, "Energy extraction removed the wrong amount");
        mediumEnergy.receiveEnergy(500, false);

        CompoundTag saved = medium.save(new CompoundTag());
        CompoundTag energyData = findEnergyData(saved);
        helper.assertTrue(energyData != null, "Serialized ItemStack lost its energy capability data");
        helper.assertTrue(energyData.getInt("schema_version") == 1, "Portable energy schema version is not 1");
        helper.assertTrue(energyData.getInt("energy") == 500, "Portable energy used an unstable NBT key");

        CompoundTag legacy = saved.copy();
        CompoundTag legacyEnergy = findEnergyData(legacy);
        helper.assertTrue(legacyEnergy != null, "Could not locate legacy energy payload");
        legacyEnergy.remove("schema_version");
        legacyEnergy.putInt("energy", 12_345);
        ItemStack restoredLegacy = ItemStack.of(legacy);
        helper.assertTrue(energy(restoredLegacy).getEnergyStored() == 0,
                "Versionless portable energy data survived the 0.2.0 reset");

        ItemStack low = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        charge(low, 12_345);
        CompoundTag legacyLow = low.save(new CompoundTag());
        CompoundTag legacyLowEnergy = findEnergyData(legacyLow);
        helper.assertTrue(legacyLowEnergy != null, "Could not locate low-battery energy payload");
        legacyLowEnergy.remove("schema_version");
        helper.assertTrue(energy(ItemStack.of(legacyLow)).getEnergyStored() == 0,
                "Low battery retained its versionless energy data");

        CompoundTag oversized = saved.copy();
        findEnergyData(oversized).putInt("energy", Integer.MAX_VALUE);
        helper.assertTrue(energy(ItemStack.of(oversized)).getEnergyStored() == 2_500_000,
                "Oversized portable energy was not clamped");
        CompoundTag malformed = saved.copy();
        findEnergyData(malformed).putString("energy", "invalid");
        helper.assertTrue(energy(ItemStack.of(malformed)).getEnergyStored() == 0,
                "Malformed portable energy did not fall back safely");
        CompoundTag negative = saved.copy();
        findEnergyData(negative).putInt("energy", -1);
        helper.assertTrue(energy(ItemStack.of(negative)).getEnergyStored() == 0,
                "Negative portable energy was not clamped");
        CompoundTag future = saved.copy();
        CompoundTag futureEnergy = findEnergyData(future);
        futureEnergy.putInt("schema_version", 99);
        futureEnergy.putInt("energy", 456);
        helper.assertTrue(energy(ItemStack.of(future)).getEnergyStored() == 0,
                "Unknown portable energy schema did not reset safely");

        ItemStack copied = medium.copy();
        helper.assertTrue(energy(copied).getEnergyStored() == 500, "ItemStack copy lost portable energy");
        CompoundTag copiedData = findEnergyData(copied.save(new CompoundTag()));
        helper.assertTrue(copiedData != null && copiedData.getInt("schema_version") == 1,
                "ItemStack copy lost the portable energy schema version");

        var tooltip = new ArrayList<net.minecraft.network.chat.Component>();
        medium.getItem().appendHoverText(medium, null, tooltip, TooltipFlag.NORMAL);
        helper.assertFalse(tooltip.isEmpty(), "Portable energy tooltip was not produced");
        helper.assertTrue(tooltip.get(0).getString().contains("500")
                        && tooltip.get(0).getString().contains("2500000"),
                "Portable energy tooltip omitted the stored energy or capacity");
        helper.assertTrue(medium.getItem().isBarVisible(medium), "Partially charged battery hid its energy bar");
        ItemStack emptyLow = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        helper.assertFalse(emptyLow.getItem().isBarVisible(emptyLow), "Empty battery displayed an energy bar");
        ItemStack fullLow = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        energy(fullLow).receiveEnergy(((PortableEnergyItem) fullLow.getItem()).capacity(), false);
        helper.assertTrue(fullLow.getItem().isBarVisible(fullLow), "Fully charged battery hid its energy bar");
        assertPortableCreativeVariants(helper);

        ItemStack drill = new ItemStack(ModMachineItems.ELECTRIC_DRILL.get());
        charge(drill, 2_000);
        helper.assertTrue(((ElectricDrillItem) drill.getItem()).consumeEnergy(drill, 2_000),
                "Internal action cost could not exceed the external transfer limit atomically");
        helper.assertTrue(energy(drill).getEnergyStored() == 0, "Atomic tool cost removed the wrong amount");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void bothPortableBatteriesWorkInChargeAndDischargeSlots(GameTestHelper helper) {
        helper.setBlock(SECOND, ModMachineBlocks.BATTERY.get());
        BatteryBlockEntity battery = require(helper, SECOND, BatteryBlockEntity.class);
        battery.electricity().node().setVoltage(90.0D);

        ItemStack medium = new ItemStack(ModMachineItems.MEDIUM_BATTERY.get());
        battery.energy().setEnergyStored(1_000);
        battery.inventory().setStackInSlot(0, medium);
        tickBattery(helper, SECOND, battery);
        helper.assertTrue(energy(medium).getEnergyStored() == BatteryBlockEntity.ITEM_TRANSFER_RATE,
                "Medium battery did not charge in slot 0");
        helper.assertTrue(battery.energy().getEnergyStored() == 500,
                "Machine battery removed the wrong amount while charging a medium battery");

        battery.inventory().setStackInSlot(0, ItemStack.EMPTY);
        battery.inventory().setStackInSlot(1, medium);
        battery.energy().setEnergyStored(0);
        tickBattery(helper, SECOND, battery);
        helper.assertTrue(energy(medium).getEnergyStored() == 0,
                "Medium battery did not discharge in slot 1");
        helper.assertTrue(battery.energy().getEnergyStored() == BatteryBlockEntity.ITEM_TRANSFER_RATE,
                "Machine battery received the wrong amount from a medium battery");

        ItemStack low = new ItemStack(ModMachineItems.LOW_BATTERY.get());
        battery.inventory().setStackInSlot(1, ItemStack.EMPTY);
        battery.inventory().setStackInSlot(0, low);
        battery.energy().setEnergyStored(1_000);
        tickBattery(helper, SECOND, battery);
        helper.assertTrue(energy(low).getEnergyStored() == BatteryBlockEntity.ITEM_TRANSFER_RATE,
                "Low battery did not charge in slot 0");

        battery.inventory().setStackInSlot(0, ItemStack.EMPTY);
        battery.inventory().setStackInSlot(1, low);
        battery.energy().setEnergyStored(0);
        tickBattery(helper, SECOND, battery);
        helper.assertTrue(energy(low).getEnergyStored() == 0, "Low battery did not discharge in slot 1");
        helper.assertTrue(battery.energy().getEnergyStored() == BatteryBlockEntity.ITEM_TRANSFER_RATE,
                "Machine battery received the wrong amount from a low battery");
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void electricToolsRetainLegacyMiningAndAttackBehavior(GameTestHelper helper) {
        ElectricDrillItem drillItem = (ElectricDrillItem) ModMachineItems.ELECTRIC_DRILL.get();
        ItemStack drill = new ItemStack(drillItem);
        charge(drill, 5_000);
        helper.assertTrue(drillItem.getDestroySpeed(drill, Blocks.STONE.defaultBlockState()) == 44.0F,
                "Electric drill lost its pickaxe speed");
        helper.assertTrue(drillItem.getDestroySpeed(drill, Blocks.DIRT.defaultBlockState()) == 15.0F,
                "Electric drill lost its shovel speed");
        helper.assertTrue(drillItem.getDestroySpeed(drill, Blocks.CAKE.defaultBlockState()) == 15.0F,
                "Electric drill lost its legacy cake speed");
        helper.assertTrue(drillItem.isCorrectToolForDrops(drill, Blocks.OBSIDIAN.defaultBlockState()),
                "Electric drill lost diamond-tier harvesting");
        helper.assertTrue(drillItem.isCorrectToolForDrops(drill, Blocks.CAKE.defaultBlockState()),
                "Powered electric drill could not harvest an arbitrary block");

        Player miner = helper.makeMockSurvivalPlayer();
        helper.setBlock(FIRST, Blocks.STONE);
        BlockPos absolute = helper.absolutePos(FIRST);
        var minedState = helper.getLevel().getBlockState(absolute);
        helper.assertTrue(helper.getLevel().destroyBlock(absolute, false, miner), "Test block was not actually destroyed");
        drillItem.mineBlock(drill, helper.getLevel(), minedState, absolute, miner);
        helper.assertTrue(energy(drill).getEnergyStored() == 4_000, "Successful block break consumed the wrong energy");

        miner.getAbilities().instabuild = true;
        helper.setBlock(SECOND, Blocks.STONE);
        BlockPos creativeBlock = helper.absolutePos(SECOND);
        var creativeState = helper.getLevel().getBlockState(creativeBlock);
        helper.assertTrue(helper.getLevel().destroyBlock(creativeBlock, false, miner),
                "Creative test block was not actually destroyed");
        drillItem.mineBlock(drill, helper.getLevel(), creativeState, creativeBlock, miner);
        helper.assertTrue(energy(drill).getEnergyStored() == 3_000,
                "Creative block break did not consume electric-tool energy");
        miner.getAbilities().instabuild = false;

        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 1);
        float health = target.getHealth();
        drillItem.hurtEnemy(drill, target, miner);
        helper.assertTrue(Math.abs(target.getHealth() - (health - 5.0F)) < 0.001F,
                "Electric drill dealt the wrong bonus damage");
        helper.assertTrue(energy(drill).getEnergyStored() == 1_000, "Electric drill attack consumed the wrong energy");
        drillItem.consumeEnergy(drill, 1_000);
        helper.assertFalse(drillItem.isCorrectToolForDrops(drill, Blocks.CAKE.defaultBlockState()),
                "Empty electric drill retained arbitrary-block harvesting");
        helper.assertTrue(drillItem.getDestroySpeed(drill, Blocks.STONE.defaultBlockState()) == 1.0F,
                "Empty electric drill did not fall back to hand speed");
        health = target.getHealth();
        helper.assertFalse(drillItem.hurtEnemy(drill, target, miner), "Empty electric drill reported a powered attack");
        helper.assertTrue(target.getHealth() == health, "Empty electric drill dealt bonus damage");

        ElectricChainsawItem chainsawItem = (ElectricChainsawItem) ModMachineItems.ELECTRIC_CHAINSAW.get();
        ItemStack chainsaw = new ItemStack(chainsawItem);
        charge(chainsaw, 3_000);
        helper.assertTrue(chainsawItem.getDestroySpeed(chainsaw, Blocks.OAK_LOG.defaultBlockState()) == 10.0F,
                "Electric chainsaw lost its wood speed");
        helper.assertTrue(chainsawItem.getDestroySpeed(chainsaw, Blocks.OAK_LEAVES.defaultBlockState()) == 10.0F,
                "Electric chainsaw lost its leaves speed");
        helper.assertTrue(chainsawItem.getDestroySpeed(chainsaw, Blocks.COBWEB.defaultBlockState()) == 10.0F,
                "Electric chainsaw lost its cobweb speed");
        Zombie sawTarget = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 1, 1, 2);
        health = sawTarget.getHealth();
        chainsawItem.hurtEnemy(chainsaw, sawTarget, miner);
        helper.assertTrue(Math.abs(sawTarget.getHealth() - (health - 14.0F)) < 0.001F,
                "Electric chainsaw dealt the wrong bonus damage");
        helper.assertTrue(energy(chainsaw).getEnergyStored() == 1_000,
                "Electric chainsaw attack consumed the wrong energy");
        target.discard();
        sawTarget.discard();
        miner.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE)
    public static void electricPistonPushesEntitiesAndUserOnlyAfterAtomicPayment(GameTestHelper helper) {
        ElectricPistonItem pistonItem = (ElectricPistonItem) ModMachineItems.ELECTRIC_PISTON.get();
        ItemStack piston = new ItemStack(pistonItem);
        charge(piston, 10_000);
        Player player = helper.makeMockSurvivalPlayer();
        Vec3 playerPosition = Vec3.atCenterOf(helper.absolutePos(FIRST));
        player.setPos(playerPosition.x, playerPosition.y, playerPosition.z);
        Zombie target = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, 2, 1, 1);
        float health = target.getHealth();
        helper.assertTrue(pistonItem.hurtEnemy(piston, target, player),
                "Electric piston did not report a powered attack");
        helper.assertTrue(Math.abs(target.getHealth() - (health - 2.0F)) < 0.001F,
                "Electric piston dealt the wrong bonus damage");
        helper.assertTrue(energy(piston).getEnergyStored() == 8_000,
                "Electric piston attack consumed the wrong energy");
        Vec3 expectedDirection = target.position().subtract(player.position()).normalize();
        Vec3 velocityBeforePush = target.getDeltaMovement();

        InteractionResult entityResult = pistonItem.interactLivingEntity(
                piston,
                player,
                target,
                InteractionHand.MAIN_HAND
        );
        helper.assertTrue(entityResult.consumesAction(), "Electric piston did not handle an entity target");
        Vec3 appliedImpulse = target.getDeltaMovement().subtract(velocityBeforePush);
        helper.assertTrue(Math.abs(appliedImpulse.dot(expectedDirection) - 1.75D) < 1.0E-6D,
                "Electric piston pushed the entity in the wrong direction");
        helper.assertTrue(energy(piston).getEnergyStored() == 4_000, "Entity push consumed the wrong energy");

        player.setItemInHand(InteractionHand.MAIN_HAND, piston);
        helper.setBlock(THIRD, Blocks.STONE);
        Vec3 targetCenter = Vec3.atCenterOf(helper.absolutePos(THIRD));
        player.lookAt(EntityAnchorArgument.Anchor.FEET, targetCenter);
        player.setDeltaMovement(Vec3.ZERO);
        player.fallDistance = 8.0F;
        var useResult = pistonItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
        helper.assertTrue(useResult.getResult().consumesAction(), "Electric piston did not handle a six-block ray hit");
        helper.assertTrue(Math.abs(player.getDeltaMovement().dot(player.getLookAngle()) + 1.75D) < 1.0E-6D,
                "Electric piston did not recoil opposite the look direction");
        helper.assertTrue(player.fallDistance == 0.0F, "Electric piston did not reset fall distance");
        helper.assertTrue(energy(piston).getEnergyStored() == 0, "Self push consumed the wrong energy");

        player.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(pistonItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        .getResult() == InteractionResult.PASS,
                "Empty electric piston did not return PASS for a block ray hit");
        helper.assertTrue(player.getDeltaMovement().equals(Vec3.ZERO),
                "Empty electric piston changed player velocity");

        Vec3 stopped = target.getDeltaMovement();
        helper.assertTrue(
                pistonItem.interactLivingEntity(piston, player, target, InteractionHand.MAIN_HAND) == InteractionResult.PASS,
                "Empty electric piston accepted another entity push"
        );
        helper.assertTrue(target.getDeltaMovement().equals(stopped), "Empty electric piston changed target velocity");

        ItemStack sneakingPiston = new ItemStack(pistonItem);
        charge(sneakingPiston, 4_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, sneakingPiston);
        player.setShiftKeyDown(true);
        player.lookAt(EntityAnchorArgument.Anchor.FEET, targetCenter);
        player.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(pistonItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        .getResult().consumesAction(),
                "Sneaking electric piston did not handle a block ray hit");
        helper.assertTrue(Math.abs(player.getDeltaMovement().dot(player.getLookAngle()) + 0.25D) < 1.0E-6D,
                "Sneaking electric piston used the wrong recoil force");
        helper.assertTrue(energy(sneakingPiston).getEnergyStored() == 0,
                "Sneaking self push consumed the wrong energy");

        ItemStack missPiston = new ItemStack(pistonItem);
        charge(missPiston, 4_000);
        player.setItemInHand(InteractionHand.MAIN_HAND, missPiston);
        player.setShiftKeyDown(false);
        player.lookAt(EntityAnchorArgument.Anchor.FEET, player.position().add(0.0D, 10.0D, 0.0D));
        player.setDeltaMovement(Vec3.ZERO);
        helper.assertTrue(pistonItem.use(helper.getLevel(), player, InteractionHand.MAIN_HAND)
                        .getResult() == InteractionResult.PASS,
                "Electric piston consumed a ray miss");
        helper.assertTrue(energy(missPiston).getEnergyStored() == 4_000,
                "Electric piston charged energy for a ray miss");
        target.discard();
        player.discard();
        helper.succeed();
    }

    @GameTest(template = TEMPLATE, timeoutTicks = 20)
    public static void diagnosticReadingsExposeAbsoluteCurrentAndCelsiusInstrumentUse(GameTestHelper helper) {
        helper.setBlock(FIRST, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(SECOND, ModNetworkBlocks.ELECTRIC_CABLE.get());
        helper.setBlock(THIRD, ModNetworkBlocks.HEAT_PIPE.get());

        helper.runAfterDelay(2, () -> {
            ElectricCableBlockEntity source = require(helper, FIRST, ElectricCableBlockEntity.class);
            ElectricCableBlockEntity target = require(helper, SECOND, ElectricCableBlockEntity.class);
            HeatPipeBlockEntity heat = require(helper, THIRD, HeatPipeBlockEntity.class);
            source.electricity().node().setVoltage(100.0D);
            target.electricity().node().setVoltage(0.0D);
            heat.heat().node().setTemperature(650.0D);

            PhysicalNetworkManager manager = PhysicalNetworkService.manager(helper.getLevel());
            manager.tick(helper.getLevel().getGameTime() + 1L);
            ElectricalDiagnosticSource.ElectricalReading sourceReading = source.electricalReading(Direction.UP)
                    .orElseThrow(AssertionError::new);
            ElectricalDiagnosticSource.ElectricalReading targetReading = target.electricalReading(Direction.UP)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(sourceReading.currentAmps() > 0.0D, "Electrical source current was not absolute throughput");
            helper.assertTrue(targetReading.currentAmps() > 0.0D, "Electrical target current was not positive");
            helper.assertTrue(Math.abs(sourceReading.currentAmps() - targetReading.currentAmps()) < 1.0E-9D,
                    "Electrical diagnostic throughput was not conserved");
            helper.assertTrue(Math.abs(sourceReading.powerWatts()
                    - source.electricity().node().lastCompletedTickJoules() * 20.0D) < 1.0E-9D,
                    "Electrical diagnostic power did not match J/t x 20");
            source.electricity().node().setVoltage(50.0D);
            target.electricity().node().setVoltage(50.0D);
            manager.tick(helper.getLevel().getGameTime() + 2L);
            helper.assertTrue(source.electricalReading(Direction.UP).orElseThrow().currentAmps() == 0.0D,
                    "Electrical source retained stale current telemetry");
            helper.assertTrue(target.electricalReading(Direction.UP).orElseThrow().currentAmps() == 0.0D,
                    "Electrical target retained stale current telemetry");
            ThermalDiagnosticSource.ThermalReading heatReading = heat.thermalReading(Direction.UP)
                    .orElseThrow(AssertionError::new);
            helper.assertTrue(Math.abs(heatReading.temperatureKelvin() - 650.0D) < 1.0E-6D,
                    "Thermometer diagnostic lost Kelvin temperature");

            Player player = helper.makeMockSurvivalPlayer();
            ItemStack modeMeter = new ItemStack(ModMachineItems.VOLTMETER.get());
            player.setItemInHand(InteractionHand.MAIN_HAND, modeMeter);
            player.setShiftKeyDown(true);
            modeMeter.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue("network_summary".equals(modeMeter.getOrCreateTag().getString("diagnostic_mode")),
                    "Voltmeter did not cycle from point mode to network summary");
            modeMeter.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue("fault_locator".equals(modeMeter.getOrCreateTag().getString("diagnostic_mode")),
                    "Voltmeter did not cycle from network summary to fault locator");
            modeMeter.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue("point".equals(modeMeter.getOrCreateTag().getString("diagnostic_mode")),
                    "Voltmeter did not wrap its mode payload back to point mode");
            player.setShiftKeyDown(false);
            assertInstrumentUse(helper, player, ModMachineItems.VOLTMETER.get(), FIRST);
            assertInstrumentUse(helper, player, ModMachineItems.THERMOMETER.get(), THIRD);
            source.electricity().setSideEnabled(Direction.UP, false);
            helper.assertTrue(instrumentUse(helper, player, ModMachineItems.VOLTMETER.get(), FIRST) == InteractionResult.PASS,
                    "Voltmeter read a disabled face instead of returning PASS");
            source.electricity().setSideEnabled(Direction.UP, true);
            PlayerInteractEvent.RightClickBlock handledEvent = diagnosticEvent(
                    helper,
                    player,
                    ModMachineItems.VOLTMETER.get(),
                    FIRST
            );
            DiagnosticInteractionEvents.onRightClickBlock(handledEvent);
            helper.assertFalse(handledEvent.isCanceled(),
                    "Diagnostic event canceled the target block interaction");
            helper.assertTrue(handledEvent.getUseBlock() != Event.Result.DENY,
                    "Diagnostic event denied the target block interaction");
            helper.assertTrue(handledEvent.getUseItem() == Event.Result.DENY,
                    "Diagnostic event allowed a duplicate item-use reading");

            helper.setBlock(THIRD, Blocks.STONE);
            helper.assertTrue(instrumentUse(helper, player, ModMachineItems.THERMOMETER.get(), THIRD) == InteractionResult.PASS,
                    "Thermometer consumed an unsupported target instead of returning PASS");
            PlayerInteractEvent.RightClickBlock passEvent = diagnosticEvent(
                    helper,
                    player,
                    ModMachineItems.THERMOMETER.get(),
                    THIRD
            );
            DiagnosticInteractionEvents.onRightClickBlock(passEvent);
            helper.assertFalse(passEvent.isCanceled(),
                    "Diagnostic event preempted an unsupported target");
            helper.assertTrue(passEvent.getUseBlock() != Event.Result.DENY,
                    "Diagnostic event denied an unsupported target block interaction");
            player.discard();
            helper.succeed();
        });
    }

    @GameTest(template = TEMPLATE)
    public static void portableEquipmentRecipesAreRegistered(GameTestHelper helper) {
        assertRecipe(helper, "low_voltage_battery", ModMachineItems.LOW_BATTERY.get());
        assertRecipe(helper, "medium_voltage_battery", ModMachineItems.MEDIUM_BATTERY.get());
        assertRecipe(helper, "electric_drill", ModMachineItems.ELECTRIC_DRILL.get());
        assertRecipe(helper, "electric_chainsaw", ModMachineItems.ELECTRIC_CHAINSAW.get());
        assertRecipe(helper, "electric_piston", ModMachineItems.ELECTRIC_PISTON.get());
        assertRecipe(helper, "voltmeter", ModMachineItems.VOLTMETER.get());
        assertRecipe(helper, "thermometer", ModMachineItems.THERMOMETER.get());
        helper.succeed();
    }

    private static void assertInstrumentUse(GameTestHelper helper, Player player, Item item, BlockPos relativePosition) {
        helper.assertTrue(instrumentUse(helper, player, item, relativePosition) == InteractionResult.PASS,
                item + " did not return PASS after reading its diagnostic target");
    }

    private static InteractionResult instrumentUse(
            GameTestHelper helper,
            Player player,
            Item item,
            BlockPos relativePosition
    ) {
        ItemStack stack = new ItemStack(item);
        player.setItemInHand(InteractionHand.MAIN_HAND, stack);
        BlockPos absolute = helper.absolutePos(relativePosition);
        UseOnContext context = new UseOnContext(
                player,
                InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false)
        );
        return stack.useOn(context);
    }

    private static PlayerInteractEvent.RightClickBlock diagnosticEvent(
            GameTestHelper helper,
            Player player,
            Item item,
            BlockPos relativePosition
    ) {
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item));
        BlockPos absolute = helper.absolutePos(relativePosition);
        BlockHitResult hit = new BlockHitResult(Vec3.atCenterOf(absolute), Direction.UP, absolute, false);
        return new PlayerInteractEvent.RightClickBlock(player, InteractionHand.MAIN_HAND, absolute, hit);
    }

    private static void assertRecipe(GameTestHelper helper, String path, Item expected) {
        ResourceLocation id = Magneticraft.id("crafting/" + path);
        Recipe<?> recipe = helper.getLevel().getRecipeManager().byKey(id)
                .orElseThrow(() -> new AssertionError("Missing recipe " + id));
        helper.assertTrue(recipe.getResultItem(helper.getLevel().registryAccess()).is(expected),
                id + " returned the wrong item");
    }

    private static void assertPortableCreativeVariants(GameTestHelper helper) {
        CreativeModeTab tab = ModCreativeTabs.MAIN.get();
        tab.buildContents(new CreativeModeTab.ItemDisplayParameters(
                helper.getLevel().enabledFeatures(),
                true,
                helper.getLevel().registryAccess()
        ));
        for (Item item : List.of(
                ModMachineItems.LOW_BATTERY.get(),
                ModMachineItems.MEDIUM_BATTERY.get(),
                ModMachineItems.ELECTRIC_DRILL.get(),
                ModMachineItems.ELECTRIC_CHAINSAW.get(),
                ModMachineItems.ELECTRIC_PISTON.get()
        )) {
            List<ItemStack> variants = tab.getDisplayItems().stream().filter(stack -> stack.is(item)).toList();
            helper.assertTrue(variants.size() == 2, item + " did not expose empty and full creative variants");
            int capacity = ((PortableEnergyItem) item).capacity();
            helper.assertTrue(variants.stream().anyMatch(stack -> energy(stack).getEnergyStored() == 0),
                    item + " omitted its empty creative variant");
            helper.assertTrue(variants.stream().anyMatch(stack -> energy(stack).getEnergyStored() == capacity),
                    item + " omitted its fully charged creative variant");
        }
    }

    private static IEnergyStorage energy(ItemStack stack) {
        return stack.getCapability(ForgeCapabilities.ENERGY).orElseThrow(AssertionError::new);
    }

    private static void charge(ItemStack stack, int amount) {
        IEnergyStorage storage = energy(stack);
        int inserted = 0;
        while (inserted < amount) {
            int accepted = storage.receiveEnergy(amount - inserted, false);
            if (accepted <= 0) {
                throw new AssertionError("Could not charge portable item to " + amount + " FE");
            }
            inserted += accepted;
        }
    }

    private static void tickBattery(
            GameTestHelper helper,
            BlockPos relativePosition,
            BatteryBlockEntity battery
    ) {
        BlockPos absolute = helper.absolutePos(relativePosition);
        BatteryBlockEntity.serverTick(
                helper.getLevel(),
                absolute,
                helper.getLevel().getBlockState(absolute),
                battery
        );
    }

    private static CompoundTag findEnergyData(CompoundTag root) {
        if (root.contains("energy", Tag.TAG_INT)) {
            return root;
        }
        for (String key : root.getAllKeys()) {
            if (root.get(key) instanceof CompoundTag child) {
                CompoundTag found = findEnergyData(child);
                if (found != null) {
                    return found;
                }
            }
        }
        return null;
    }

    private static <T extends BlockEntity> T require(GameTestHelper helper, BlockPos position, Class<T> type) {
        BlockEntity blockEntity = helper.getBlockEntity(position);
        if (!type.isInstance(blockEntity)) {
            throw new AssertionError("Expected " + type.getSimpleName() + " at " + position + ", got " + blockEntity);
        }
        return type.cast(blockEntity);
    }
}
