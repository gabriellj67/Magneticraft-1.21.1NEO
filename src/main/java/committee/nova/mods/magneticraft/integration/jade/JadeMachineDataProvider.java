package committee.nova.mods.magneticraft.integration.jade;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.observation.MachineObservation;
import committee.nova.mods.magneticraft.content.machine.observation.MachineObservationCodec;
import committee.nova.mods.magneticraft.content.machine.observation.MachineObservationService;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.entity.BlockEntity;
import snownee.jade.api.BlockAccessor;
import snownee.jade.api.IServerDataProvider;

enum JadeMachineDataProvider implements IServerDataProvider<BlockAccessor> {
    INSTANCE;

    private static final ResourceLocation UID = Magneticraft.id("machine_status");

    @Override
    public void appendServerData(CompoundTag data, BlockAccessor accessor) {
        BlockEntity blockEntity = accessor.getBlockEntity();
        if (blockEntity == null || blockEntity.getLevel() == null || blockEntity.getLevel().isClientSide) {
            return;
        }
        MachineObservation observation = MachineObservationService.observe(blockEntity, accessor.getSide());
        MachineObservationCodec.write(data, observation);
    }

    @Override
    public ResourceLocation getUid() {
        return UID;
    }
}
