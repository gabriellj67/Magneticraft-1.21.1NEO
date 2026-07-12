package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.system.network.logistics.ItemHandlerTransactions;
import net.minecraft.core.Direction;
import net.minecraft.core.NonNullList;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.inventory.TransientCraftingContainer;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraftforge.items.IItemHandler;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Ghost-grid recipe resolution and atomic ingredient reservation for the fabricator.
 */
final class SingleBlockFabricator {
    private final SingleBlockMachineBlockEntity machine;

    SingleBlockFabricator(SingleBlockMachineBlockEntity machine) {
        this.machine = machine;
    }

    ItemStack result() {
        if (!isAvailable()) {
            return ItemStack.EMPTY;
        }
        CraftingContainer grid = createGrid();
        return machine.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, machine.getLevel())
                .map(recipe -> recipe.assemble(grid, machine.getLevel().registryAccess()))
                .orElse(ItemStack.EMPTY);
    }

    boolean canCraft() {
        return createPlan().isPresent();
    }

    boolean craft() {
        Optional<FabricatorPlan> optionalPlan = createPlan();
        if (optionalPlan.isEmpty()
                || machine.inventory() == null
                || !(machine.getLevel() instanceof ServerLevel level)) {
            return false;
        }
        FabricatorPlan plan = optionalPlan.get();
        for (ReservedSlot reserved : plan.reservations()) {
            ItemStack simulated = reserved.handler().extractItem(reserved.slot(), reserved.amount(), true);
            if (simulated.getCount() != reserved.amount()) {
                return false;
            }
        }
        for (ReservedSlot reserved : plan.reservations()) {
            reserved.handler().extractItem(reserved.slot(), reserved.amount(), false);
        }

        NonNullList<ItemStack> remaining = plan.recipe().getRemainingItems(plan.grid());
        for (ItemStack stack : remaining) {
            insertOrDrop(level, stack.copy());
        }
        insertOrDrop(level, plan.result().copy());
        machine.markChangedAndSync();
        return true;
    }

    private boolean isAvailable() {
        return machine.definition() == SingleBlockMachineDefinition.FABRICATOR
                && machine.filters() != null
                && machine.inventory() != null
                && machine.getLevel() != null;
    }

    private CraftingContainer createGrid() {
        CraftingContainer grid = new TransientCraftingContainer(new DummyCraftingMenu(), 3, 3);
        if (machine.filters() != null) {
            for (int slot = 0; slot < 9; slot++) {
                grid.setItem(slot, machine.filters().getFilter(slot));
            }
        }
        return grid;
    }

    private Optional<FabricatorPlan> createPlan() {
        if (!isAvailable()) {
            return Optional.empty();
        }
        CraftingContainer grid = createGrid();
        Optional<CraftingRecipe> recipeOptional = machine.getLevel().getRecipeManager()
                .getRecipeFor(RecipeType.CRAFTING, grid, machine.getLevel());
        if (recipeOptional.isEmpty()) {
            return Optional.empty();
        }
        CraftingRecipe recipe = recipeOptional.get();
        ItemStack expected = recipe.assemble(grid, machine.getLevel().registryAccess());
        if (expected.isEmpty()) {
            return Optional.empty();
        }

        List<IItemHandler> sources = ingredientSources();
        Map<HandlerSlotKey, Integer> reservedCounts = new LinkedHashMap<>();
        for (int gridSlot = 0; gridSlot < 9; gridSlot++) {
            ItemStack sample = machine.filters().getFilter(gridSlot);
            if (sample.isEmpty()) {
                continue;
            }
            if (!reserveGridSlot(grid, recipe, expected, gridSlot, sources, reservedCounts)) {
                return Optional.empty();
            }
        }
        List<ReservedSlot> reservations = new ArrayList<>(reservedCounts.size());
        reservedCounts.forEach((key, amount) -> reservations.add(
                new ReservedSlot(key.handler(), key.slot(), amount)
        ));
        return Optional.of(new FabricatorPlan(recipe, grid, expected.copy(), List.copyOf(reservations)));
    }

    private List<IItemHandler> ingredientSources() {
        List<IItemHandler> sources = new ArrayList<>();
        sources.add(machine.inventory().menuHandler());
        for (Direction direction : Direction.values()) {
            IItemHandler handler = SingleBlockMachineSupport.adjacentItemHandler(machine, direction);
            if (handler != null && handler != machine.inventory().menuHandler()) {
                sources.add(handler);
            }
        }
        return sources;
    }

    private boolean reserveGridSlot(
            CraftingContainer grid,
            CraftingRecipe recipe,
            ItemStack expected,
            int gridSlot,
            List<IItemHandler> sources,
            Map<HandlerSlotKey, Integer> reservedCounts
    ) {
        ItemStack original = grid.getItem(gridSlot);
        for (IItemHandler source : sources) {
            for (int sourceSlot = 0; sourceSlot < source.getSlots(); sourceSlot++) {
                ItemStack available = source.getStackInSlot(sourceSlot);
                HandlerSlotKey key = new HandlerSlotKey(source, sourceSlot);
                int alreadyReserved = reservedCounts.getOrDefault(key, 0);
                if (available.isEmpty() || available.getCount() <= alreadyReserved) {
                    continue;
                }
                grid.setItem(gridSlot, available.copyWithCount(1));
                if (!recipe.matches(grid, machine.getLevel())
                        || !ItemStack.isSameItemSameTags(
                        expected,
                        recipe.assemble(grid, machine.getLevel().registryAccess())
                )) {
                    continue;
                }
                reservedCounts.put(key, alreadyReserved + 1);
                return true;
            }
        }
        grid.setItem(gridSlot, original);
        return false;
    }

    private void insertOrDrop(ServerLevel level, ItemStack stack) {
        if (stack.isEmpty() || machine.inventory() == null) {
            return;
        }
        ItemStack remainder = ItemHandlerTransactions.insert(machine.inventory().menuHandler(), stack, false);
        if (!remainder.isEmpty()) {
            net.minecraft.world.Containers.dropItemStack(
                    level,
                    machine.getBlockPos().getX() + 0.5D,
                    machine.getBlockPos().getY() + 1.0D,
                    machine.getBlockPos().getZ() + 0.5D,
                    remainder
            );
        }
    }

    private static final class DummyCraftingMenu extends AbstractContainerMenu {
        private DummyCraftingMenu() {
            super(null, -1);
        }

        @Override
        public ItemStack quickMoveStack(Player player, int index) {
            return ItemStack.EMPTY;
        }

        @Override
        public boolean stillValid(Player player) {
            return false;
        }
    }

    private record HandlerSlotKey(IItemHandler handler, int slot) {
        @Override
        public boolean equals(Object object) {
            return object instanceof HandlerSlotKey other && handler == other.handler && slot == other.slot;
        }

        @Override
        public int hashCode() {
            return System.identityHashCode(handler) * 31 + slot;
        }
    }

    private record ReservedSlot(IItemHandler handler, int slot, int amount) {
    }

    private record FabricatorPlan(
            CraftingRecipe recipe,
            CraftingContainer grid,
            ItemStack result,
            List<ReservedSlot> reservations
    ) {
    }
}
