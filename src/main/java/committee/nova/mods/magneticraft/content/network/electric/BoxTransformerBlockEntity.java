package committee.nova.mods.magneticraft.content.network.electric;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.network.block.NetworkComponentBlockEntity;
import committee.nova.mods.magneticraft.content.network.module.ElectricalNetworkModule;
import committee.nova.mods.magneticraft.content.network.module.TieredElectricalSideHost;
import committee.nova.mods.magneticraft.content.network.module.TransformerCouplerModule;
import committee.nova.mods.magneticraft.content.network.module.TransformerElectricalHost;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.network.electric.ElectricalNodeKind;
import committee.nova.mods.magneticraft.system.network.electric.profile.TransformerProfileIds;
import committee.nova.mods.magneticraft.system.network.electric.profile.VoltageTierIds;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.level.block.state.BlockState;

import java.util.Optional;

/** Single-block transformer with physically isolated rear input and front output terminals. */
public final class BoxTransformerBlockEntity extends NetworkComponentBlockEntity
        implements TransformerElectricalHost, TieredElectricalSideHost {
    private final ElectricalNetworkModule input;
    private final ElectricalNetworkModule output;
    private final TransformerCouplerModule transformer;

    public BoxTransformerBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.BOX_TRANSFORMER.get(), position, state);
        input = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_input"),
                this,
                VoltageTierIds.LOW,
                ElectricPoleBlockEntity.INPUT_TERMINAL,
                ElectricalNodeKind.MACHINE,
                side -> side == facing().getOpposite()
        ));
        output = addModule(new ElectricalNetworkModule(
                Magneticraft.id("electricity_output"),
                this,
                VoltageTierIds.MEDIUM,
                ElectricPoleBlockEntity.OUTPUT_TERMINAL,
                ElectricalNodeKind.MACHINE,
                side -> side == facing()
        ));
        transformer = addModule(new TransformerCouplerModule(
                Magneticraft.id("transformer_coupler"),
                this,
                input,
                output,
                TransformerProfileIds.LV_TO_MV
        ));
    }

    @Override
    public TransformerCouplerModule transformerCoupler() {
        return transformer;
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

    public ElectricalNetworkModule input() {
        return input;
    }

    public ElectricalNetworkModule output() {
        return output;
    }

    @Override
    public Component configure(Direction side, boolean secondaryAction) {
        if (secondaryAction) {
            transformer.tryReverse();
        } else {
            transformer.cycleRedstoneMode();
        }
        return Component.translatable(
                "message.magneticraft.transformer_status",
                transformer.profileId(),
                transformer.reversed(),
                transformer.redstoneMode().name()
        );
    }

    private Direction facing() {
        return getBlockState().getValue(BoxTransformerBlock.FACING);
    }
}
