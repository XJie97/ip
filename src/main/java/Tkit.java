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
import java.util.Scanner;

/**
 * Primary entry point and command loop for the Tkit task manager.
 * Level 7: Save
 * Read and parse user commands
 * Mutate the in-memory task list
 * Persist changes to disk immediately after each mutation
 * Load tasks from disk on startup
 * Note: Code refactored by ChatGPT per standard Java convention
 */
public class Tkit {

    /** Identity banner line. */
    private static final String IDENTITY = "not three kids in a trench coat";

    /** Storage component handling serialization and durability. */
    private static final Storage STORAGE = new Storage();

    /**
     * Reads input, executes commands, persists changes, and prints results.
     * Exits when the user enters {@code bye}.
     *
     * @param args unused.
     */
    public static void main(String[] args) {
        // --- startup banner ---
        System.out.println("____________________ \n\nHello from  " + IDENTITY);
        System.out.println("What do you mean we are three kids in a trench coat?");
        System.out.println("We are an adult person! \n____________________\n");

        // --- load existing tasks (if any) ---
        List<Task> tasks = STORAGE.load();

        try (Scanner input = new Scanner(System.in)) {
            // --- command loop ---
            while (true) {
                String rawLine = input.nextLine().trim();

                try {
                    SplitCommand parsed = SplitCommand.parse(rawLine);

                    switch (parsed.command) {
                    case BYE: {
                        System.out.println("____________________\n");
                        System.out.println("Goodbye, fellow adult!");
                        System.out.println("____________________\n");
                        return;
                    }

                    case LIST: {
                        System.out.println("____________________\n");
                        if (tasks.isEmpty()) {
                            System.out.println("There are no entries yet.");
                        } else {
                            System.out.println("Here are the tasks in your list:");
                            for (int i = 0; i < tasks.size(); i++) {
                                System.out.println((i + 1) + ". " + tasks.get(i));
                            }
                        }
                        System.out.println("____________________\n");
                        break;
                    }

                    case TODO: {
                        String description = parsed.argOrEmpty().trim();
                        if (description.isEmpty()) {
                            throw new TkitException(
                                    "Mommy, what does this mean?\n" +
                                            "Todo requires a description. Format: todo <DESCRIPTION>"
                            );
                        }

                        Task task = new Todo(description);
                        tasks.add(task);

                        // persist after mutation
                        STORAGE.save(tasks);

                        printAdded(task, tasks.size());
                        break;
                    }

                    case DEADLINE: {
                        String body = parsed.argOrEmpty().trim();

                        // split once at "/by", to allow surrounding whitespace
                        String[] parts = body.split("\\s*/by\\s*", 2);
                        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                            throw new TkitException(
                                    "Mommy, what does this mean?\n" +
                                            "Wrong deadline input format. Format: deadline <TASK> /by <DEADLINE>"
                            );
                        }

                        Task task = new Deadline(parts[0].trim(), parts[1].trim());
                        tasks.add(task);

                        // persist after mutation
                        STORAGE.save(tasks);

                        printAdded(task, tasks.size());
                        break;
                    }

                    case EVENT: {
                        String body = parsed.argOrEmpty().trim();

                        String[] firstSplit = body.split("\\s*/from\\s*", 2);
                        if (firstSplit.length < 2 || firstSplit[0].trim().isEmpty()) {
                            throw new TkitException(
                                    "Mommy, what does this mean?\n" +
                                            "Wrong event input format. Format: event <EVENT> /from <START> /to <END>"
                            );
                        }

                        String[] secondSplit = firstSplit[1].split("\\s*/to\\s*", 2);
                        if (secondSplit.length < 2 || secondSplit[0].trim().isEmpty() || secondSplit[1].trim().isEmpty()) {
                            throw new TkitException(
                                    "Mommy, what does this mean?\n" +
                                            "Wrong event input format. Format: event <EVENT> /from <START> /to <END>"
                            );
                        }

                        Task task = new Event(firstSplit[0].trim(), secondSplit[0].trim(), secondSplit[1].trim());
                        tasks.add(task);

                        // persist after mutation
                        STORAGE.save(tasks);

                        printAdded(task, tasks.size());
                        break;
                    }

                    case MARK: {
                        int index = parseIndex(parsed.argOrEmpty(), tasks.size());
                        Task task = tasks.get(index);
                        task.markAsDone();

                        // persist after mutation
                        STORAGE.save(tasks);

                        System.out.println("____________________\n");
                        System.out.println("Nice! I've marked this task as done:");
                        System.out.println("  " + task);
                        System.out.println("____________________\n");
                        break;
                    }

                    case UNMARK: {
                        int index = parseIndex(parsed.argOrEmpty(), tasks.size());
                        Task task = tasks.get(index);
                        task.markAsUndone();

                        // persist after mutation
                        STORAGE.save(tasks);

                        System.out.println("____________________\n");
                        System.out.println("OK mommy, I've marked this task as not done yet:");
                        System.out.println("  " + task);
                        System.out.println("____________________\n");
                        break;
                    }

                    case DELETE: {
                        String arg = parsed.argOrEmpty();
                        if (arg.isEmpty()) {
                            throw new TkitException(
                                    "Mommy, what does this mean?\n" +
                                            "Delete requires an index. Format: delete <TASK_NUMBER>"
                            );
                        }

                        int index = parseIndex(arg, tasks.size());
                        Task removed = tasks.remove(index);

                        // persist after mutation
                        STORAGE.save(tasks);

                        System.out.println("____________________\n");
                        System.out.println("Noted. I've removed this task:");
                        System.out.println("  " + removed);
                        System.out.println("Now you have " + tasks.size() + " tasks in the list.");
                        System.out.println("____________________\n");
                        break;
                    }

                    case UNKNOWN:
                    default:
                        throw new TkitException(
                                "Unknown command: \"" + rawLine +
                                        "\". Try: list, todo, deadline, event, mark N, unmark N, delete N, bye."
                        );
                    }
                } catch (TkitException e) {
                    printError(e.getMessage());
                }
            }
        }
    }

    /**
     * Prints the standard "added" notification for a newly appended task.
     *
     * @param taskAdded  the task that was just added
     * @param totalCount the current size of the list after the addition
     */
    private static void printAdded(Task taskAdded, int totalCount) {
        System.out.println("____________________\n");
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + taskAdded);
        System.out.println("Now you have " + totalCount + " tasks in the list.");
        System.out.println("____________________\n");
    }

    /**
     * Prints an error notification with the given message.
     *
     * @param message the error text to display
     */
    private static void printError(String message) {
        System.out.println("____________________\n");
        System.out.println(message);
        System.out.println("____________________\n");
    }

    /**
     * Parses a 1-based index from user input and validates it for the current list size.
     *
     * @param userSuppliedIndex string containing a 1-based index
     * @param currentSize       current task list size
     * @return zero-based index within {@code [0, currentSize)}
     * @throws TkitException if parsing fails or the index is out of range
     */
    private static int parseIndex(String userSuppliedIndex, int currentSize) throws TkitException {
        String trimmed = userSuppliedIndex.trim();
        try {
            int oneBased = Integer.parseInt(trimmed);
            int zeroBased = oneBased - 1;
            if (zeroBased < 0 || zeroBased >= currentSize) {
                throw new TkitException("Invalid task number: " + oneBased + ". List has " + currentSize + " task(s).");
            }
            return zeroBased;
        } catch (NumberFormatException e) {
            throw new TkitException("Task number must be of type int. Received: \"" + trimmed + "\"");
        }
    }
}

