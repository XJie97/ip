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
 *
 * Format (pipe-delimited with escaping):
 *   TYPE | DONE | DESCRIPTION | OTHER...
 *   T | 1 | read book
 *   D | 0 | return book | 2019-12-02T18:00
 *   E | 0 | project meeting | 2019-12-02T14:00 | 2019-12-02T16:00
 * Escaping rules:
 *   {@code \|} represents a literal pipe within a field
 *   {@code \\} represents a literal backslash
 * Corrupted lines are skipped silently but counted for diagnostics.
 */
final class Storage {

    /** Relative, OS-independent data file path. */
    private final Path dataFile = Path.of("data", "Tkit.txt");

    /** Line prefix used for user-readable comments in temp writes. */
    private static final String HEADER_PREFIX = "#";

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
                    // skip blank lines and comments
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
     * Persists all tasks to disk using a temp file then an atomic move (with non-atomic fallback).
     *
     * @param tasks current snapshot of tasks to persist
     */
    public void save(List<Task> tasks) {
        ensureParentDir();
        Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");

        try (BufferedWriter writer = Files.newBufferedWriter(tmp, StandardCharsets.UTF_8)) {
            writer.write(HEADER_PREFIX + " Tkit save @ " + LocalDateTime.now());
            writer.newLine();

            for (Task t : tasks) {
                writer.write(encodeTask(t));
                writer.newLine();
            }
        } catch (IOException io) {
            System.out.println("____________________\n");
            System.out.println("Warning: failed to write data file: " + io.getMessage());
            System.out.println("____________________\n");
            return;
        }

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
            if (parent != null && !Files.exists(parent)) {
                Files.createDirectories(parent);
            }
        } catch (IOException ignored) {
        }
    }

    /** Serializes a task into a single line. */
    private String encodeTask(Task t) {
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
                    .append(escape(DateTimeUtil.toStorage(d.getDueAt())));
        } else if (t instanceof Event) {
            Event e = (Event) t;
            sb.append(" | ")
                    .append(escape(DateTimeUtil.toStorage(e.getFrom())))
                    .append(" | ")
                    .append(escape(DateTimeUtil.toStorage(e.getTo())));
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
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }

    /** Reverses {@link #escape(String)} on a field. */
    private static String unescape(String s) {
        StringBuilder out = new StringBuilder();
        boolean escaping = false;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (escaping) {
                out.append(c);
                escaping = false;
            } else if (c == '\\') {
                escaping = true;
            } else {
                out.append(c);
            }
        }
        if (escaping) {
            out.append('\\');
        }
        return out.toString();
    }

    /** Splits a line by unescaped {@code |}, preserving escaped separators. */
    private static List<String> splitPreservingEscapes(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean escaping = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (escaping) {
                current.append(c);
                escaping = false;
                continue;
            }

            if (c == '\\') {
                escaping = true;
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
