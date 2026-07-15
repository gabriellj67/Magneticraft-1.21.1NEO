package committee.nova.mods.magneticraft.content.machine.electricfurnace;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.content.machine.framework.module.ItemInventoryModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalPowerModule;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * Two-slot, energy-proportional electric furnace.
 */
public final class ElectricFurnaceBlockEntity extends MachineBlockEntity implements MenuProvider {
    public static final int ENERGY_CAPACITY = 10_000;

    private final ItemInventoryModule inventory;
    private final ElectricalPowerModule energy;
    private final ElectricalNetworkModule electricity;
    private final ElectricFurnaceProcessModule process;
    private final ContainerData data;

    public ElectricFurnaceBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.ELECTRIC_FURNACE.get(), position, state);
        inventory = addModule(new ItemInventoryModule(
                Magneticraft.id("inventory"),
                this,
                2,
                (slot, stack) -> slot == 0,
                side -> new ItemInventoryModule.SlotAccess(new int[]{0}, new int[]{1})
        ));
        electricity = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity"),
                this,
                ElectricalNodeKind.MACHINE,
                side -> true
        ));
        energy = addModule(new ElectricalPowerModule(
                Magneticraft.id("energy_storage"),
                Magneticraft.id("electric_furnace"),
                this,
                electricity,
                ElectricalPowerModule.ForgeEnergyAccess.NONE,
                side -> false,
                false
        ));
        process = addModule(new ElectricFurnaceProcessModule(
                Magneticraft.id("processing"),
                this,
                inventory,
                energy
        ));
        data = new ContainerData() {
            @Override
            public int get(int index) {
                return switch (index) {
                    case 0 -> energy.storedWholeJoules();
                    case 1 -> energy.ratedCapacityWholeJoules();
                    case 2 -> process.progressUnits();
                    case 3 -> ElectricFurnaceProcessModule.TOTAL_PROGRESS_UNITS;
                    default -> 0;
                };
            }

            @Override
            public void set(int index, int value) {
                if (index == 0) {
                    energy.setStoredJoules(value);
                }
            }

            @Override
            public int getCount() {
                return 4;
            }
        };
    }

    public static void serverTick(Level level, BlockPos position, BlockState state, ElectricFurnaceBlockEntity furnace) {
        furnace.tickModules();
        boolean lit = state.getValue(ElectricFurnaceBlock.LIT);
        boolean shouldBeLit = !furnace.electricalFaulted() && furnace.process.working();
        if (lit != shouldBeLit) {
            level.setBlock(position, state.setValue(ElectricFurnaceBlock.LIT, shouldBeLit), 3);
        }
        furnace.finishServerTick();
    }

    public ItemInventoryModule inventory() {
        return inventory;
    }

    public ElectricalPowerModule energy() {
        return energy;
    }

    public ElectricalNetworkModule electricity() {
        return electricity;
    }

    public ElectricFurnaceProcessModule process() {
        return process;
    }

    public ContainerData data() {
        return data;
    }

    public void dropContents(Level level) {
        for (int slot = 0; slot < inventory.slots(); slot++) {
            ItemStack stack = inventory.extractInternal(slot, Integer.MAX_VALUE, false);
            if (!stack.isEmpty()) {
                net.minecraft.world.Containers.dropItemStack(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        stack
                );
            }
        }
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magneticraft.electric_furnace");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInventory, Player player) {
        return new ElectricFurnaceMenu(containerId, playerInventory, this);
    }
}
