package committee.nova.mods.magneticraft.content.computer;

/** Original Magneticraft floppy-disk variants and their historical texture indices. */
public enum FloppyDiskVisualVariant {
    USER("user", 0),
    LISP("lisp", 1),
    FORTH("forth", 2),
    SHELL("shell", 3),
    BASIC("basic", 4),
    EDITOR("editor", 5),
    ASM("asm", 6);

    private final String preset;
    private final int textureIndex;

    FloppyDiskVisualVariant(String preset, int textureIndex) {
        this.preset = preset;
        this.textureIndex = textureIndex;
    }

    public String preset() {
        return preset;
    }

    public int textureIndex() {
        return textureIndex;
    }

    public static FloppyDiskVisualVariant fromPreset(String preset) {
        if (preset == null) {
            return USER;
        }
        return switch (preset) {
            case "lisp" -> LISP;
            case "forth" -> FORTH;
            case "shell" -> SHELL;
            case "basic" -> BASIC;
            case "editor" -> EDITOR;
            case "asm" -> ASM;
            default -> USER;
        };
    }
}
