package committee.nova.mods.magneticraft.content.nuclear.radiation;

import committee.nova.mods.magneticraft.api.nuclear.radiation.RadiationSource;
import committee.nova.mods.magneticraft.content.machine.framework.MachineBlockEntity;
import committee.nova.mods.magneticraft.init.ModBlockEntities;
import committee.nova.mods.magneticraft.system.nuclear.data.ReactorParameterRegistry;
import committee.nova.mods.magneticraft.system.nuclear.radiation.RadiationSourceRegistry;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

/** Schema-backed intensity for debris/corium blocks. Removal immediately removes their hazard. */
public final class RadioactiveSourceBlockEntity extends MachineBlockEntity implements RadiationSource {
    private static final int SCHEMA_VERSION = 1;
    private double intensity = 1.0D;

    public RadioactiveSourceBlockEntity(BlockPos position, BlockState state) {
        super(ModBlockEntities.RADIOACTIVE_SOURCE.get(), position, state);
    }

    public static void serverTick(Level level, BlockPos position, BlockState state,
                                  RadioactiveSourceBlockEntity source) {
        source.finishServerTick();
    }

    public void setIntensity(double intensity) {
        this.intensity = Math.max(0.05D, Math.min(10.0D, intensity));
        markChangedAndSync();
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level instanceof ServerLevel serverLevel) {
            RadiationSourceRegistry.register(serverLevel, worldPosition);
        }
    }

    @Override
    public void setRemoved() {
        if (level instanceof ServerLevel serverLevel) {
            RadiationSourceRegistry.unregister(serverLevel, worldPosition);
        }
        super.setRemoved();
    }

    @Override public BlockPos radiationOrigin() { return worldPosition; }

    @Override
    public double doseRateMillisievertsPerHour() {
        var parameters = ReactorParameterRegistry.INSTANCE.current().parameters();
        return intensity * (kind() == RadioactiveSourceKind.CORIUM
                ? parameters.coriumDoseRateMillisievertsPerHour()
                : parameters.contaminationDoseRateMillisievertsPerHour());
    }

    @Override public boolean contaminationSource() { return true; }

    @Override
    protected void saveMachineData(CompoundTag tag) {
        tag.putInt("schema_version", SCHEMA_VERSION);
        tag.putDouble("intensity", intensity);
    }

    @Override
    protected void loadMachineData(CompoundTag tag) {
        intensity = tag.getInt("schema_version") == SCHEMA_VERSION
                && Double.isFinite(tag.getDouble("intensity"))
                ? Math.max(0.05D, Math.min(10.0D, tag.getDouble("intensity"))) : 1.0D;
    }

    private RadioactiveSourceKind kind() {
        return getBlockState().getBlock() instanceof RadioactiveSourceBlock block
                ? block.kind() : RadioactiveSourceKind.DEBRIS;
    }
}
