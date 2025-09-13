package tkit;

import java.util.Scanner;

import tkit.Parser.SplitCommand;

/**
 * Primary entry point and command loop for the Tkit task manager.
 * Responsibilities:
 *   Initialize subsystems (UI, storage, task list)
 *   Read input, parse commands, and dispatch actions
 *   Persist changes after each mutation
 *
 * Usage
 * # Compile from project root (source root: src/main/java)
 * javac -d out src/main/java/tkit/*.java
 * # Run
 * java -cp out tkit.Tkit
 */
public final class Tkit {
    /** Identity banner line. */
    private static final String IDENTITY = "not three kids in a trench coat";

    /** Storage component handling serialization and durability. */
    private static final Storage storage = new Storage();

    /** Console UI renderer. */
    private static final Ui ui = new Ui();

    /**
     * Application entry point.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        ui.banner(IDENTITY);

        TaskList tasks = new TaskList(storage.load());
        assert tasks != null : "TaskList must be constructed";

        try (Scanner input = new Scanner(System.in)) {
            while (true) {
                String rawLine = input.nextLine().trim();
                assert rawLine != null : "Scanner returned null line";

                try {
                    SplitCommand parsed = Parser.parse(rawLine);
                    assert parsed != null;

                    switch (parsed.command) {
                    case BYE: {
                        ui.exit();
                        return;
                    }

                    case LIST: {
                        ui.list(tasks.view());
                        break;
                    }

                    case TODO: {
                        String description = parsed.argOrEmpty().trim();
                        if (description.isEmpty()) {
                            ui.error("I do not understand this input format\n"
                                    + "Todo requires a description."
                                    + "Please try Format: todo <DESCRIPTION>");
                            break;
                        }
                        Task task = new Todo(description);
                        tasks.add(task);
                        storage.save(tasks.view());
                        ui.added(task, tasks.size());
                        break;
                    }

                    case DEADLINE: {
                        String body = parsed.argOrEmpty().trim();
                        String[] parts = body.split("\\s*/by\\s*", 2);
                        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                            ui.error("I do not understand this input format\n"
                                    + "Wrong deadline input format.\n"
                                    + "Please try format: deadline <TASK> /by <DATE_OR_DATE_TIME>\n"
                                    + "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                            break;
                        }
                        var by = DateTimeUtil.tryParseToLdt(parts[1].trim());
                        if (by == null) {
                            ui.error("I do not recognize this date/time format: \""
                                    + parts[1].trim()
                                    + "\"\nPlease try format: deadline <TASK> /by <DATE_OR_DATE_TIME>\n"
                                    + "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                            break;
                        }
                        Task task = new Deadline(parts[0].trim(), by);
                        tasks.add(task);
                        storage.save(tasks.view());
                        ui.added(task, tasks.size());
                        break;
                    }

                    case EVENT: {
                        String body = parsed.argOrEmpty().trim();
                        String[] firstSplit = body.split("\\s*/from\\s*", 2);
                        if (firstSplit.length < 2 || firstSplit[0].trim().isEmpty()) {
                            ui.error("I do not understand this input format.\n"
                                    + "Wrong event input format.\n"
                                    + "Please try format: event <EVENT> /from <START> /to <END>");
                            break;
                        }
                        String[] secondSplit = firstSplit[1].split("\\s*/to\\s*", 2);
                        if (secondSplit.length < 2
                                || secondSplit[0].trim().isEmpty()
                                || secondSplit[1].trim().isEmpty()) {
                            ui.error("I do not understand this input format.\n"
                                    + "Wrong event input format.\n"
                                    + "Use: event <EVENT> /from <START> /to <END>");
                            break;
                        }
                        var from = DateTimeUtil.tryParseToLdt(secondSplit[0].trim());
                        var to = DateTimeUtil.tryParseToLdt(secondSplit[1].trim());
                        if (from == null || to == null) {
                            ui.error("I do not understand this input format.\n"
                                    + "Use: event <EVENT> /from <START> /to <END>\n"
                                    + "Examples: 2019-12-02 1400  |  2019-12-02  |  2/12/2019 1600");
                            break;
                        }
                        Task task = new Event(firstSplit[0].trim(), from, to);
                        tasks.add(task);
                        storage.save(tasks.view());
                        ui.added(task, tasks.size());
                        break;
                    }

                    case MARK: {
                        int index = parseIndex(parsed.argOrEmpty(), tasks.size());
                        tasks.mark(index);
                        storage.save(tasks.view());
                        ui.marked(tasks.get(index));
                        break;
                    }

                    case UNMARK: {
                        int index = parseIndex(parsed.argOrEmpty(), tasks.size());
                        tasks.unmark(index);
                        storage.save(tasks.view());
                        ui.unmarked(tasks.get(index));
                        break;
                    }

                    case DELETE: {
                        String arg = parsed.argOrEmpty();
                        if (arg.isEmpty()) {
                            ui.error("I do not understand this input format.\n"
                                    + "Delete requires an index. "
                                    + "Please try format: delete <TASK_NUMBER>");
                            break;
                        }
                        int index = parseIndex(arg, tasks.size());
                        Task removed = tasks.removeAt(index);
                        storage.save(tasks.view());
                        ui.removed(removed, tasks.size());
                        break;
                    }

                    case ON: {
                        String raw = parsed.argOrEmpty().trim();
                        var targetDate = DateTimeUtil.tryParseToLocalDate(raw);
                        if (targetDate == null) {
                            ui.error("Unrecognized date.\nUse: on <DATE or DATE TIME>\n"
                                    + "Examples: on 2019-12-02  |  on 2/12/2019");
                            break;
                        }
                        ui.onDate(targetDate, tasks.onDate(targetDate));
                        break;
                    }

                    case FIND: {
                        String keyword = parsed.argOrEmpty().trim();
                        if (keyword.isEmpty()) {
                            ui.error("I do not understand this input format.\n"
                                    + "Find requires a keyword. "
                                    + "Use: find <KEYWORD>");
                            break;
                        }
                        ui.matches(tasks.find(keyword));
                        break;
                    }

                    case UNKNOWN:
                    default:
                        ui.error("I do not understand this command: \""
                                + rawLine
                                + "\".\nTry: list, todo, deadline, event, "
                                + "mark N, unmark N, delete N, find <KEYWORD>, bye.");
                        break;
                    }
                } catch (TkitException e) {
                    ui.error(e.getMessage());
                } catch (Exception e) {
                    ui.error("Error: " + e.getMessage());
                }
            }
        }
    }

    /**
     * Parses a 1-based index from user input and validates it against the current list size.
     *
     * @param userSuppliedIndex string containing a 1-based index
     * @param currentSize       current task list size
     * @return zero-based index within {@code [0, currentSize)}
     * @throws TkitException if parsing fails or the index is out of range
     */
    private static int parseIndex(String userSuppliedIndex, int currentSize) throws TkitException {
        assert currentSize >= 0;
        String trimmed = userSuppliedIndex.trim();
        assert !trimmed.isEmpty();
        try {
            int oneBased = Integer.parseInt(trimmed);
            int zeroBased = oneBased - 1;
            if (zeroBased < 0 || zeroBased >= currentSize) {
                throw new TkitException("Invalid task number: "
                        + oneBased
                        + ". List has "
                        + currentSize
                        + " task(s).");
            }
            return zeroBased;
        } catch (NumberFormatException e) {
            throw new TkitException("Task number must be of type int. Received: \"" + trimmed + "\"");
        }
    }
}