/**
 * Encapsulates reading from and writing to an OS-independent relative file path.
 * Format (pipe-delimited with escaping):
 *   TYPE | DONE | DESCRIPTION | OTHER...
 *   T | 1 | read book
 *   D | 0 | return book | June 6th
 *   E | 0 | project meeting | Aug 6th | 2-4pm
 * Escaping:
 *  {@code \|} represents a literal pipe within a field</li>
 *  {@code \\} represents a literal backslash</li>
 * Corrupted lines are skipped silently but counted for diagnostics.
 */
final class Storage {

    /** Relative, OS-independent data file path. */
    private final Path dataFile = Path.of("data", "Tkit.txt");

    /** Line delimiter used for user-readable comments in temp writes. */
    private static final String HEADER_PREFIX = "#";

    /**
     * Loads tasks from disk. Creates parent directory if missing.
     * Missing file returns an empty list. Corrupted lines are ignored.
     *
     * @return list of Tasks parsed from the data file
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
                    // skip corrupted line
                    corruptedCount++;
                }
            }
        } catch (IOException io) {
            // treat as no data; do not crash the app
            return new ArrayList<>();
        }

        if (corruptedCount > 0) {
            System.out.println("____________________\n");
            System.out.println("Warning: " + corruptedCount + " corrupted line(s) ignored while loading.");
            System.out.println("____________________\n");
        }

        return loaded;
    }

    /**
     * Saves the entire task list to disk. Write-to-temp then atomic move.
     * Creates parent directory on demand.
     *
     * @param tasks current snapshot of tasks to persist
     */
    public void save(List<Task> tasks) {
        ensureParentDir();
        Path tmp = dataFile.resolveSibling(dataFile.getFileName() + ".tmp");

        // write temp file first
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

        // move temp over the real file
        try {
            Files.move(tmp, dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
        } catch (AtomicMoveNotSupportedException am) {
            // fallback: non-atomic replace
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
            // continue; save/load will report specific errors
        }
    }

    /**
     * Serializes a task into a single line.
     *
     * @param t task to encode
     * @return encoded line
     */
    private String encodeTask(Task t) {
        String doneFlag = (t.status == Status.DONE) ? "1" : "0";
        String type = t.type.tag();

        StringBuilder sb = new StringBuilder();
        sb.append(type).append(" | ").append(doneFlag).append(" | ")
                .append(escape(t.description));

        if (t instanceof Deadline) {
            sb.append(" | ").append(escape(((Deadline) t).by));
        } else if (t instanceof Event) {
            Event e = (Event) t;
            sb.append(" | ").append(escape(e.from)).append(" | ").append(escape(e.to));
        }

        return sb.toString();
    }

    /**
     * Deserializes a line into a task. Returns {@code null} if the line is not decodable.
     *
     * @param line encoded line
     * @return constructed task or {@code null} if corrupted
     */
    private Task decodeLine(String line) {
        // split on unescaped pipes
        List<String> rawFields = splitPreservingEscapes(line);
        if (rawFields.size() < 3) {
            return null;
        }

        String type = rawFields.get(0).trim();
        String done = rawFields.get(1).trim();

        // unescape all fields after trimming
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
            if (rawFields.size() < 4) return null;
            task = new Deadline(description, rawFields.get(3));
            break;
        case "E":
            if (rawFields.size() < 5) return null;
            task = new Event(description, rawFields.get(3), rawFields.get(4));
            break;
        default:
            return null;
        }

        if ("1".equals(done)) {
            task.markAsDone();
        } else if (!"0".equals(done)) {
            // invalid done flag → treat as corrupted
            return null;
        }

        return task;
    }

