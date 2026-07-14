package committee.nova.mods.magneticraft.content.machine.singleblock;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.GhostFilterMenuAccess;
import committee.nova.mods.magneticraft.content.machine.framework.menu.GhostSlot;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.module.GhostFilterModule;
import committee.nova.mods.magneticraft.init.ModMachineBlocks;
import committee.nova.mods.magneticraft.init.ModMenus;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.SimpleContainer;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.items.IItemHandler;
import net.minecraftforge.items.ItemStackHandler;
import net.minecraftforge.items.SlotItemHandler;

import java.util.ArrayList;
import java.util.List;

/**
 * One dynamic menu type whose layout is selected by the authoritative machine definition.
 */
public final class SingleBlockMachineMenu extends AbstractMachineMenu implements GhostFilterMenuAccess {
    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final SingleBlockMachineDefinition definition;
    private final ContainerData data;
    private final List<Integer> playerTargetSlots = new ArrayList<>();
    private final SimpleContainer fabricatorResult = new SimpleContainer(1);
    private final int playerInventoryTop;
    private final int fabricatorResultSlot;
    private final SingleBlockMachineBlockEntity machine;

    public SingleBlockMachineMenu(
            SingleBlockMachineDefinition definition,
            int containerId,
            Inventory playerInventory,
            FriendlyByteBuf buffer
    ) {
        this(containerId, playerInventory, buffer.readBlockPos(), null, definition);
    }

    public SingleBlockMachineMenu(
            int containerId,
            Inventory playerInventory,
            SingleBlockMachineBlockEntity machine
    ) {
        this(containerId, playerInventory, machine.getBlockPos(), machine, machine.definition());
    }

