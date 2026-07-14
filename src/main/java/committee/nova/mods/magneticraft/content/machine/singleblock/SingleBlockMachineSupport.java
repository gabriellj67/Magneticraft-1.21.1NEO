package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.framework.module.FluidTankModule;
import committee.nova.mods.magneticraft.content.network.module.HeatNetworkModule;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.GasificationRecipe;
import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModMachineItems;
import committee.nova.mods.magneticraft.init.ModRecipeTypes;
import committee.nova.mods.magneticraft.system.network.heat.HeatNode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.AbstractCookingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.fluids.FluidStack;
import net.minecraftforge.fluids.capability.IFluidHandler;
import net.minecraftforge.items.IItemHandler;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Shared boundary operations used by two or more single-block behavior domains.
 */
final class SingleBlockMachineSupport {
    private SingleBlockMachineSupport() {
    }

    static Direction facing(SingleBlockMachineBlockEntity machine) {
        return machine.getBlockState().getValue(SingleBlockMachineBlock.FACING);
    }

    static Optional<SluiceRecipe> findSluiceRecipe(SingleBlockMachineBlockEntity machine, ItemStack stack) {
        if (stack.isEmpty() || machine.getLevel() == null) {
            return Optional.empty();
        }
        return machine.getLevel().getRecipeManager().getRecipeFor(
                ModRecipeTypes.SLUICE_TYPE.get(),
                new SimpleContainer(stack.copyWithCount(1)),
                machine.getLevel()
        );
    }

    static Optional<GasificationRecipe> findGasificationRecipe(
            SingleBlockMachineBlockEntity machine,
            ItemStack stack
    ) {
        if (stack.isEmpty() || machine.getLevel() == null) {
            return Optional.empty();
        }
        return machine.getLevel().getRecipeManager().getRecipeFor(
                ModRecipeTypes.GASIFICATION_TYPE.get(),
                new SimpleContainer(stack.copyWithCount(1)),
                machine.getLevel()
        );
    }

    static Optional<? extends AbstractCookingRecipe> findSmeltingRecipe(
            SingleBlockMachineBlockEntity machine,
            ItemStack stack
    ) {
        if (stack.isEmpty() || machine.getLevel() == null) {
            return Optional.empty();
        }
        return machine.getLevel().getRecipeManager().getRecipeFor(
                RecipeType.SMELTING,
                new SimpleContainer(stack.copyWithCount(1)),
                machine.getLevel()
        );
    }

    static IItemHandler adjacentItemHandler(SingleBlockMachineBlockEntity machine, Direction direction) {
        if (!(machine.getLevel() instanceof ServerLevel level)) {
            return null;
        }
        BlockPos targetPosition = machine.getBlockPos().relative(direction);
        var targetChunk = level.getChunkSource().getChunkNow(
                targetPosition.getX() >> 4,
                targetPosition.getZ() >> 4
        );
        BlockEntity blockEntity = targetChunk == null ? null : targetChunk.getBlockEntity(targetPosition);
        return blockEntity == null
                ? null
                : blockEntity.getCapability(ForgeCapabilities.ITEM_HANDLER, direction.getOpposite()).orElse(null);
    }

    static int pushFluid(
            SingleBlockMachineBlockEntity machine,
            Direction direction,
            FluidTankModule source,
            int maximum
    ) {
        if (maximum <= 0 || !(machine.getLevel() instanceof ServerLevel level)) {
            return 0;
        }
        BlockPos targetPosition = machine.getBlockPos().relative(direction);
        var targetChunk = level.getChunkSource().getChunkNow(
                targetPosition.getX() >> 4,
                targetPosition.getZ() >> 4
        );
        if (targetChunk == null) {
            return 0;
        }
        BlockEntity targetEntity = targetChunk.getBlockEntity(targetPosition);
        if (targetEntity == null) {
            return 0;
        }
        IFluidHandler target = targetEntity
                .getCapability(ForgeCapabilities.FLUID_HANDLER, direction.getOpposite())
                .orElse(null);
        if (target == null) {
            return 0;
        }
        FluidStack offered = source.tank().drain(maximum, IFluidHandler.FluidAction.SIMULATE);
        if (offered.isEmpty()) {
            return 0;
        }
        int accepted = target.fill(offered, IFluidHandler.FluidAction.SIMULATE);
        if (accepted <= 0) {
            return 0;
        }
        FluidStack drained = source.tank().drain(accepted, IFluidHandler.FluidAction.EXECUTE);
        int inserted = target.fill(drained, IFluidHandler.FluidAction.EXECUTE);
        if (inserted < drained.getAmount()) {
            FluidStack remainder = drained.copy();
            remainder.setAmount(drained.getAmount() - inserted);
            source.tank().fill(remainder, IFluidHandler.FluidAction.EXECUTE);
        }
        return inserted;
    }