    /**
     * Escapes literal backslashes and pipes within a field.
     *
     * @param s input field
     * @return escaped field
     */
    private static String escape(String s) {
        return s.replace("\\", "\\\\").replace("|", "\\|");
    }

    /**
     * Reverses {@link #escape(String)} on a field.
     *
     * @param s escaped field
     * @return raw field
     */
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
            // trailing backslash; treat as literal
            out.append('\\');
        }
        return out.toString();
    }

    /**
     * Splits a line by unescaped {@code |}, preserving escaped separators.
     *
     * @param line encoded line
     * @return list of raw fields (still escaped)
     */
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
                // do not append yet; next character decides
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

/**
 * Command keywords recognized by the parser.
 */
enum Command {
    BYE("bye"),
    UNKNOWN(""),
    LIST("list"),
    TODO("todo"),
    DEADLINE("deadline"),
    EVENT("event"),
    MARK("mark"),
    UNMARK("unmark"),
    DELETE("delete");

    private final String keyword;

    Command(String keyword) {
        this.keyword = keyword;
    }

    /**
     * Returns the keyword string that triggers this command.
     *
     * @return command keyword
     */
    public String keyword() {
        return keyword;
    }

    /**
     * Maps the first token of an input line to a {@link Command}.
     *
     * @param input first token (case-insensitive)
     * @return matching command or {@link #UNKNOWN}
     */
    public static Command fromInput(String input) {
        String s = input == null ? "" : input.toLowerCase();
        for (Command c : values()) {
            if (!c.keyword.isEmpty() && c.keyword.equals(s)) {
                return c;
            }
        }
        return UNKNOWN;
    }
}

