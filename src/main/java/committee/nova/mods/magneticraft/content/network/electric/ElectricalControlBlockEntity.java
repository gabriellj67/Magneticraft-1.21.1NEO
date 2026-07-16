package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalControlModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalPlacementHost;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalSideHost;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

import java.util.Optional;

/** Two isolated terminals joined exclusively by the selected control-device coupler. */
public final class ElectricalControlBlockEntity extends NetworkComponentBlockEntity
        implements TieredElectricalHost, TieredElectricalSideHost, TieredElectricalPlacementHost, MenuProvider {
    public static final ResourceLocation BACK_TERMINAL = Magneticraft.id("back");
    public static final ResourceLocation FRONT_TERMINAL = Magneticraft.id("front");

    private final ElectricalControlKind kind;
    private final ElectricalNetworkModule back;
    private final ElectricalNetworkModule front;
    private final ElectricalControlModule control;

    public ElectricalControlBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.ELECTRICAL_CONTROL.get(), position, state);
        kind = block(state).kind();
        back = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_back"),
                this,
                VoltageTierIds.LOW,
                BACK_TERMINAL,
                ElectricalNodeKind.MACHINE,
                side -> side == facing().getOpposite()
        ));
        front = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_front"),
                this,
                VoltageTierIds.LOW,
                FRONT_TERMINAL,
                ElectricalNodeKind.MACHINE,
                side -> side == facing()
        ));
        control = addModule(new ElectricalControlModule(
                Magneticraft.id("electrical_control"),
                this,
                back,
                front,
                kind
        ));
    }

    @Override
    public ElectricalNetworkModule tieredElectricalModule() {
        return back;
    }

    @Override
    public Optional<ElectricalNetworkModule> electricalTerminal(Direction side) {
        if (side == facing().getOpposite()) {
            return Optional.of(back);
        }
        if (side == facing()) {
            return Optional.of(front);
        }
        return Optional.empty();
    }

    @Override
    public boolean applyElectricalItemData(TieredElectricalItemData data) {
        return control.applyElectricalItemData(data);
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        return Component.translatable(
                "message.magneticraft.electrical_control_status",
                Component.translatable("block.magneticraft." + kind.id()),
                control.enabled(),
                control.resistanceOhms()
        );
    }

    @Override
    public Component getDisplayName() {
        return Component.translatable("container.magneticraft." + kind.id());
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new ElectricalDeviceMenu(containerId, inventory, this);
    }

    public ElectricalControlKind kind() {
        return kind;
    }

    public ElectricalNetworkModule back() {
        return back;
    }

    public ElectricalNetworkModule front() {
        return front;
    }

    public ElectricalControlModule control() {
        return control;
    }

    private Direction facing() {
        return getBlockState().getValue(ElectricalControlBlock.FACING);
    }

    private static ElectricalControlBlock block(BlockState state) {
        if (!(state.getBlock() instanceof ElectricalControlBlock block)) {
            throw new IllegalArgumentException("Electrical control entity attached to " + state.getBlock());
        }
        return block;
    }
}
