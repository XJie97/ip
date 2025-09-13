package tkit;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsulates reading from and writing to an OS-independent relative file path.
 * Format (pipe-delimited with isEscaping):
 *   TYPE | DONE | DESCRIPTION | OTHER...
 *   T | 1 | read book
 *   D | 0 | return book | 2019-12-02T18:00
 *   E | 0 | project meeting | 2019-12-02T14:00 | 2019-12-02T16:00
 * isEscaping rules:
 *   {@code \|} represents a literal pipe within a field
 *   {@code \\} represents a literal backslash
 * Corrupted lines are skipped without aborting, but counted for diagnostics.
 */
final class Storage {

    /** Line prefix used for user-readable comments in temp writes. */
    private static final String HEADER_PREFIX = "#";

    /** Relative, OS-independent data file path. */
    private final Path dataFile = Path.of("data", "Tkit.txt");

    /**
     * Loads tasks from disk. Creates parent directory if missing; ignores corrupted lines.
     *
     * @return list of tasks parsed from the data file
     */
    public List<Task> load() {
        ensureParentDir();

        if (!Files.exists(dataFile)) {
            return new ArrayList<>();
        }

        List<Task> loaded = new ArrayList<>();
        int corruptedCount = 0;

        try (BufferedReader reader = Files.newBufferedReader(dataFile, StandardCharsets.UTF_8)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String trimmed = line.trim();
                if (trimmed.isEmpty() || trimmed.startsWith(HEADER_PREFIX)) {
                    continue;
                }
                try {
                    Task t = decodeLine(trimmed);
                    if (t != null) {
                        loaded.add(t);
                    } else {
                        corruptedCount++;
                    }
                } catch (Exception ex) {
                    corruptedCount++;
                }
            }
        } catch (IOException io) {
            return new ArrayList<>();
        }

        if (corruptedCount > 0) {
            System.out.println("____________________\n");
            System.out.println("Warning: "
                    + corruptedCount
                    + " corrupted line(s) ignored while loading.");
            System.out.println("____________________\n");
        }