    static boolean filterAllows(
            SingleBlockMachineBlockEntity machine,
            SingleBlockMachineState state,
            ItemStack stack,
            boolean whitelist
    ) {
        if (stack.isEmpty()) {
            return false;
        }
        int filterSlots = machine.filters() == null ? 0 : machine.filters().size();
        if (filterSlots == 0) {
            return !stack.isEmpty();
        }
        boolean anyFilter = false;
        boolean matched = false;
        for (int slot = 0; slot < filterSlots; slot++) {
            ItemStack filter = machine.filters().getFilter(slot);
            if (filter.isEmpty()) {
                continue;
            }
            anyFilter = true;
            if (matchesFilter(state, filter, stack)) {
                matched = true;
                break;
            }
        }
        if (!anyFilter) {
            return machine.definition() == SingleBlockMachineDefinition.INSERTER ? !whitelist : true;
        }
        return whitelist ? matched : !matched;
    }

    static boolean canAcceptItemOutput(SingleBlockMachineBlockEntity machine, int slot, ItemStack result) {
        if (result.isEmpty()) {
            return true;
        }
        if (machine.inventory() == null) {
            return false;
        }
        ItemStack output = machine.inventory().getStackInSlot(slot);
        return output.isEmpty()
                || (ItemStack.isSameItemSameTags(output, result)
                && output.getCount() + result.getCount() <= output.getMaxStackSize());
    }

    static void insertInventory(SingleBlockMachineBlockEntity machine, int slot, ItemStack result) {
        if (machine.inventory() == null || result.isEmpty()) {
            return;
        }
        ItemStack output = machine.inventory().getStackInSlot(slot);
        if (output.isEmpty()) {
            machine.inventory().setStackInSlot(slot, result.copy());
            return;
        }
        ItemStack combined = output.copy();
        combined.grow(result.getCount());
        machine.inventory().setStackInSlot(slot, combined);
    }

    static int thermalSpeed(SingleBlockMachineBlockEntity machine, double minimumTemperature, int scale) {
        return machine.heat() == null
                ? 0
                : SingleBlockMachineMath.thermalSpeed(
                machine.heat().node().temperatureKelvin(),
                minimumTemperature,
                scale
        );
    }

    static void dissipateHeat(SingleBlockMachineBlockEntity machine, double joules) {
        HeatNetworkModule heat = machine.heat();
        if (heat == null) {
            return;
        }
        double ambientEnergy = HeatNode.AMBIENT_TEMPERATURE_KELVIN * heat.node().heatCapacityJoulesPerKelvin();
        double removable = Math.max(0.0D, heat.node().internalEnergyJoules() - ambientEnergy);
        heat.node().removeHeat(Math.min(joules, removable), false);
    }

    static boolean isTroughFood(ItemStack stack) {
        return stack.is(Items.WHEAT) || stack.is(Items.CARROT) || stack.is(Items.WHEAT_SEEDS);
    }

    static boolean isCombustionFuel(ItemStack stack) {
        return stack.is(Items.COAL) || stack.is(Items.CHARCOAL) || stack.is(Blocks.COAL_BLOCK.asItem());
    }

    static boolean isInserterUpgrade(ItemStack stack) {
        return stack.is(ModMachineItems.INSERTER_SPEED_UPGRADE.get())
                || stack.is(ModMachineItems.INSERTER_STACK_UPGRADE.get());
    }

    static List<InserterAccess> inserterInventoryAccesses(BlockPos origin, Direction direction) {
        BlockPos sameLevel = origin.relative(direction);
        return List.of(
                new InserterAccess(sameLevel, Direction.UP, true),
                new InserterAccess(sameLevel.below(), Direction.UP, true),
                new InserterAccess(sameLevel, direction.getOpposite(), false)
        );
    }

    static List<BlockPos> inserterGroundPickupPositions(BlockPos origin, Direction direction) {
        BlockPos sameLevel = origin.relative(direction);
        return List.of(sameLevel.below(), sameLevel);
    }

    static List<BlockPos> inserterGroundDropPositions(BlockPos origin, Direction direction) {
        BlockPos sameLevel = origin.relative(direction);
        return List.of(sameLevel.below(), sameLevel);
    }

    record InserterAccess(BlockPos position, Direction side, boolean includeMinecarts) {
    }

    private static boolean matchesFilter(SingleBlockMachineState state, ItemStack filter, ItemStack stack) {
        boolean itemMatches = filter.is(stack.getItem());
        if (!itemMatches && state.inserterUseTags) {
            itemMatches = filter.getTags().anyMatch(stack::is);
        }
        if (!itemMatches) {
            return false;
        }
        if (state.inserterUseDamage && filter.getDamageValue() != stack.getDamageValue()) {
            return false;
        }
        return !state.inserterUseNbt || Objects.equals(filter.getTag(), stack.getTag());
    }
}
