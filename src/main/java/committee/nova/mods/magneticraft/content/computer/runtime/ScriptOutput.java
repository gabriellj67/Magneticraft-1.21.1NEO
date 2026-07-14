package committee.nova.mods.magneticraft.content.computer.runtime;

final class ScriptOutput {
    private final StringBuilder value = new StringBuilder();

    void append(String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        value.append(text);
        trim();
    }

    void appendLine(String text) {
        append(text);
        append("\n");
    }

    void restore(String text) {
        value.setLength(0);
        append(text);
    }

    void clear() {
        value.setLength(0);
    }

    String value() {
        return value.toString();
    }

    private void trim() {
        int overflow = value.length() - ScriptRuntime.MAX_OUTPUT_CHARACTERS;
        if (overflow > 0) {
            value.delete(0, overflow);
        }
    }
}
