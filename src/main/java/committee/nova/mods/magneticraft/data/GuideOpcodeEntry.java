package committee.nova.mods.magneticraft.data;

/**
 * Stable, loader-independent projection of one programmable-machine opcode.
 * The VM owns execution; the guide provider receives only this display data.
 */
public record GuideOpcodeEntry(
        String id,
        int code,
        int operandCount,
        String descriptionTranslationKey
) {
    public GuideOpcodeEntry {
        if (id == null || id.isBlank()) {
            throw new IllegalArgumentException("Opcode ID must not be blank");
        }
        if (code < 0 || code > 255) {
            throw new IllegalArgumentException("Opcode code must be between 0 and 255: " + id);
        }
        if (operandCount < 0 || operandCount > 3) {
            throw new IllegalArgumentException("Opcode operand count must be between 0 and 3: " + id);
        }
        if (descriptionTranslationKey == null || descriptionTranslationKey.isBlank()) {
            throw new IllegalArgumentException("Opcode description key must not be blank: " + id);
        }
    }
}
