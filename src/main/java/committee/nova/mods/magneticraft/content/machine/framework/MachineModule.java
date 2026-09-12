package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;

/**
 * A composable, independently persisted machine responsibility.
 */
public interface MachineModule {
    ResourceLocation id();

    /** Schema owned by this module's namespaced persistence payload. */
    default int persistenceSchemaVersion() {
        return 1;
    }

    /**
     * Allows a module to opt into explicit migration from an older payload.
     */
    default boolean canLoadPersistenceSchema(int schemaVersion) {
        return schemaVersion == persistenceSchemaVersion();
    }

    default void load(CompoundTag tag, HolderLookup.Provider registries) {
    }

    default void save(CompoundTag tag, HolderLookup.Provider registries) {
    }

    /**
     * Restores this module to the defaults represented by an empty payload.
     * Persistence routing calls this instead of exposing incompatible or missing payloads.
     */
    default void resetPersistentState(HolderLookup.Provider registries) {
        load(new CompoundTag(), registries);
    }

    default void loadClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    default void saveClientData(CompoundTag tag, HolderLookup.Provider registries) {
    }

    default void serverTick() {
    }

    default void onLoad() {
    }

    /**
     * Called when the owning block entity is removed or its chunk unloads.
     * Implementations must be idempotent because the same removal may
     * invalidate other lifecycle surfaces in the same pass.
     */
    default void onUnload() {
    }
}
