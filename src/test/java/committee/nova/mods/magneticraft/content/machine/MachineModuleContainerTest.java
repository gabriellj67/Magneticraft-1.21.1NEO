package committee.nova.mods.magneticraft.content.machine;

import committee.nova.mods.magneticraft.Magneticraft;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModule;
import committee.nova.mods.magneticraft.content.machine.framework.MachineModuleContainer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class MachineModuleContainerTest {
    @Test
    void stableIdsRouteOnlyCurrentSchemaAndResetMissingOrIncompatibleNodes() {
        ResourceLocation id = Magneticraft.id("test_module");
        MachineModuleContainer modules = new MachineModuleContainer();
        CountingModule module = modules.add(new CountingModule(id));

        CompoundTag saved = modules.save();
        assertTrue(saved.contains(id.toString()));
        CompoundTag savedModule = saved.getCompound(id.toString());
        assertEquals(1, savedModule.getInt("schema_version"));
        assertEquals(42, savedModule.getInt("value"));
        assertThrows(IllegalArgumentException.class, () -> modules.add(new CountingModule(id)));

        modules.load(saved);
        assertEquals(1, module.loadCount);
        assertEquals(42, module.loadedValue);

        CompoundTag unknownOnly = new CompoundTag();
        unknownOnly.put("other:unknown", new CompoundTag());
        modules.load(unknownOnly);
        assertEquals(2, module.loadCount);
        assertEquals(0, module.loadedValue, "Missing known nodes must reset to defaults");

        CompoundTag legacyRoot = new CompoundTag();
        CompoundTag legacyModule = new CompoundTag();
        legacyModule.putInt("value", 99);
        legacyRoot.put(id.toString(), legacyModule);
        modules.load(legacyRoot);
        assertEquals(3, module.loadCount);
        assertEquals(0, module.loadedValue, "Missing schema version must not expose the legacy payload");

        CompoundTag futureRoot = saved.copy();
        futureRoot.getCompound(id.toString()).putInt("schema_version", 2);
        futureRoot.getCompound(id.toString()).putInt("value", 99);
        modules.load(futureRoot);
        assertEquals(4, module.loadCount);
        assertEquals(0, module.loadedValue, "Unsupported schema version must reset to defaults");
    }

    private static final class CountingModule implements MachineModule {
        private final ResourceLocation id;
        private int loadCount;
        private int loadedValue;

        private CountingModule(ResourceLocation id) {
            this.id = id;
        }

        @Override
        public ResourceLocation id() {
            return id;
        }

        @Override
        public void load(CompoundTag tag) {
            loadCount++;
            loadedValue = tag.getInt("value");
        }

        @Override
        public void save(CompoundTag tag) {
            tag.putInt("value", 42);
        }
    }
}
