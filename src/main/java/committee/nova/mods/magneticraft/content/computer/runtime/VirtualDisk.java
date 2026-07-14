package committee.nova.mods.magneticraft.content.computer.runtime;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.StringTag;
import net.minecraft.nbt.Tag;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.TreeMap;
import java.util.TreeSet;

/** Bounded in-NBT floppy filesystem; it never touches the host filesystem. */
public final class VirtualDisk {
    public static final int CAPACITY_BYTES = 128 * 1_024;
    public static final int MAX_ENTRIES = 128;
    public static final int MAX_FILE_BYTES = 32 * 1_024;
    public static final int MAX_PATH_LENGTH = 96;
    public static final int MAX_LABEL_LENGTH = 32;

    private static final int SCHEMA_VERSION = 1;
    private static final String SCHEMA_VERSION_TAG = "schema_version";
    private static final String FORMATTED_TAG = "formatted";
    private static final String LABEL_TAG = "label";
    private static final String DIRECTORIES_TAG = "directories";
    private static final String FILES_TAG = "files";
    private static final String PATH_TAG = "path";
    private static final String CONTENT_TAG = "content";

    private final TreeSet<String> directories = new TreeSet<>();
    private final TreeMap<String, String> files = new TreeMap<>();
    private boolean formatted;
    private String label = "Unnamed";

    public VirtualDisk() {
        directories.add("/");
    }

    public boolean formatted() {
        return formatted;
    }

    public String label() {
        return label;
    }

    public void format() {
        directories.clear();
        directories.add("/");
        files.clear();
        formatted = true;
    }

    public boolean setLabel(String newLabel) {
        if (newLabel == null || newLabel.isBlank() || newLabel.length() > MAX_LABEL_LENGTH
                || newLabel.indexOf('\u0000') >= 0) {
            return false;
        }
        label = newLabel;
        return true;
    }

    public int usedBytes() {
        return files.values().stream().mapToInt(value -> value.getBytes(StandardCharsets.UTF_8).length).sum();
    }

    public int freeBytes() {
        return CAPACITY_BYTES - usedBytes();
    }

    public int entryCount() {
        return directories.size() - 1 + files.size();
    }

    public Optional<String> changeDirectory(String currentDirectory, String requestedPath) {
        Optional<String> path = resolve(currentDirectory, requestedPath);
        return path.filter(directories::contains);
    }

    public List<String> list(String currentDirectory, String requestedPath) {
        Optional<String> resolved = resolve(currentDirectory, requestedPath == null || requestedPath.isBlank()
                ? "."
                : requestedPath);
        if (resolved.isEmpty() || !directories.contains(resolved.get())) {
            return List.of();
        }
        String directory = resolved.get();
        String prefix = directory.equals("/") ? "/" : directory + "/";
        List<String> result = new ArrayList<>();
        directories.stream()
                .filter(path -> !path.equals(directory) && parent(path).equals(directory))
                .map(path -> name(path) + "/")
                .forEach(result::add);
        files.keySet().stream()
                .filter(path -> parent(path).equals(directory))
                .map(VirtualDisk::name)
                .forEach(result::add);
        return List.copyOf(result);
    }

    public boolean mkdir(String currentDirectory, String requestedPath) {
        if (!formatted || entryCount() >= MAX_ENTRIES) {
            return false;
        }
        Optional<String> resolved = resolve(currentDirectory, requestedPath);
        if (resolved.isEmpty() || resolved.get().equals("/") || files.containsKey(resolved.get())) {
            return false;
        }
        String path = resolved.get();
        return directories.contains(parent(path)) && directories.add(path);
    }

    public boolean touch(String currentDirectory, String requestedPath) {
        if (!formatted || entryCount() >= MAX_ENTRIES) {
            return false;
        }
        Optional<String> resolved = resolve(currentDirectory, requestedPath);
        if (resolved.isEmpty() || resolved.get().equals("/") || directories.contains(resolved.get())) {
            return false;
        }
        String path = resolved.get();
        if (!directories.contains(parent(path))) {
            return false;
        }
        if (files.containsKey(path)) {
            return true;
        }
        files.put(path, "");
        return true;
    }

    public boolean write(String currentDirectory, String requestedPath, String content) {
        Objects.requireNonNull(content, "content");
        int encodedBytes = content.getBytes(StandardCharsets.UTF_8).length;
        if (!formatted || encodedBytes > MAX_FILE_BYTES || content.indexOf('\u0000') >= 0) {
            return false;
        }
        Optional<String> resolved = resolve(currentDirectory, requestedPath);
        if (resolved.isEmpty() || directories.contains(resolved.get()) || !directories.contains(parent(resolved.get()))) {
            return false;
        }
        String path = resolved.get();
        boolean newFile = !files.containsKey(path);
        if (newFile && entryCount() >= MAX_ENTRIES) {
            return false;
        }
        int previousBytes = newFile ? 0 : files.get(path).getBytes(StandardCharsets.UTF_8).length;
        if (usedBytes() - previousBytes + encodedBytes > CAPACITY_BYTES) {
            return false;
        }
        files.put(path, content);
        return true;
    }

    public Optional<String> read(String currentDirectory, String requestedPath) {
        return resolve(currentDirectory, requestedPath).map(files::get).filter(Objects::nonNull);
    }

