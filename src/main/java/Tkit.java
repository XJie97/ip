import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Primary entry point and command loop for the Tkit task manager.
 * Level 8: Dates/Times
 * - Read and parse user commands
 * - Mutate the in-memory task list
 * - Persist changes to disk immediately after each mutation
 * - Load tasks from disk on startup
 * - Interpret dates/times and render them in a human-friendly format
 * - Change chatbot personality to be more suitable for a task manager.
 *
 * Notes:
 * - Invalid inputs do not terminate the app; they produce an error message and continue.
 * - Dates are stored in ISO-8601.
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
                            printError("I do not understand this input format\n" +
                                    "Todo requires a description." +
                                    "Please try Format: todo <DESCRIPTION>");
                            break;
                        }

                        Task task = new Todo(description);
                        tasks.add(task);
                        STORAGE.save(tasks);
                        printAdded(task, tasks.size());
                        break;
                    }

                    case DEADLINE: {
                        String body = parsed.argOrEmpty().trim();

                        String[] parts = body.split("\\s*/by\\s*", 2);
                        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                            printError("I do not understand this input format\n" +
                                    "Wrong deadline input format.\n" +
                                    "Please try format: deadline <TASK> /by <DATE_OR_DATE_TIME>\n" +
                                    "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                            break;
                        }

                        LocalDateTime by = DateTimeUtil.tryParseToLdt(parts[1].trim());
                        if (by == null) {
                            printError("I do not recognize this date/time format:" +
                                    " \"" + parts[1].trim() + "\"\n" +
                                    "Please try format: deadline <TASK> /by <DATE_OR_DATE_TIME>\n" +
                                    "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                            break;
                        }

                        Task task = new Deadline(parts[0].trim(), by);
                        tasks.add(task);
                        STORAGE.save(tasks);
                        printAdded(task, tasks.size());
                        break;
                    }

                    case EVENT: {
                        String body = parsed.argOrEmpty().trim();

                        String[] firstSplit = body.split("\\s*/from\\s*", 2);
                        if (firstSplit.length < 2 || firstSplit[0].trim().isEmpty()) {
                            printError("I do not understand this input format.\n" +
                                    "Wrong event input format.\n" +
                                    "Please try format: event <EVENT> /from <START> /to <END>");
                            break;
                        }

                        String[] secondSplit = firstSplit[1].split("\\s*/to\\s*", 2);
                        if (secondSplit.length < 2 || secondSplit[0].trim().isEmpty() || secondSplit[1].trim().isEmpty()) {
                            printError("I do not understand this input format.\n" +
                                    "Wrong event input format.\n" +
                                    "Use: event <EVENT> /from <START> /to <END>");
                            break;
                        }

                        LocalDateTime from = DateTimeUtil.tryParseToLdt(secondSplit[0].trim());
                        LocalDateTime to = DateTimeUtil.tryParseToLdt(secondSplit[1].trim());
                        if (from == null || to == null) {
                            printError("I do not understand this input format.\n" +
                                    "Use: event <EVENT> /from <START> /to <END>\n" +
                                    "Examples: 2019-12-02 1400  |  2019-12-02  |  2/12/2019 1600");
                            break;
                        }

                        Task task = new Event(firstSplit[0].trim(), from, to);
                        tasks.add(task);
                        STORAGE.save(tasks);
                        printAdded(task, tasks.size());
                        break;
                    }

                    case MARK: {
                        int index = parseIndex(parsed.argOrEmpty(), tasks.size());
                        Task task = tasks.get(index);
                        task.markAsDone();
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
                        STORAGE.save(tasks);
                        System.out.println("____________________\n");
                        System.out.println("OK, I've marked this task as not done yet:");
                        System.out.println("  " + task);
                        System.out.println("____________________\n");
                        break;
                    }

                    case DELETE: {
                        String arg = parsed.argOrEmpty();
                        if (arg.isEmpty()) {
                            printError("I do not understand this input format.\n" +
                                    "Delete requires an index. " +
                                    "Please try format: delete <TASK_NUMBER>");
                            break;
                        }

                        int index = parseIndex(arg, tasks.size());
                        Task removed = tasks.remove(index);
                        STORAGE.save(tasks);
                        System.out.println("____________________\n");
                        System.out.println("Noted. I've removed this task:");
                        System.out.println("  " + removed);
                        System.out.println("Now you have " + tasks.size() + " tasks in the list.");
                        System.out.println("____________________\n");
                        break;
                    }

                    case ON: {
                        String raw = parsed.argOrEmpty().trim();
                        var targetDate = DateTimeUtil.tryParseToLocalDate(raw);
                        if (targetDate == null) {
                            printError("Unrecognized date.\nUse: on <DATE or DATE TIME>\nExamples: on 2019-12-02  |  on 2/12/2019");
                            break;
                        }

                        List<Task> hits = new ArrayList<>();
                        for (Task t : tasks) {
                            if (t instanceof Deadline) {
                                Deadline d = (Deadline) t;
                                if (d.getDueAt().toLocalDate().equals(targetDate)) {
                                    hits.add(t);
                                }
                            } else if (t instanceof Event) {
                                Event e = (Event) t;
                                if (DateTimeUtil.dateIntersects(targetDate, e.getFrom(), e.getTo())) {
                                    hits.add(t);
                                }
                            }
                        }

                        System.out.println("____________________\n");
                        if (hits.isEmpty()) {
                            System.out.println("No deadlines/events on " + DateTimeUtil.pretty(targetDate) + ".");
                        } else {
                            System.out.println("Deadlines/events on " + DateTimeUtil.pretty(targetDate) + ":");
                            for (int i = 0; i < hits.size(); i++) {
                                System.out.println((i + 1) + ". " + hits.get(i));
                            }
                        }
                        System.out.println("____________________\n");
                        break;
                    }


                    case UNKNOWN:
                    default:
                        printError("I do not understand this command: \"" + rawLine + "\".\n" +
                                "Try: list, todo, deadline, event, mark N, unmark N, delete N, bye.");
                        break;
                    }
                } catch (TkitException e) {
                    printError(e.getMessage());
                } catch (Exception e) {
                    // Final safety net: do not terminate; report and continue.
                    printError("Error: " + e.getMessage());
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
 *   D | 0 | return book | 2019-12-02T18:00
 *   E | 0 | project meeting | 2019-12-02T14:00 | 2019-12-02T16:00
 * Escaping:
 *  {@code \|} represents a literal pipe within a field
 *  {@code \\} represents a literal backslash
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
                    corruptedCount++;
                }
            }
        } catch (IOException io) {
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
            Files.move(tmp, dataFile,
                    StandardCopyOption.REPLACE_EXISTING,
                    StandardCopyOption.ATOMIC_MOVE);
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
            Deadline d = (Deadline) t;
            sb.append(" | ").append(escape(DateTimeUtil.toStorage(d.getDueAt())));
        } else if (t instanceof Event) {
            Event e = (Event) t;
            sb.append(" | ").append(escape(DateTimeUtil.toStorage(e.getFrom())))
                    .append(" | ").append(escape(DateTimeUtil.toStorage(e.getTo())));
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
            if (rawFields.size() < 4) return null;
            LocalDateTime by = DateTimeUtil.tryParseStorageOrInput(rawFields.get(3));
            if (by == null) return null;
            task = new Deadline(description, by);
            break;
        case "E":
            if (rawFields.size() < 5) return null;
            LocalDateTime from = DateTimeUtil.tryParseStorageOrInput(rawFields.get(3));
            LocalDateTime to = DateTimeUtil.tryParseStorageOrInput(rawFields.get(4));
            if (from == null || to == null) return null;
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

/** Command keywords recognized by the parser. */
enum Command {
    BYE("bye"),
    UNKNOWN(""),
    LIST("list"),
    TODO("todo"),
    DEADLINE("deadline"),
    EVENT("event"),
    MARK("mark"),
    UNMARK("unmark"),
    DELETE("delete"),
    ON("on");


    private final String keyword;

    Command(String keyword) {
        this.keyword = keyword;
    }

    /** Returns the keyword string that triggers this command. */
    public String keyword() {
        return keyword;
    }

    /** Maps the first token of an input line to a {@link Command}. */
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

/** Lightweight representation of a parsed command: verb + remainder. */
final class SplitCommand {
    final Command command;
    final String remainder; // may be empty

    private SplitCommand(Command command, String remainder) {
        this.command = command;
        this.remainder = remainder == null ? "" : remainder;
    }

    /** Parses a full input line into command and trailing argument string. */
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

    /** Returns the remainder after the command token, or an empty string. */
    public String argOrEmpty() {
        return remainder;
    }
}

/** Display status of a task with a single-character icon. */
enum Status {
    DONE("X"),
    NOT_DONE(" ");

    private final String stateIcon;

    Status(String stateIcon) {
        this.stateIcon = stateIcon;
    }

    /** Returns the one-character state icon used in list rendering. */
    public String stateIcon() {
        return stateIcon;
    }
}

/** Common base type for all tasks. */
abstract class Task {
    protected final String description;
    protected Status status;
    protected final TaskType type;

    /** Constructs a task with a type and description. Default status is NOT_DONE. */
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

    /** Renders as {@code [Type][State] Description}. */
    @Override
    public String toString() {
        return "[" + type.tag() + "][" + status.stateIcon() + "] " + description;
    }
}

/** Task type representing a simple to-do with only a description. */
class Todo extends Task {
    public Todo(String description) {
        super(TaskType.TODO, description);
    }
}

/** Task type with a deadline date/time. */
class Deadline extends Task {
    private final LocalDateTime dueAt;

    public Deadline(String description, LocalDateTime dueAt) {
        super(TaskType.DEADLINE, description);
        this.dueAt = dueAt;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    @Override
    public String toString() {
        return super.toString() + " (by: " + DateTimeUtil.pretty(dueAt) + ")";
    }
}

/** Task type describing a time-ranged event. */
class Event extends Task {
    private final LocalDateTime from;
    private final LocalDateTime to;

    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(TaskType.EVENT, description);
        this.from = from;
        this.to = to;
    }

    public LocalDateTime getFrom() {
        return from;
    }

    public LocalDateTime getTo() {
        return to;
    }

    @Override
    public String toString() {
        return super.toString() + " (from: " + DateTimeUtil.pretty(from) + " to: " + DateTimeUtil.pretty(to) + ")";
    }
}

/** Supported task categories. Each value carries a single-letter tag. */
enum TaskType {
    TODO("T"),
    DEADLINE("D"),
    EVENT("E");

    private final String tag;

    TaskType(String tag) {
        this.tag = tag;
    }

    /** Returns the single-letter tag for this type. */
    public String tag() {
        return tag;
    }
}

/** Checked exception used for input and command errors in Tkit. */
class TkitException extends Exception {
    public TkitException(String message) {
        super(message);
    }
}

/**
 * Date/time parsing and formatting helpers for Level 8.
 *
 * Supported input examples:
 *  - 2019-12-02
 *  - 2019-12-02 1800
 *  - 2/12/2019
 *  - 2/12/2019 1800
 *
 * Storage format is ISO-8601 LocalDateTime (e.g., 2019-12-02T18:00).
 * Display format is "MMM d yyyy" or "MMM d yyyy HH:mm" if time is non-midnight.
 */
final class DateTimeUtil {
    private DateTimeUtil() {}

    // Accept ISO yyyy-MM-dd[ HHmm] OR d/M/yyyy[ HHmm]
    private static final DateTimeFormatter INPUT_FMT = new DateTimeFormatterBuilder()
            // First optional branch: yyyy-MM-dd[ HHmm]
            .appendOptional(new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .optionalStart().appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalEnd()
                    .toFormatter())
            // Second optional branch: d/M/yyyy[ HHmm]
            .appendOptional(new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('/')
                    .appendValue(ChronoField.YEAR)
                    .optionalStart().appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalEnd()
                    .toFormatter())
            .toFormatter();

    private static final DateTimeFormatter OUT_DATE = DateTimeFormatter.ofPattern("MMM d yyyy");
    private static final DateTimeFormatter OUT_DATE_TIME = DateTimeFormatter.ofPattern("MMM d yyyy HH:mm");

    /** Strict parse; throws on failure (kept for internal use). */
    public static LocalDateTime parseToLdt(String raw) {
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, INPUT_FMT);
        } catch (Exception ignore) {
        }
        try {
            LocalDate d = LocalDate.parse(s, INPUT_FMT);
            return d.atStartOfDay();
        } catch (Exception e) {
            throw new IllegalArgumentException("I do not understand this date/time format\n" +
                    "Please try this instead: \"" + raw + "\"", e);
        }
    }

    /** Non-throwing parse; returns null on failure. */
    public static LocalDateTime tryParseToLdt(String raw) {
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, INPUT_FMT);
        } catch (Exception ignore) {
        }
        try {
            LocalDate d = LocalDate.parse(s, INPUT_FMT);
            return d.atStartOfDay();
        } catch (Exception ignore) {
            return null;
        }
    }

    /** Pretty-print: omit time if midnight, else include HH:mm. */
    public static String pretty(LocalDateTime ldt) {
        if (ldt.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return ldt.format(OUT_DATE);
        }
        return ldt.format(OUT_DATE_TIME);
    }

    /** Lossless storage format (ISO-8601). */
    public static String toStorage(LocalDateTime ldt) {
        return ldt.toString();
    }

    /** Strict storage parse with fallback to input; throws on failure. */
    public static LocalDateTime parseStorageOrInput(String text) {
        String s = text.trim();
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {
            return parseToLdt(s);
        }
    }

    /** Non-throwing storage parse with fallback to input; returns null on failure. */
    public static LocalDateTime tryParseStorageOrInput(String text) {
        String s = text.trim();
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {
        }
        return tryParseToLdt(s);
    }
    /** Non-throwing parse to LocalDate; returns null on failure. */
    public static LocalDate tryParseToLocalDate(String raw) {
        if (raw == null) return null;
        String s = raw.trim();
        // Try full date-time first, then date-only
        try {
            return LocalDateTime.parse(s, INPUT_FMT).toLocalDate();
        } catch (Exception ignore) { }
        try {
            return LocalDate.parse(s, INPUT_FMT);
        } catch (Exception ignore) {
            return null;
        }
    }

    /** Pretty-print for a LocalDate using the same OUT_DATE pattern. */
    public static String pretty(LocalDate date) {
        return date.format(OUT_DATE);
    }

    /**
     * True if the given calendar date is:
     *  - the same date as the start, or
     *  - the same date as the end, or
     *  - strictly between start and end by date (inclusive).
     * Assumes start <= end; if not, swaps.
     */
    public static boolean dateIntersects(LocalDate date, LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            var tmp = start; start = end; end = tmp;
        }
        LocalDate s = start.toLocalDate();
        LocalDate e = end.toLocalDate();
        return !(date.isBefore(s) || date.isAfter(e));
    }

}