/**
 * Lightweight representation of a parsed command: verb + remainder.
 */
final class SplitCommand {
    final Command command;
    final String remainder; // may be empty

    private SplitCommand(Command command, String remainder) {
        this.command = command;
        this.remainder = remainder == null ? "" : remainder;
    }

    /**
     * Parses a full input line into command and trailing argument string.
     *
     * @param line full user input
     * @return parsed {@link SplitCommand}
     */
    public static SplitCommand parse(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return new SplitCommand(Command.UNKNOWN, "");
        }
        String[] parts = normalized.split("\\s+", 2);
        Command cmd = Command.fromInput(parts[0]);
        String rest = parts.length > 1 ? parts[1] : "";
        return new SplitCommand(cmd, rest);
    }

    /**
     * Returns the remainder after the command token, or an empty string.
     *
     * @return remainder string
     */
    public String argOrEmpty() {
        return remainder;
    }
}

/**
 * Display status of a task with a single-character icon.
 */
enum Status {
    DONE("X"),
    NOT_DONE(" ");

    private final String stateIcon;

    Status(String stateIcon) {
        this.stateIcon = stateIcon;
    }

    /**
     * Returns the one-character state icon used in list rendering.
     *
     * @return icon string
     */
    public String stateIcon() {
        return stateIcon;
    }
}

/**
 * Common base type for all tasks.
 */
abstract class Task {
    protected final String description;
    protected Status status;
    protected final TaskType type;

    /**
     * Constructs a task with a type and description. Default status is NOT_DONE.
     *
     * @param type        task type tag
     * @param description human-readable description
     */
    protected Task(TaskType type, String description) {
        this.type = type;
        this.description = description;
        this.status = Status.NOT_DONE;
    }

    /** Marks this task as done. */
    public void markAsDone() {
        this.status = Status.DONE;
    }

    /** Marks this task as not done. */
    public void markAsUndone() {
        this.status = Status.NOT_DONE;
    }

    /**
     * Renders as {@code [Type][State] Description}, e.g. {@code [T][X] read book}.
     *
     * @return display string
     */
    @Override
    public String toString() {
        return "[" + type.tag() + "][" + status.stateIcon() + "] " + description;
    }
}

/**
 * Task type representing a simple to-do with only a description.
 */
class Todo extends Task {
    /**
     * Constructs a {@code Todo}.
     *
     * @param description description text
     */
    public Todo(String description) {
        super(TaskType.TODO, description);
    }
}

/**
 * Task type with a deadline date/time.
 */
class Deadline extends Task {
    protected final String by;

    /**
     * Constructs a {@code Deadline}.
     *
     * @param description task description
     * @param by          deadline label
     */
    public Deadline(String description, String by) {
        super(TaskType.DEADLINE, description);
        this.by = by;
    }

    /**
     * Appends {@code (by: ...)} to the base representation.
     *
     * @return display string
     */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by + ")";
    }
}

/**
 * Task type describing a time-ranged event.
 */
class Event extends Task {
    protected final String from;
    protected final String to;

    /**
     * Constructs an {@code Event}.
     *
     * @param description event description
     * @param from        start label
     * @param to          end label
     */
    public Event(String description, String from, String to) {
        super(TaskType.EVENT, description);
        this.from = from;
        this.to = to;
    }

    /**
     * Appends {@code (from: ... to: ...)} to the base representation.
     *
     * @return display string
     */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }
}

/**
 * Supported task categories. Each value carries a single-letter tag.
 */
enum TaskType {
    TODO("T"),
    DEADLINE("D"),
    EVENT("E");

    private final String tag;

    TaskType(String tag) {
        this.tag = tag;
    }

    /**
     * Returns the single-letter tag for this type.
     *
     * @return tag string
     */
    public String tag() {
        return tag;
    }
}

/**
 * Checked exception used for input and command errors in Tkit.
 */
class TkitException extends Exception {
    /**
     * Constructs an exception with a message intended for user display.
     *
     * @param message error text
     */
    public TkitException(String message) {
        super(message);
    }
}
