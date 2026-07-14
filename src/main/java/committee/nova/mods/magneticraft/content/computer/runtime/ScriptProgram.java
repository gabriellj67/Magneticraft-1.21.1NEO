package committee.nova.mods.magneticraft.content.computer.runtime;

import java.nio.charset.StandardCharsets;
import java.util.Objects;

/** Immutable source payload accepted by computers, robots and floppy disks. */
public record ScriptProgram(ScriptLanguage language, String source) {
    public ScriptProgram {
        Objects.requireNonNull(language, "language");
        Objects.requireNonNull(source, "source");
        if (source.indexOf('\u0000') >= 0) {
            throw new IllegalArgumentException("Script source contains a NUL character");
        }
        int bytes = source.getBytes(StandardCharsets.UTF_8).length;
        if (bytes > ScriptRuntime.MAX_SOURCE_BYTES) {
            throw new IllegalArgumentException("Script source exceeds " + ScriptRuntime.MAX_SOURCE_BYTES + " bytes");
        }
    }

    public int encodedSize() {
        return source.getBytes(StandardCharsets.UTF_8).length;
    }
}
