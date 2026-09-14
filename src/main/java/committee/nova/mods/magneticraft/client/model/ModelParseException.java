package committee.nova.mods.magneticraft.client.model;

/** Checked format error with the originating model resource and field path. */
public final class ModelParseException extends Exception {
    public ModelParseException(String resource, String path, String message) {
        super(resource + " at " + path + ": " + message);
    }

    public ModelParseException(String resource, String path, String message, Throwable cause) {
        super(resource + " at " + path + ": " + message, cause);
    }
}
