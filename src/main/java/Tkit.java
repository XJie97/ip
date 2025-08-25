import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Manages task list.
 * Supports operations: creating, marking, unmarking, deleting tasks, exiting.
 */
public class Tkit {

    /**
     * Reads input from user interface, processes and prints output.
     * Exits on "bye".
     *
     * @param args unused.
     */
    public static void main(String[] args) {
        String identity = "not three kids in a trench coat";
        Scanner sc = new Scanner(System.in);
        List<Task> lst = new ArrayList<>();

        System.out.println("____________________ \n\nHello from  " + identity);
        System.out.println("What do you mean we are three kids in a trench coat?");
        System.out.println("We are an adult person! \n____________________\n");

        while (true) {
            String str = sc.nextLine().trim();
            try {
                SplitCommand spc = SplitCommand.parse(str);

                switch (spc.command) {
                case BYE: {
                    System.out.println("____________________\n");
                    System.out.println("Goodbye, fellow adult!");
                    System.out.println("____________________\n");

                    sc.close();

                    return;
                }
                case LIST: {
                    System.out.println("____________________\n");

                    if (lst.isEmpty()) {
                        System.out.println("There are no entries yet.");
                    } else {
                        System.out.println("Here are the tasks in your list:");
                        for (int i = 0; i < lst.size(); i++) {
                            System.out.println((i + 1) + ". " + lst.get(i));
                        }
                    }

                    System.out.println("____________________\n");

                    break;
                }
                case TODO: {
                    String desc = spc.argOrEmpty().trim();

                    if (desc.isEmpty()) {
                        throw new TkitException("Mommy, what does this mean?\n" +
                                "Todo requires a description. Format: todo <DESCRIPTION>");
                    }

                    Task t = new Todo(desc);
                    lst.add(t);

                    printAdded(t, lst.size());

                    break;
                }
                case DEADLINE: {
                    String body = spc.argOrEmpty().trim();
                    // Split the input string into at most 2 parts, using the pattern \s*/by\s* as the separator.
                    // \\s* → In Java string literals, \\ means a single backslash in the regex.
                    // \\s means \s in regex, matches whitespace (spaces, tabs, newlines).
                    // \\s* means "zero or more whitespace characters."
                    // /by → Matches the literal string /by.
                    // \\s* (again) → Allow optional spaces after /by.
                    // Split at /by, allowing spaces before and after. Split into 2 pieces.

                    String[] parts = body.split("\\s*/by\\s*", 2);

                    if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                        throw new TkitException("Mommy, what does this mean?\n" +
                                "Wrong deadline input format. Format: deadline <TASK> /by <DEADLINE>");
                    }

                    Task t = new Deadline(parts[0].trim(), parts[1].trim());
                    lst.add(t);

                    printAdded(t, lst.size());

                    break;
                }
                case EVENT: {
                    String body = spc.argOrEmpty().trim();
                    // \\s*: optional spaces before /from.
                    // /from: keyword.
                    // \\s* → optional spaces after keyword.
                    // split at 'from', allowing spaces before/after. Split into 2 pieces.
                    String[] p1 = body.split("\\s*/from\\s*", 2);

                    if (p1.length < 2 || p1[0].trim().isEmpty()) {
                        throw new TkitException("Mommy, what does this mean?\n" +
                                "Wrong event input format. Format: event <EVENT> /from <START> /to <END>");
                    }

                    // Split the second half again, at /to. Split into 2 pieces.
                    String[] p2 = p1[1].split("\\s*/to\\s*", 2);
                    if (p2.length < 2 || p2[0].trim().isEmpty() || p2[1].trim().isEmpty()) {
                        throw new TkitException("Mommy, what does this mean?\n" +
                                "Wrong event input format. Format: event <EVENT> /from <START> /to <END>");
                    }
                    Task t = new Event(p1[0].trim(), p2[0].trim(), p2[1].trim());
                    lst.add(t);
                    printAdded(t, lst.size());
                    break;
                }
                case MARK: {
                    int idx = parseIndex(spc.argOrEmpty(), lst.size());
                    Task t = lst.get(idx);

                    t.markAsDone();

                    System.out.println("____________________\n");
                    System.out.println("Nice! I've marked this task as done:");
                    System.out.println("  " + t);
                    System.out.println("____________________\n");

                    break;
                }
                case UNMARK: {
                    int idx = parseIndex(spc.argOrEmpty(), lst.size());
                    Task t = lst.get(idx);

                    t.markAsUndone();

                    System.out.println("____________________\n");
                    System.out.println("OK mommy, I've marked this task as not done yet:");
                    System.out.println("  " + t);
                    System.out.println("____________________\n");

                    break;
                }
                case DELETE: {
                    String arg = spc.argOrEmpty();

                    if (arg.isEmpty()) {
                        throw new TkitException("Mommy, what does this mean?\n" +
                                "Delete requires an index. Format: delete <TASK_NUMBER>");
                    }

                    int idx = parseIndex(arg, lst.size());
                    Task removed = lst.remove(idx);

                    System.out.println("____________________\n");
                    System.out.println("Noted. I've removed this task:");
                    System.out.println("  " + removed);
                    System.out.println("Now you have " + lst.size() + " tasks in the list.");
                    System.out.println("____________________\n");

                    break;
                }

                case UNKNOWN:
                default:
                    throw new TkitException("Unknown command: \"" + str +
                            "\". Try: list, todo, deadline, event, mark N, unmark N, delete N, bye.");
                }
            } catch (TkitException e) {
                printError(e.getMessage());
            }
        }
    }

    /**
     * Prints standard notification on task add.
     *
     * @param t task added.
     * @param count list size after addition.
     */
    private static void printAdded(Task t, int count) {
        System.out.println("____________________\n");
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + t);
        System.out.println("Now you have " + count + " tasks in the list.");
        System.out.println("____________________\n");
    }

    /**
     * Prints error notification with error message.
     *
     * @param msg displayed error message.
     */
    private static void printError(String msg) {
        System.out.println("____________________\n");
        System.out.println(msg);
        System.out.println("____________________\n");
    }

    /**
     * Changes index start to 1 from 0 for user-friendliness, validate list bounds.
     *
     * @param s string containing a 1-based index.
     * @param size current list size.
     * @return zero-based index within [0, size).
     * @throws TkitException if parsing fails or index is out of range.
     */
    private static int parseIndex(String s, int size) throws TkitException {
        String trimmed = s.trim();
        try {
            int n = Integer.parseInt(trimmed);
            int idx = n - 1;
            if (idx < 0 || idx >= size) {
                throw new TkitException("Invalid task number: " + n + ". List has " + size + " task(s).");
            }
            return idx;
        } catch (NumberFormatException e) {
            throw new TkitException("Task number must be of type int. Received: \"" + trimmed + "\"");
        }
    }
}

