package committee.nova.mods.magneticraft.content.machine.framework;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Loader-independent module identity and NBT routing.
 */
public final class MachineModuleContainer {
    static final String SCHEMA_VERSION_TAG = "schema_version";

    private final Map<ResourceLocation, MachineModule> modules = new LinkedHashMap<>();

    public <T extends MachineModule> T add(T module) {
        MachineModule previous = modules.putIfAbsent(module.id(), module);
        if (previous != null) {
            throw new IllegalArgumentException("Duplicate machine module id: " + module.id());
        }
        return module;
    }

    public CompoundTag save(HolderLookup.Provider registries) {
        CompoundTag root = new CompoundTag();
        modules.forEach((id, module) -> {
            CompoundTag moduleTag = new CompoundTag();
            module.save(moduleTag, registries);
            moduleTag.putInt(SCHEMA_VERSION_TAG, module.persistenceSchemaVersion());
            root.put(id.toString(), moduleTag);
        });
        return root;
    }

    public void load(CompoundTag root, HolderLookup.Provider registries) {
        modules.forEach((id, module) -> {
            String key = id.toString();
            if (!root.contains(key, Tag.TAG_COMPOUND)) {
                module.resetPersistentState(registries);
                return;
            }
            CompoundTag moduleTag = root.getCompound(key);
            if (!moduleTag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                    || !module.canLoadPersistenceSchema(moduleTag.getInt(SCHEMA_VERSION_TAG))) {
                module.resetPersistentState(registries);
                return;
            }
            module.load(moduleTag, registries);
        });
    }

    void resetPersistentState(HolderLookup.Provider registries) {
        modules.values().forEach(module -> module.resetPersistentState(registries));
    }

    public Collection<MachineModule> values() {
        return Collections.unmodifiableCollection(modules.values());
    }
}
