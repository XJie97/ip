package tkit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Scanner;

/**
 * Primary entry point and command loop for the Tkit task manager.
 *
 * Usage:
 * # Compile from the project root:
 * javac -d out src/main/java/tkit/*.java
 * # Run the program
 * java -cp out tkit.Tkit
 *
 * Level 9: Find
 * - Read and parse user commands
 * - Mutate the in-memory task list
 * - Persist changes to disk immediately after each mutation
 * - Load tasks from disk on startup
 * - Interpret dates/times and render them in a human-friendly format
 * - Change chatbot personality to be more suitable for a task manager.
 * Notes:
 * - Invalid inputs do not terminate the app; they produce an error message and continue.
 * - Dates are stored in ISO-8601.
 */
public class Tkit {

    /** Identity banner line. */
    private static final String IDENTITY = "not three kids in a trench coat";

    /** Storage component handling serialization and durability. Mutable → lowerCamelCase. */
    private static final Storage storage = new Storage();

    /**
     * Reads input, executes commands, persists changes, and prints results.
     * Exits when the user enters {@code bye}.
     *
     * @param args unused
     */
    public static void main(String[] args) {
        // --- startup banner ---
        System.out.println("____________________ \n\nHello from  " + IDENTITY);
        System.out.println("What do you mean we are three kids in a trench coat?");
        System.out.println("We are an adult person! \n____________________\n");

        // --- load existing tasks (if any) ---
        List<Task> tasks = storage.load();

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
                            printError("I do not understand this input format\n"
                                    + "Todo requires a description."
                                    + "Please try Format: todo <DESCRIPTION>");
                            break;
                        }

