package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.ElectricalProtectionModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalHost;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalPlacementHost;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalSideHost;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.init.ModNetworkBlocks;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.item.TieredElectricalItemData;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.Containers;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Two isolated terminals bridged only by the protection module's explicit internal edge. */
public final class ElectricalProtectionBlockEntity extends NetworkComponentBlockEntity
        implements TieredElectricalHost, TieredElectricalSideHost, TieredElectricalPlacementHost {
    public static final net.minecraft.resources.ResourceLocation INPUT_TERMINAL = Magneticraft.id("input");
    public static final net.minecraft.resources.ResourceLocation OUTPUT_TERMINAL = Magneticraft.id("output");

    private final ElectricalProtectionKind kind;
    private final ElectricalNetworkModule input;
    private final ElectricalNetworkModule output;
    private final ElectricalProtectionModule protection;

    public ElectricalProtectionBlockEntity(BlockPos position, BlockState state) {
        super(type(state), position, state);
        kind = block(state).kind();
        input = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_input"),
                this,
                VoltageTierIds.LOW,
                INPUT_TERMINAL,
                ElectricalNodeKind.MACHINE,
                side -> side == facing().getOpposite()
        ));
        output = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_output"),
                this,
                VoltageTierIds.LOW,
                OUTPUT_TERMINAL,
                ElectricalNodeKind.MACHINE,
                side -> side == facing()
        ));
        protection = addModule(new ElectricalProtectionModule(
                Magneticraft.id("electrical_protection"),
                this,
                input,
                output,
                kind
        ));
    }

    @Override
    public ElectricalNetworkModule tieredElectricalModule() {
        return input;
    }

    @Override
    public Optional<ElectricalNetworkModule> electricalTerminal(Direction side) {
        if (side == facing().getOpposite()) {
            return Optional.of(input);
        }
        if (side == facing()) {
            return Optional.of(output);
        }
        return Optional.empty();
    }

    @Override
    public boolean applyElectricalItemData(TieredElectricalItemData data) {
        return protection.applyElectricalItemData(data);
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        return Component.translatable(
                "message.magneticraft.protection_status",
                protection.ratingId(),
                protection.thermalStress(),
                protection.tripped() || protection.blown() || protection.redstoneForcedOpen()
        );
    }

    @Override
    public void dropContents(Level level) {
        ItemStack fuse = protection.removeFuseForDrop();
        if (!fuse.isEmpty() && !level.isClientSide) {
            Containers.dropItemStack(
                    level,
                    getBlockPos().getX() + 0.5D,
                    getBlockPos().getY() + 0.5D,
                    getBlockPos().getZ() + 0.5D,
                    fuse
            );
        }
    }

    public ElectricalProtectionModule protection() {
        return protection;
    }

    public ElectricalNetworkModule input() {
        return input;
    }

    public ElectricalNetworkModule output() {
        return output;
    }

    private Direction facing() {
        return getBlockState().getValue(ElectricalProtectionBlock.FACING);
    }

    private static ElectricalProtectionBlock block(BlockState state) {
        if (!(state.getBlock() instanceof ElectricalProtectionBlock block)) {
            throw new IllegalArgumentException("Electrical protection entity attached to " + state.getBlock());
        }
        return block;
    }

    private static BlockEntityType<?> type(BlockState state) {
        return block(state).kind() == ElectricalProtectionKind.FUSE_BOX
                ? ModBlockEntities.FUSE_BOX.get()
                : ModBlockEntities.CIRCUIT_BREAKER.get();
    }
}
