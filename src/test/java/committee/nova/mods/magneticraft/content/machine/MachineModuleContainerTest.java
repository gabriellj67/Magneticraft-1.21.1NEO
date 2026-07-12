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
    void stableIdsRouteNbtAndUnknownOrMissingNodesAreIgnored() {
        ResourceLocation id = Magneticraft.id("test_module");
        MachineModuleContainer modules = new MachineModuleContainer();
        CountingModule module = modules.add(new CountingModule(id));

        CompoundTag saved = modules.save();
        assertTrue(saved.contains(id.toString()));
        assertEquals(42, saved.getCompound(id.toString()).getInt("value"));
        assertThrows(IllegalArgumentException.class, () -> modules.add(new CountingModule(id)));

        CompoundTag unknownOnly = new CompoundTag();
        unknownOnly.put("other:unknown", new CompoundTag());
        modules.load(unknownOnly);
        assertEquals(0, module.loadCount, "Missing known nodes must retain module defaults");

        modules.load(saved);
        assertEquals(1, module.loadCount);
        assertEquals(42, module.loadedValue);
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
