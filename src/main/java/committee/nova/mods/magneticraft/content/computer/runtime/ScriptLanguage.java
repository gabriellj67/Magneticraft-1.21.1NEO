package committee.nova.mods.magneticraft.content.computer.runtime;

import java.util.Locale;
import java.util.Optional;

/** Released script environments backed by the bounded Java runtime. */
public enum ScriptLanguage {
    FORTH,
    LISP,
    SHELL;

    public String serializedName() {
        return name().toLowerCase(Locale.ROOT);
    }

    public static Optional<ScriptLanguage> parse(String name) {
        if (name == null) {
            return Optional.empty();
        }
        for (ScriptLanguage language : values()) {
            if (language.serializedName().equals(name.toLowerCase(Locale.ROOT))) {
                return Optional.of(language);
            }
        }
        return Optional.empty();
    }
}
