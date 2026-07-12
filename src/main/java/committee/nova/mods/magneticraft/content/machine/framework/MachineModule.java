package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.util.LazyOptional;
import org.jetbrains.annotations.Nullable;

/**
 * A composable, independently persisted machine responsibility.
 */
public interface MachineModule {
    ResourceLocation id();

    default void load(CompoundTag tag) {
    }

    default void save(CompoundTag tag) {
    }

    default void loadClientData(CompoundTag tag) {
    }

    default void saveClientData(CompoundTag tag) {
    }

    default void serverTick() {
    }

    default void onLoad() {
    }

    default void invalidateCapabilities() {
    }

    default void reviveCapabilities() {
    }

    default <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return LazyOptional.empty();
    }
}