    public boolean remove(String currentDirectory, String requestedPath) {
        if (!formatted) {
            return false;
        }
        Optional<String> resolved = resolve(currentDirectory, requestedPath);
        if (resolved.isEmpty() || resolved.get().equals("/")) {
            return false;
        }
        String path = resolved.get();
        if (files.remove(path) != null) {
            return true;
        }
        if (!directories.contains(path)) {
            return false;
        }
        String prefix = path + "/";
        files.keySet().removeIf(candidate -> candidate.startsWith(prefix));
        directories.removeIf(candidate -> candidate.equals(path) || candidate.startsWith(prefix));
        return true;
    }

    public CompoundTag save() {
        CompoundTag tag = new CompoundTag();
        tag.putInt(SCHEMA_VERSION_TAG, SCHEMA_VERSION);
        tag.putBoolean(FORMATTED_TAG, formatted);
        tag.putString(LABEL_TAG, label);
        ListTag encodedDirectories = new ListTag();
        directories.stream().filter(path -> !path.equals("/")).forEach(path -> encodedDirectories.add(StringTag.valueOf(path)));
        tag.put(DIRECTORIES_TAG, encodedDirectories);
        ListTag encodedFiles = new ListTag();
        files.forEach((path, content) -> {
            CompoundTag entry = new CompoundTag();
            entry.putString(PATH_TAG, path);
            entry.putString(CONTENT_TAG, content);
            encodedFiles.add(entry);
        });
        tag.put(FILES_TAG, encodedFiles);
        return tag;
    }

    public boolean restore(CompoundTag tag) {
        Objects.requireNonNull(tag, "tag");
        if (!tag.contains(SCHEMA_VERSION_TAG, Tag.TAG_INT)
                || tag.getInt(SCHEMA_VERSION_TAG) != SCHEMA_VERSION
                || !tag.contains(FORMATTED_TAG, Tag.TAG_BYTE)
                || !tag.contains(LABEL_TAG, Tag.TAG_STRING)
                || !tag.contains(DIRECTORIES_TAG, Tag.TAG_LIST)
                || !tag.contains(FILES_TAG, Tag.TAG_LIST)) {
            return false;
        }
        String restoredLabel = tag.getString(LABEL_TAG);
        ListTag encodedDirectories = tag.getList(DIRECTORIES_TAG, Tag.TAG_STRING);
        ListTag encodedFiles = tag.getList(FILES_TAG, Tag.TAG_COMPOUND);
        if (restoredLabel.isBlank() || restoredLabel.length() > MAX_LABEL_LENGTH
                || encodedDirectories.size() + encodedFiles.size() > MAX_ENTRIES) {
            return false;
        }
        TreeSet<String> restoredDirectories = new TreeSet<>();
        restoredDirectories.add("/");
        for (Tag element : encodedDirectories) {
            String path = element.getAsString();
            if (resolve("/", path).filter(path::equals).isEmpty() || path.equals("/")
                    || !restoredDirectories.add(path)) {
                return false;
            }
        }
        for (String path : restoredDirectories) {
            if (!path.equals("/") && !restoredDirectories.contains(parent(path))) {
                return false;
            }
        }
        TreeMap<String, String> restoredFiles = new TreeMap<>();
        int used = 0;
        for (Tag element : encodedFiles) {
            CompoundTag entry = (CompoundTag) element;
            if (!entry.contains(PATH_TAG, Tag.TAG_STRING) || !entry.contains(CONTENT_TAG, Tag.TAG_STRING)) {
                return false;
            }
            String path = entry.getString(PATH_TAG);
            String content = entry.getString(CONTENT_TAG);
            int bytes = content.getBytes(StandardCharsets.UTF_8).length;
            used += bytes;
            if (resolve("/", path).filter(path::equals).isEmpty()
                    || !restoredDirectories.contains(parent(path))
                    || restoredDirectories.contains(path)
                    || restoredFiles.put(path, content) != null
                    || bytes > MAX_FILE_BYTES
                    || used > CAPACITY_BYTES) {
                return false;
            }
        }
        directories.clear();
        directories.addAll(restoredDirectories);
        files.clear();
        files.putAll(restoredFiles);
        formatted = tag.getBoolean(FORMATTED_TAG);
        label = restoredLabel;
        return true;
    }

    public VirtualDisk copy() {
        VirtualDisk copy = new VirtualDisk();
        if (!copy.restore(save())) {
            throw new IllegalStateException("Failed to copy a validated virtual disk");
        }
        return copy;
    }

    private static Optional<String> resolve(String currentDirectory, String requestedPath) {
        if (requestedPath == null || requestedPath.isBlank() || requestedPath.indexOf('\u0000') >= 0) {
            return Optional.empty();
        }
        String base = requestedPath.startsWith("/") ? requestedPath : currentDirectory + "/" + requestedPath;
        List<String> parts = new ArrayList<>();
        for (String part : base.replace('\\', '/').split("/+")) {
            if (part.isEmpty() || part.equals(".")) {
                continue;
            }
            if (part.equals("..")) {
                if (parts.isEmpty()) {
                    return Optional.empty();
                }
                parts.remove(parts.size() - 1);
                continue;
            }
            if (!part.matches("[A-Za-z0-9._-]{1,32}")) {
                return Optional.empty();
            }
            parts.add(part);
        }
        String path = parts.isEmpty() ? "/" : "/" + String.join("/", parts);
        return path.length() <= MAX_PATH_LENGTH ? Optional.of(path) : Optional.empty();
    }

    private static String parent(String path) {
        int separator = path.lastIndexOf('/');
        return separator <= 0 ? "/" : path.substring(0, separator);
    }

    private static String name(String path) {
        int separator = path.lastIndexOf('/');
        return separator < 0 ? path : path.substring(separator + 1);
    }
}