                        Task task = new Todo(description);
                        tasks.add(task);
                        storage.save(tasks);
                        printAdded(task, tasks.size());
                        break;
                    }

                    case DEADLINE: {
                        String body = parsed.argOrEmpty().trim();

                        String[] parts = body.split("\\s*/by\\s*", 2);
                        if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                            printError("I do not understand this input format\n"
                                    + "Wrong deadline input format.\n"
                                    + "Please try format: deadline <TASK> /by <DATE_OR_DATE_TIME>\n"
                                    + "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                            break;
                        }

                        LocalDateTime by = DateTimeUtil.tryParseToLdt(parts[1].trim());
                        if (by == null) {
                            printError("I do not recognize this date/time format: \""
                                    + parts[1].trim()
                                    + "\"\nPlease try format: deadline <TASK> /by <DATE_OR_DATE_TIME>\n"
                                    + "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                            break;
                        }

                        Task task = new Deadline(parts[0].trim(), by);
                        tasks.add(task);
                        storage.save(tasks);
                        printAdded(task, tasks.size());
                        break;
                    }

                    case EVENT: {
                        String body = parsed.argOrEmpty().trim();

                        String[] firstSplit = body.split("\\s*/from\\s*", 2);
                        if (firstSplit.length < 2 || firstSplit[0].trim().isEmpty()) {
                            printError("I do not understand this input format.\n"
                                    + "Wrong event input format.\n"
                                    + "Please try format: event <EVENT> /from <START> /to <END>");
                            break;
                        }

                        String[] secondSplit = firstSplit[1].split("\\s*/to\\s*", 2);
                        if (secondSplit.length < 2
                                || secondSplit[0].trim().isEmpty()
                                || secondSplit[1].trim().isEmpty()) {
                            printError("I do not understand this input format.\n"
                                    + "Wrong event input format.\n"
                                    + "Use: event <EVENT> /from <START> /to <END>");
                            break;
                        }

                        LocalDateTime from = DateTimeUtil.tryParseToLdt(secondSplit[0].trim());
                        LocalDateTime to = DateTimeUtil.tryParseToLdt(secondSplit[1].trim());
                        if (from == null || to == null) {
                            printError("I do not understand this input format.\n"
                                    + "Use: event <EVENT> /from <START> /to <END>\n"
                                    + "Examples: 2019-12-02 1400  |  2019-12-02  |  2/12/2019 1600");
                            break;
                        }

                        Task task = new Event(firstSplit[0].trim(), from, to);
                        tasks.add(task);
                        storage.save(tasks);
                        printAdded(task, tasks.size());
                        break;
                    }

                    case MARK: {
                        int index = parseIndex(parsed.argOrEmpty(), tasks.size());
                        Task task = tasks.get(index);
                        task.markAsDone();
                        storage.save(tasks);
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
                        storage.save(tasks);
                        System.out.println("____________________\n");
                        System.out.println("OK, I've marked this task as not done yet:");
                        System.out.println("  " + task);
                        System.out.println("____________________\n");
                        break;
                    }

                    case DELETE: {
                        String arg = parsed.argOrEmpty();
                        if (arg.isEmpty()) {
                            printError("I do not understand this input format.\n"
                                    + "Delete requires an index. "
                                    + "Please try format: delete <TASK_NUMBER>");
                            break;
                        }

                        int index = parseIndex(arg, tasks.size());
                        Task removed = tasks.remove(index);
                        storage.save(tasks);
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
                            printError("Unrecognized date.\nUse: on <DATE or DATE TIME>\n"
                                    + "Examples: on 2019-12-02  |  on 2/12/2019");
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
                            System.out.println("No deadlines/events on "
                                    + DateTimeUtil.pretty(targetDate)
                                    + ".");
                        } else {
                            System.out.println("Deadlines/events on "
                                    + DateTimeUtil.pretty(targetDate)
                                    + ":");
                            for (int i = 0; i < hits.size(); i++) {
                                System.out.println((i + 1) + ". " + hits.get(i));
                            }
                        }
                        System.out.println("____________________\n");
                        break;
                    }

                    case FIND: {
                        String keyword = parsed.argOrEmpty().trim();
                        if (keyword.isEmpty()) {
                            printError("I do not understand this input format.\n"
                                    + "Find requires a keyword. "
                                    + "Use: find <KEYWORD>");
                            break;
                        }

                        List<Task> hits = new ArrayList<>();
                        for (Task t : tasks) {
                            if (t.containsKeyword(keyword)) {
                                hits.add(t);
                            }
                        }

                        System.out.println("____________________\n");
                        if (hits.isEmpty()) {
                            System.out.println("No matching tasks found.");
                        } else {
                            System.out.println("Here are the matching tasks in your list:");
                            for (int i = 0; i < hits.size(); i++) {
                                System.out.println((i + 1) + ". " + hits.get(i));
                            }
                        }
                        System.out.println("____________________\n");
                        break;
                    }


                    case UNKNOWN:
                    default:
                        printError("I do not understand this command: \""
                                + rawLine
                                + "\".\nTry: list, todo, deadline, event, mark N, unmark N, delete N, bye.");
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

    /** Prints the standard "added" notification for a newly appended task. */
    private static void printAdded(Task taskAdded, int totalCount) {
        System.out.println("____________________\n");
        System.out.println("Got it. I've added this task:");
        System.out.println("  " + taskAdded);
        System.out.println("Now you have " + totalCount + " tasks in the list.");
        System.out.println("____________________\n");
    }

    /** Prints an error notification with the given message. */
    private static void printError(String message) {
        System.out.println("____________________\n");
        System.out.println(message);
        System.out.println("____________________\n");
    }

    /**
     * Parses a 1-based index from user input and validates it for the current list size.
     *
     * @param userSuppliedIndex string containing a 1-based index
     * @param currentSize current task list size
     * @return zero-based index within [0, currentSize)
     * @throws TkitException if parsing fails or the index is out of range
     */
    private static int parseIndex(String userSuppliedIndex, int currentSize) throws TkitException {
        String trimmed = userSuppliedIndex.trim();
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