/**
 * Enumerates for keyword mapping and parsing.
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
     * Returns user input keyword for command.
     *
     * @return user input keyword for command.
     */
    public String keyword() {
        return keyword;
    }

    /**
     * Resolves input command by first keyword.
     *
     * @param input command input.
     * @return matching Command or UNKNOWN if none matches.
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
 * Parses input line into command and remainder strings.
 */
final class SplitCommand {
    final Command command;
    final String remainder; // may be empty

    private SplitCommand(Command command, String remainder) {
        this.command = command;
        this.remainder = remainder == null ? "" : remainder;
    }

    /**
     * Parses a str input line into a Command and its argument string (if any).
     *
     * @param str full input line.
     * @return parsed command object.
     */
    public static SplitCommand parse(String str) {
        String line = str == null ? "" : str.trim();
        if (line.isEmpty()) {
            return new SplitCommand(Command.UNKNOWN, "");
        }
        String[] parts = line.split("\\s+", 2);
        Command cmd = Command.fromInput(parts[0]);
        String rest = parts.length > 1 ? parts[1] : "";
        return new SplitCommand(cmd, rest);
    }

    /**
     * Returns argument str following command input.
     *
     * @return the argument string (possibly empty) following the command input.
     */
    public String argOrEmpty() {
        return remainder;
    }
}

/**
 * Enumerates Task status. Encapsulates status stateIcon rendering.
 */
enum Status {
    DONE("X"),
    NOT_DONE(" ");

    private final String stateIcon;

    Status(String stateIcon) {
        this.stateIcon = stateIcon;
    }

    /**
     * Returns single-character stateIcon used in list rendering.
     *
     * @return single-character stateIcon used in list rendering.
     */
    public String stateIcon() {
        return stateIcon;
    }
}

/**
 * Creates an abstract class for Task.
 */
abstract class Task {
    protected final String description;
    protected Status status;
    protected final TaskType type;

    /**
     * Constructs Task.
     *
     * @param type task type.
     * @param description description.
     */
    protected Task(TaskType type, String description) {
        this.type = type;
        this.description = description;
        this.status = Status.NOT_DONE;
    }

    /**
     * Marks task done.
     */
    public void markAsDone() {
        this.status = Status.DONE;
    }

    /**
     * Marks task not done.
     */
    public void markAsUndone() {
        this.status = Status.NOT_DONE;
    }

    /**
     * Returns [task type][status] description, "[T][X] not fail CS2030S".
     *
     * @return [task type][status] description, "[T][X] not fail CS2030S".
     */
    @Override
    public String toString() {
        return "[" + type.tag() + "][" + status.stateIcon() + "] " + description;
    }
}

/**
 * Creates Task type: _Todo.
 */
class Todo extends Task {
    /**
     * Returns a _Todo with the given description.
     *
     * @param description _Todo description
     */
    public Todo(String description) {
        super(TaskType.TODO, description);
    }
}

/**
 * Creates Task type: Deadline, has due date.
 */
class Deadline extends Task {
    protected final String by;

    /**
     * Constructs a Deadline.
     *
     * @param description task description.
     * @param by deadline due date.
     */
    public Deadline(String description, String by) {
        super(TaskType.DEADLINE, description);
        this.by = by;
    }

    /**
     * Returns string of deadline with due date.
     *
     * @return string of deadline with due date.
     */
    @Override
    public String toString() {
        return super.toString() + " (by: " + by + ")";
    }
}

/**
 * Creates a Task type: Event, has start and end dates.
 */
class Event extends Task {
    protected final String from;
    protected final String to;

    /**
     * Constructs an Task of type Event.
     *
     * @param description event description.
     * @param from start date.
     * @param to end date.
     */
    public Event(String description, String from, String to) {
        super(TaskType.EVENT, description);
        this.from = from;
        this.to = to;
    }

    /**
     * Returns string of event with time range.
     *
     * @return string of event with time range.
     */
    @Override
    public String toString() {
        return super.toString() + " (from: " + from + " to: " + to + ")";
    }
}

/**
 * Enumerates Task types supported by Tkit bot.
 * Encapsulates type tag.
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
     * Returns a String of the Task tag.
     *
     * @return list type tag used
     */
    public String tag() {
        return tag;
    }
}

/**
 * Creates a checked exception for input error.
 */
class TkitException extends Exception {
    /**
     * Throws a TkitException with the displayed error message
     *
     * @param message displayed error message
     */
    public TkitException(String message) {
        super(message);
    }
}

