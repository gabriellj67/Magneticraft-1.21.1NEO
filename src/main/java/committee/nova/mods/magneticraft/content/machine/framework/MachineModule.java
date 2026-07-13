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

    /** Schema owned by this module's namespaced persistence payload. */
    default int persistenceSchemaVersion() {
        return 1;
    }

    default void load(CompoundTag tag) {
    }

    default void save(CompoundTag tag) {
    }

    /**
     * Restores this module to the defaults represented by an empty payload.
     * Persistence routing calls this instead of exposing incompatible or missing payloads.
     */
    default void resetPersistentState() {
        load(new CompoundTag());
    }

    default void loadClientData(CompoundTag tag) {
    }

    default void saveClientData(CompoundTag tag) {
    }

    default void serverTick() {
    }

    default void onLoad() {
    }

    /**
     * Called when the owning block entity is removed or its chunk unloads.
     * Implementations must be idempotent because Forge may invalidate other
     * lifecycle surfaces during the same removal.
     */
    default void onUnload() {
    }

    default void invalidateCapabilities() {
    }

    default void reviveCapabilities() {
    }

    default <T> LazyOptional<T> getCapability(Capability<T> capability, @Nullable Direction side) {
        return LazyOptional.empty();
    }
}
