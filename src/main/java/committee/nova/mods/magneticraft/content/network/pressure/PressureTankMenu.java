package committee.nova.mods.magneticraft.content.network.pressure;

import committee.nova.mods.magneticraft.content.machine.framework.menu.AbstractMachineMenu;
import committee.nova.mods.magneticraft.content.machine.framework.menu.Int32ContainerData;
import committee.nova.mods.magneticraft.content.machine.framework.menu.LegacyMachineGuiLayout;
import committee.nova.mods.magneticraft.init.ModMenus;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BlockEntity;

import java.util.Optional;

public final class PressureTankMenu extends AbstractMachineMenu {
    private final Inventory playerInventory;
    private final BlockPos position;
    private final ContainerLevelAccess access;
    private final ContainerData data;

    public PressureTankMenu(int containerId, Inventory inventory, RegistryFriendlyByteBuf buffer) {
        this(containerId, inventory, buffer.readBlockPos(), null);
    }

    public PressureTankMenu(int containerId, Inventory inventory, PressureTankBlockEntity tank) {
        this(containerId, inventory, tank.getBlockPos(), tank);
    }

    private PressureTankMenu(
            int containerId,
            Inventory inventory,
            BlockPos position,
            PressureTankBlockEntity knownTank
    ) {
        super(ModMenus.PRESSURE_TANK.get(), containerId);
        playerInventory = inventory;
        this.position = position.immutable();
        data = knownTank == null
                ? new SimpleContainerData(PressureTankBlockEntity.MENU_DATA_COUNT)
                : knownTank.data();
        access = knownTank == null
                ? ContainerLevelAccess.NULL
                : ContainerLevelAccess.create(inventory.player.level(), position);
        addDataSlots(data);
        finishMachineSlots(
                inventory,
                LegacyMachineGuiLayout.STANDARD_PLAYER_LEFT,
                LegacyMachineGuiLayout.STANDARD_PLAYER_TOP
        );
    }

    @Override
    public boolean stillValid(Player player) {
        return AbstractContainerMenu.stillValid(access, player, ModNetworkBlocks.PRESSURE_TANK.get());
    }

    public double pressureKpa() {
        return Int32ContainerData.read(data, 0) / 1_000.0D;
    }

    public double gasKpaLiters() {
        return Int32ContainerData.read(data, 1) / 1_000.0D;
    }

    public double capacityKpaLiters() {
        return Int32ContainerData.read(data, 2) / 1_000.0D;
    }

    public double fillRatio() {
        double capacity = capacityKpaLiters();
        return capacity <= 0.0D ? 0.0D : gasKpaLiters() / capacity;
    }

    public Optional<ResourceLocation> gasId() {
        BlockEntity blockEntity = playerInventory.player.level().getBlockEntity(position);
        return blockEntity instanceof PressureTankBlockEntity tank
                ? tank.pressure().node().gasId()
                : Optional.empty();
    }

    @Override
    protected boolean movePlayerStackToMachine(ItemStack stack) {
        return false;
    }
}
