package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.singleblock.recipe.SluiceRecipe;
import committee.nova.mods.magneticraft.init.ModNetworkItems;
import committee.nova.mods.magneticraft.init.ModTags;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.fluids.FluidActionResult;
import net.neoforged.neoforge.fluids.FluidStack;
import net.neoforged.neoforge.fluids.FluidUtil;

import java.util.Optional;

/**
 * Server-authoritative direct player interactions that do not open a menu.
 */
final class SingleBlockMachineInteractions {
    private final SingleBlockMachineBlockEntity machine;
    private final SingleBlockMachineState state;

    SingleBlockMachineInteractions(SingleBlockMachineBlockEntity machine, SingleBlockMachineState state) {
        this.machine = machine;
        this.state = state;
    }

    InteractionResult interact(Player player, InteractionHand hand, BlockHitResult hit) {
        if (machine.getLevel() == null) {
            return InteractionResult.PASS;
        }
        return switch (machine.definition()) {
            case SLUICE_BOX -> interactSluice(player, hand);
            case FEEDING_TROUGH -> interactFeedingTrough(player, hand);
            case SMALL_TANK, WATER_GENERATOR, STEAM_BOILER, GASIFICATION_UNIT ->
                    interactFluidMachine(player, hand);
            case COMBUSTION_CHAMBER -> interactCombustionChamber(player, hand, hit);
            default -> InteractionResult.PASS;
        };
    }

    private InteractionResult interactSluice(Player player, InteractionHand hand) {
        if (machine.inventory() == null || machine.getLevel() == null) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        Optional<FluidStack> contained = FluidUtil.getFluidContained(held);
        if (state.progress == 0 && contained.filter(fluid -> fluid.getFluid().defaultFluidState()
                .is(net.minecraft.tags.FluidTags.WATER) && fluid.getAmount() >= 1_000).isPresent()) {
            if (!machine.getLevel().isClientSide) {
                FluidActionResult result = FluidUtil.tryEmptyContainer(
                        held,
                        new net.neoforged.neoforge.fluids.capability.templates.FluidTank(1_000),
                        1_000,
                        player,
                        !player.getAbilities().instabuild
                );
                if (result.isSuccess() && !player.getAbilities().instabuild) {
                    player.setItemInHand(hand, result.getResult());
                }
                machine.activateSluiceChain();
            }
            return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
        }
        Optional<SluiceRecipe> recipe = SingleBlockMachineSupport.findSluiceRecipe(machine, held);
        if (!held.isEmpty() && recipe.isPresent()) {
            if (!machine.getLevel().isClientSide) {
                ItemStack stored = machine.inventory().getStackInSlot(0);
                int room = stored.isEmpty()
                        ? SingleBlockMachineBlockEntity.SLUICE_MAX_ITEMS
                        : SingleBlockMachineBlockEntity.SLUICE_MAX_ITEMS - stored.getCount();
                if (room > 0 && (stored.isEmpty() || ItemStack.isSameItemSameComponents(stored, held))) {
                    int moved = Math.min(room, held.getCount());
                    ItemStack inserted = held.copyWithCount(moved);
                    machine.inventory().setStackInSlot(
                            0,
                            stored.isEmpty() ? inserted : stored.copyWithCount(stored.getCount() + moved)
                    );
                    if (!player.getAbilities().instabuild) {
                        held.shrink(moved);
                    }
                    machine.markChangedAndSync();
                }
            }
            return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
        }
        return InteractionResult.PASS;
    }

    private InteractionResult interactFeedingTrough(Player player, InteractionHand hand) {
        if (machine.inventory() == null || machine.getLevel() == null) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (held.isEmpty()) {
            if (!machine.getLevel().isClientSide) {
                ItemStack stored = machine.inventory().getStackInSlot(0);
                ItemStack extracted = machine.inventory().extractInternal(0, stored.getCount(), false);
                player.setItemInHand(hand, extracted);
            }
            return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
        }
        if (SingleBlockMachineSupport.isTroughFood(held)) {
            if (!machine.getLevel().isClientSide) {
                ItemStack remainder = machine.inventory().menuHandler().insertItem(0, held.copy(), false);
                int moved = held.getCount() - remainder.getCount();
                if (!player.getAbilities().instabuild) {
                    held.shrink(moved);
                }
            }
            return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
        }
        return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
    }

    private InteractionResult interactFluidMachine(Player player, InteractionHand hand) {
        if (machine.getLevel() == null || machine.primaryTank() == null) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (machine.definition() == SingleBlockMachineDefinition.SMALL_TANK
                && (held.is(ModNetworkItems.WRENCH.get()) || held.is(ModTags.Items.WRENCHES))) {
            if (!machine.getLevel().isClientSide) {
                state.tankExportEnabled = !state.tankExportEnabled;
                player.displayClientMessage(
                        Component.translatable(
                                state.tankExportEnabled
                                        ? "message.magneticraft.tank_export_enabled"
                                        : "message.magneticraft.tank_export_disabled"
                        ),
                        true
                );
                machine.markChangedAndSync();
            }
            return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
        }
        if (held.getCapability(Capabilities.FluidHandler.ITEM) == null) {
            return InteractionResult.PASS;
        }
        if (!machine.getLevel().isClientSide) {
            boolean containsFluid = FluidUtil.getFluidContained(held).filter(stack -> !stack.isEmpty()).isPresent();
            boolean success;
            if (containsFluid) {
                success = FluidUtil.interactWithFluidHandler(player, hand, machine.primaryTank().tank());
                if (!success && machine.secondaryTank() != null) {
                    success = FluidUtil.interactWithFluidHandler(player, hand, machine.secondaryTank().tank());
                }
            } else {
                success = machine.secondaryTank() != null
                        && FluidUtil.interactWithFluidHandler(player, hand, machine.secondaryTank().tank());
                if (!success) {
                    success = FluidUtil.interactWithFluidHandler(player, hand, machine.primaryTank().tank());
                }
            }
            if (!success) {
                return InteractionResult.PASS;
            }
        }
        return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
    }

    private InteractionResult interactCombustionChamber(
            Player player,
            InteractionHand hand,
            BlockHitResult hit
    ) {
        if (machine.inventory() == null || machine.getLevel() == null || player.isSecondaryUseActive()
                || !isCombustionDoorHit(hit)) {
            return InteractionResult.PASS;
        }
        ItemStack held = player.getItemInHand(hand);
        if (!machine.getLevel().isClientSide) {
            if (state.doorOpen && SingleBlockMachineSupport.isCombustionFuel(held)) {
                ItemStack remainder = machine.inventory().menuHandler().insertItem(0, held.copy(), false);
                int moved = held.getCount() - remainder.getCount();
                if (!player.getAbilities().instabuild) {
                    held.shrink(moved);
                }
            } else {
                state.doorOpen = !state.doorOpen;
                machine.markChangedAndSync();
            }
        }
        return InteractionResult.sidedSuccess(machine.getLevel().isClientSide);
    }

    private boolean isCombustionDoorHit(BlockHitResult hit) {
        Direction facing = SingleBlockMachineSupport.facing(machine);
        if (hit.getDirection() != facing) {
            return false;
        }
        Vec3 local = hit.getLocation().subtract(Vec3.atLowerCornerOf(machine.getBlockPos()));
        double across = facing.getAxis() == Direction.Axis.X ? local.z : local.x;
        return across >= 3.0D / 16.0D && across <= 13.0D / 16.0D
                && local.y >= 2.0D / 16.0D && local.y <= 10.0D / 16.0D;
    }
}
