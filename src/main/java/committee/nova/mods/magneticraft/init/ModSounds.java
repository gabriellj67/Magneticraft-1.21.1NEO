package committee.nova.mods.magneticraft.init;

import committee.nova.mods.magneticraft.Magneticraft;
import net.minecraft.sounds.SoundEvent;
import net.minecraftforge.registries.RegistryObject;

/**
 * Crushing-table sound events.
 */
public final class ModSounds {
    public static final RegistryObject<SoundEvent> CRUSHING_HIT = register("crushing_table_hit");
    public static final RegistryObject<SoundEvent> CRUSHING_FINAL = register("crushing_table_complete");

    private ModSounds() {
    }

    public static void bootstrap() {
    }

    private static RegistryObject<SoundEvent> register(String id) {
        return ModRegistries.SOUND_EVENTS.register(
                id,
                () -> SoundEvent.createVariableRangeEvent(Magneticraft.id(id))
        );
    }
}