    private SingleBlockMachineMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            SingleBlockMachineBlockEntity knownMachine,
            SingleBlockMachineDefinition definition
    ) {
        super(ModMenus.singleBlockMachine(definition).get(), containerId);
        this.position = position.immutable();

        SingleBlockMachineBlockEntity resolved = knownMachine;
        if (resolved == null) {
            BlockEntity candidate = playerInventory.player.level().getBlockEntity(position);
            if (candidate instanceof SingleBlockMachineBlockEntity loaded) {
                resolved = loaded;
            }
        }
        if (resolved != null && resolved.definition() != definition) {
            throw new IllegalArgumentException("Menu type " + definition.id()
                    + " does not match block entity " + resolved.definition().id());
        }
        machine = resolved;
        this.definition = definition;
        access = resolved == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(playerInventory.player.level(), position);
        data = knownMachine == null
                ? new SimpleContainerData(SingleBlockMachineBlockEntity.MENU_DATA_COUNT)
                : knownMachine.menuData();
        addDataSlots(data);

        IItemHandler inventory = resolved == null || resolved.inventory() == null
                ? new ItemStackHandler(definition.inventorySlots())
                : resolved.inventory().menuHandler();
        GhostFilterModule filters = resolved == null ? null : resolved.filters();

        int resultSlot = -1;
        switch (definition) {
            case BOX -> addGrid(inventory, 0, 3, 9, 8, 18, true);
            case FABRICATOR -> {
                addGhostGrid(filters, 3, 3, 30, 18);
                resultSlot = slots.size();
                addSlot(new Slot(fabricatorResult, 0, 124, 36) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }

                    @Override
                    public boolean mayPickup(Player player) {
                        return false;
                    }
                });
                addGrid(inventory, 0, 3, 3, 62, 84, true);
            }
            case INSERTER -> {
                addMachineSlot(inventory, 0, 26, 35, false);
                addMachineSlot(inventory, 1, 26, 57, true);
                addMachineSlot(inventory, 2, 44, 57, true);
                addGhostGrid(filters, 3, 3, 92, 18);
            }
            case RELAY -> addGrid(inventory, 0, 3, 3, 62, 18, true);
            case FILTER, TRANSPOSER -> addGhostGrid(filters, 3, 3, 62, 18);
            case COMBUSTION_CHAMBER -> addMachineSlot(inventory, 0, 80, 35, true);
            case GASIFICATION_UNIT, BRICK_FURNACE -> {
                addMachineSlot(inventory, 0, 56, 35, true);
                addSlot(new SlotItemHandler(inventory, 1, 116, 35) {
                    @Override
                    public boolean mayPlace(ItemStack stack) {
                        return false;
                    }
                });
            }
            default -> {
            }
        }
        fabricatorResultSlot = resultSlot;
        playerInventoryTop = definition == SingleBlockMachineDefinition.FABRICATOR ? 156 : 84;
        finishMachineSlots(playerInventory, 8, playerInventoryTop);
        refreshFabricatorResult();
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModMachineBlocks.machine(definition).get());
    }

    @Override
    public void clicked(int slotId, int button, ClickType clickType, Player player) {
        if (slotId == fabricatorResultSlot && clickType == ClickType.PICKUP) {
            if (!player.level().isClientSide && machine != null && stillValid(player)) {
                if (button == 0 && player.getAbilities().mayBuild) {
                    machine.craftFabricator();
                } else if (button == 1
                        && player.getAbilities().mayBuild
                        && machine.filters() != null) {
                    for (int slot = 0; slot < machine.filters().size(); slot++) {
                        machine.filters().setFilter(slot, ItemStack.EMPTY);
                    }
                }
                refreshFabricatorResult();
            }
            return;
        }
        super.clicked(slotId, button, clickType, player);
    }

    @Override
    public boolean clickMenuButton(Player player, int id) {
        if (!player.level().isClientSide
                && definition == SingleBlockMachineDefinition.INSERTER
                && id >= 0 && id < 6
                && machine != null
                && player.getAbilities().mayBuild
                && stillValid(player)) {
            machine.toggleInserterFlag(id);
            return true;
        }
        return false;
    }

    @Override
    public void broadcastChanges() {
        refreshFabricatorResult();
        super.broadcastChanges();
    }

    public SingleBlockMachineDefinition definition() {
        return definition;
    }

    public int playerInventoryTop() {
        return playerInventoryTop;
    }

    public int imageHeight() {
        return definition == SingleBlockMachineDefinition.FABRICATOR ? 238 : 166;
    }

    public int energyStored() {
        return Int32ContainerData.read(data, 0);
    }

    public int energyCapacity() {
        return Int32ContainerData.read(data, 1);
    }

    public int progress() {
        return Int32ContainerData.read(data, 2);
    }

    public int totalProgress() {
        return Int32ContainerData.read(data, 3);
    }

    public double temperatureKelvin() {
        return Int32ContainerData.read(data, 4) / 10.0D;
    }

    public int primaryFluid() {
        return Int32ContainerData.read(data, 5);
    }

    public int primaryCapacity() {
        return Int32ContainerData.read(data, 6);
    }

    public int secondaryFluid() {
        return Int32ContainerData.read(data, 7);
    }

    public int secondaryCapacity() {
        return Int32ContainerData.read(data, 8);
    }

    public int flags() {
        return Int32ContainerData.read(data, 9);
    }

    public double voltage() {
        return Int32ContainerData.read(data, 10) / 10.0D;
    }

    public int thermopileFlux() {
        return Int32ContainerData.read(data, 11);
    }

    public int burnProgress() {
        return Int32ContainerData.read(data, 12);
    }

    public int burnTotal() {
        return Int32ContainerData.read(data, 13);
    }

    public int lastConsumption() {
        return Int32ContainerData.read(data, 14);
    }

    public int lastProduction() {
        return Int32ContainerData.read(data, 15);
    }

    public boolean working() {
        return Int32ContainerData.read(data, 16) != 0;
    }

    @Override
    public BlockPos machinePosition() {
        return position;
    }

    @Override
    public int ghostFilterCount() {
        return machine == null || machine.filters() == null ? 0 : machine.filters().size();
    }

    @Override
    public void setGhostFilter(int slot, ItemStack sample) {
        if (machine != null && machine.filters() != null) {
            machine.filters().setFilter(slot, sample);
            refreshFabricatorResult();
        }
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        for (int slotIndex : playerTargetSlots) {
            Slot slot = slots.get(slotIndex);
            if (slot.mayPlace(stack) && moveItemStackTo(stack, slotIndex, slotIndex + 1, false)) {
                return true;
            }
        }
        return false;
    }

    private void addGrid(
            IItemHandler handler,
            int startSlot,
            int rows,
            int columns,
            int x,
            int y,
            boolean playerTarget
    ) {
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                addMachineSlot(
                        handler,
                        startSlot + row * columns + column,
                        x + column * 18,
                        y + row * 18,
                        playerTarget
                );
            }
        }
    }

    private void addMachineSlot(IItemHandler handler, int slot, int x, int y, boolean playerTarget) {
        int index = slots.size();
        addSlot(new SlotItemHandler(handler, slot, x, y));
        if (playerTarget) {
            playerTargetSlots.add(index);
        }
    }

    private void addGhostGrid(@javax.annotation.Nullable GhostFilterModule filters, int rows, int columns, int x, int y) {
        if (filters == null) {
            return;
        }
        for (int row = 0; row < rows; row++) {
            for (int column = 0; column < columns; column++) {
                int slot = row * columns + column;
                addSlot(new GhostSlot(filters, slot, x + column * 18, y + row * 18));
            }
        }
    }

    private void refreshFabricatorResult() {
        if (definition == SingleBlockMachineDefinition.FABRICATOR && machine != null) {
            fabricatorResult.setItem(0, machine.fabricatorResult());
        }
    }
}