        return loaded;
    }

    /**
     * Persists all tasks to disk. Writes to a temporary file first and then moves it into place atomically
     * (with a non-atomic fallback) to minimize the risk of partial writes.
     *
     * @param tasks the current snapshot of tasks to save; must not be {@code null}
     */

    public void save(final List<Task> tasks) {
        assert tasks != null : "save(): tasks must not be null";

        ensureParentDir();
        final Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");

        if (!writeSnapshot(tmp, tasks)) {
            return;
        }

        finalizeSave(tmp);
    }

    /**
     * Writes a header and all tasks to the given temporary file using UTF-8 encoding.
     * Logs a warning and returns {@code false} on failure; otherwise returns {@code true}.
     *
     * @param tmp   the path to the temporary file that will hold the snapshot
     * @param tasks the tasks to be serialized into the file
     * @return {@code true} if the snapshot was written successfully; {@code false} otherwise
     */
    private boolean writeSnapshot(final Path tmp, final List<Task> tasks) {
        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            writer.write(HEADER_PREFIX + " Tkit save @ " + LocalDateTime.now());
            writer.newLine();
            for (Task t : tasks) {
                writer.write(encodeTask(t));
                writer.newLine();
            }
            return true;
        } catch (IOException io) {
            System.out.println("____________________\n");
            System.out.println("Warning: failed to write data file: " + io.getMessage());
            System.out.println("____________________\n");
            return false;
        }
    }

    /**
     * Moves the temporary snapshot file into place as the main data file. Attempts an atomic move first;
     * if the filesystem does not support it, falls back to a non-atomic replace. Logs warnings on failure.
     *
     * @param tmp the path to the temporary file to move into place
     */
    private void finalizeSave(final Path tmp) {
        try {
            Files.move(
                    tmp,
                    dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE
            );
        } catch (AtomicMoveNotSupportedException am) {
            try {
                Files.move(tmp, dataFile, StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException io) {
                System.out.println("____________________\n");
                System.out.println("Warning: failed to finalize data file: " + io.getMessage());
                System.out.println("____________________\n");
            }
        } catch (IOException io) {
            System.out.println("____________________\n");
            System.out.println("Warning: failed to finalize data file: " + io.getMessage());
            System.out.println("____________________\n");
        }
    }


    /** Ensures parent directory exists; creates it if missing. */
    private void ensureParentDir() {

        try {
            Path parent = dataFile.getParent();
            assert parent != null : "Data file must have a parent directory";
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
            // intentional no-op: directory creation is best-effort and failure is non-fatal
        }
    }

    /** Serializes a task into a single line. */
    private String encodeTask(Task t) {
        assert t != null : "encodeTask(): task is null";
        assert t.type != null && t.status != null
                && t.description != null : "encodeTask(): fields must be non-null";

        String doneFlag = (t.status == Status.DONE) ? "1" : "0";
        String type = t.type.tag();

        StringBuilder sb = new StringBuilder();
        sb.append(type)
                .append(" | ")
                .append(doneFlag)
                .append(" | ")
                .append(escape(t.description));

        if (t instanceof Deadline) {
            Deadline d = (Deadline) t;
            sb.append(" | ")
                    .append(escape(DateTimeUtil.toStorage(d.getDueDate())));
        } else if (t instanceof Event) {
            Event e = (Event) t;
            sb.append(" | ")
                    .append(escape(DateTimeUtil.toStorage(e.getFromDate())))
                    .append(" | ")
                    .append(escape(DateTimeUtil.toStorage(e.getToDate())));
        }

        return sb.toString();
    }

    /**
     * Deserializes a line into a task.
     *
     * @param line encoded line
     * @return constructed task or {@code null} if corrupted
     */
    private Task decodeLine(String line) {
        assert line != null && !line.isBlank() : "decodeLine(): empty line";
        List<String> rawFields = splitPreservingEscapes(line);
        if (rawFields.size() < 3) {
            return null;
        }

        String type = rawFields.get(0).trim();
        String done = rawFields.get(1).trim();

        for (int i = 0; i < rawFields.size(); i++) {
            rawFields.set(i, unescape(rawFields.get(i).trim()));
        }

        String description = rawFields.get(2);
        Task task;

        switch (type) {
        case "T":
            task = new Todo(description);
            break;
        case "D":
            if (rawFields.size() < 4) {
                return null;
            }
            LocalDateTime by = DateTimeUtil.tryParseStorageOrInput(rawFields.get(3));
            if (by == null) {
                return null;
            }
            task = new Deadline(description, by);
            break;
        case "E":
            if (rawFields.size() < 5) {
                return null;
            }
            LocalDateTime from = DateTimeUtil.tryParseStorageOrInput(rawFields.get(3));
            LocalDateTime to = DateTimeUtil.tryParseStorageOrInput(rawFields.get(4));
            if (from == null || to == null) {
                return null;
            }
            task = new Event(description, from, to);
            break;
        default:
            return null;
        }

        if ("1".equals(done)) {
            task.markAsDone();
        } else if (!"0".equals(done)) {
            return null;
        }

        return task;
    }

    /** Escapes literal backslashes and pipes within a field. */
    private static String escape(String s) {
        assert s != null : "escape(): null";
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }

    /** Reverses {@link #escape(String)} on a field. */
    private static String unescape(String s) {
        assert s != null : "unescape(): null";
        StringBuilder out = new StringBuilder();
        boolean isEscaping = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isEscaping) {
                out.append(c);
                isEscaping = false;
            } else if (c == '\\') {
                isEscaping = true;
            } else {
                out.append(c);
            }
        }
        if (isEscaping) {
            out.append('\\');
        }
        return out.toString();
    }

    /** Splits a line by unescaped {@code |}, preserving escaped separators. */
    private static List<String> splitPreservingEscapes(String line) {
        assert line != null : "splitPreservingEscapes(): null";
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean isEscaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (isEscaping) {
                current.append(c);
                isEscaping = false;
                continue;
            }

            if (c == '\\') {
                isEscaping = true;
                continue;
            }

            if (c == '|') {
                fields.add(current.toString());
                current.setLength(0);
                continue;
            }

            current.append(c);
        }
        fields.add(current.toString());
        return fields;
    }
}
