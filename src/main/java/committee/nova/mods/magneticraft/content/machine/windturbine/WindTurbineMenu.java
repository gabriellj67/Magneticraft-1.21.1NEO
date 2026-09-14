package committee.nova.mods.magneticraft.content.machine.windturbine;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemStackHandler;
import net.neoforged.neoforge.items.SlotItemHandler;

public final class WindTurbineMenu extends AbstractMachineMenu {
    public static final int ROTOR_SLOT_X = 28;
    public static final int ROTOR_SLOT_Y = 36;

    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public WindTurbineMenu(int containerId, Inventory playerInventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, playerInventory, readPosition(buffer), null);
    }

    static BlockPos readPosition(RegistryFriendlyByteBuf buffer) {
        return buffer == null || buffer.readableBytes() < Long.BYTES
                ? BlockPos.ZERO
                : buffer.readBlockPos();
    }

    public WindTurbineMenu(int containerId, Inventory playerInventory, WindTurbineBlockEntity turbine) {
        this(containerId, playerInventory, turbine.getBlockPos(), turbine);
    }

    private WindTurbineMenu(
            int containerId,
            Inventory playerInventory,
            BlockPos position,
            WindTurbineBlockEntity knownTurbine
    ) {
        super(ModMenus.WIND_TURBINE.get(), containerId);
        this.position = position.immutable();
        WindTurbineBlockEntity turbine = knownTurbine;
        if (turbine == null) {
            BlockEntity candidate = playerInventory.player.level().getBlockEntity(position);
            if (candidate instanceof WindTurbineBlockEntity loaded) {
                turbine = loaded;
            }
        }
        IItemHandler handler = turbine == null ? new ItemStackHandler(1) : turbine.inventory().menuHandler();
        data = knownTurbine == null
                ? new SimpleContainerData(WindTurbineBlockEntity.MENU_DATA_COUNT)
                : knownTurbine.menuData();
        access = turbine == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(playerInventory.player.level(), position);

        addSlot(new SlotItemHandler(handler, 0, ROTOR_SLOT_X, ROTOR_SLOT_Y) {
            @Override
            public boolean mayPlace(ItemStack stack) {
                return WindTurbineRotorItem.tier(stack) != null;
            }
        });
        addDataSlots(data);
        finishMachineSlots(
                playerInventory,
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModNetworkBlocks.WIND_TURBINE.get());
    }

    public BlockPos position() {
        return position;
    }

    public int energyStored() {
        return Int32ContainerData.read(data, 0);
    }

    public int energyCapacity() {
        return Int32ContainerData.read(data, 1);
    }

    public double wind() {
        return Int32ContainerData.read(data, 2) / 1_000.0D;
    }

    public double openSpace() {
        return Int32ContainerData.read(data, 3) / 1_000.0D;
    }

    public double productionJoulesPerTick() {
        return Int32ContainerData.read(data, 4) / 10.0D;
    }

    public int rotorTier() {
        return Int32ContainerData.read(data, 5);
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return slots.get(0).mayPlace(stack) && moveItemStackTo(stack, 0, 1, false);
    }
}
