package tkit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

final class CommandProcessor {
    private final Storage storage = new Storage();
    private final TaskList tasks;

    CommandProcessor() {
        this.tasks = new TaskList(storage.load());
    }

    boolean isExit(String rawLine) {
        Parser.SplitCommand parsed = Parser.parse(rawLine);
        return parsed.command == Command.BYE;
    }

    String handle(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        Parser.SplitCommand parsed = Parser.parse(line);

        try {
            switch (parsed.command) {
            case BYE:
                return block("Goodbye, fellow adult!");

            case LIST:
                return renderList(tasks.view());

            case TODO: {
                String description = parsed.argOrEmpty().trim();
                if (description.isEmpty()) {
                    return err("Todo requires a description.\nUse: todo <DESCRIPTION>");
                }
                Task task = new Todo(description);
                tasks.add(task);
                storage.save(tasks.view());
                return added(task, tasks.size());
            }

            case DEADLINE: {
                String body = parsed.argOrEmpty().trim();
                String[] parts = body.split("\\s*/by\\s*", 2);
                if (parts.length < 2 || parts[0].trim().isEmpty() || parts[1].trim().isEmpty()) {
                    return err("Wrong deadline input format.\n"
                            + "Use: deadline <TASK> /by <DATE_OR_DATE_TIME>\n"
                            + "Examples: 2019-12-02 1800  |  2019-12-02  |  2/12/2019 1800");
                }
                LocalDateTime by = DateTimeUtil.tryParseToLdt(parts[1].trim());
                if (by == null) {
                    return err("Unrecognized date/time: \"" + parts[1].trim() + "\"\n"
                            + "Use: deadline <TASK> /by <DATE_OR_DATE_TIME>");
                }
                Task task = new Deadline(parts[0].trim(), by);
                tasks.add(task);
                storage.save(tasks.view());
                return added(task, tasks.size());
            }

            case EVENT: {
                String body = parsed.argOrEmpty().trim();
                String[] first = body.split("\\s*/from\\s*", 2);
                if (first.length < 2 || first[0].trim().isEmpty()) {
                    return err("Wrong event input format.\nUse: event <EVENT> /from <START> /to <END>");
                }
                String[] second = first[1].split("\\s*/to\\s*", 2);
                if (second.length < 2 || second[0].trim().isEmpty() || second[1].trim().isEmpty()) {
                    return err("Wrong event input format.\nUse: event <EVENT> /from <START> /to <END>");
                }
                LocalDateTime from = DateTimeUtil.tryParseToLdt(second[0].trim());
                LocalDateTime to = DateTimeUtil.tryParseToLdt(second[1].trim());
                if (from == null || to == null) {
                    return err("Unrecognized date/time.\nUse: event <EVENT> /from <START> /to <END>");
                }
                Task task = new Event(first[0].trim(), from, to);
                tasks.add(task);
                storage.save(tasks.view());
                return added(task, tasks.size());
            }

            case MARK: {
                int idx = parseIndex(parsed.argOrEmpty(), tasks.size());
                tasks.mark(idx);
                storage.save(tasks.view());
                return block("Marked as done:\n  " + tasks.get(idx));
            }

            case UNMARK: {
                int idx = parseIndex(parsed.argOrEmpty(), tasks.size());
                tasks.unmark(idx);
                storage.save(tasks.view());
                return block("Marked as not done:\n  " + tasks.get(idx));
            }

            case DELETE: {
                String arg = parsed.argOrEmpty().trim();
                if (arg.isEmpty()) {
                    return err("Delete requires an index.\nUse: delete <TASK_NUMBER>");
                }
                int idx = parseIndex(arg, tasks.size());
                Task removed = tasks.removeAt(idx);
                storage.save(tasks.view());
                return block("Removed:\n  " + removed + "\nNow you have " + tasks.size() + " task(s).");
            }

            case ON: {
                String raw = parsed.argOrEmpty().trim();
                LocalDate target = DateTimeUtil.tryParseToLocalDate(raw);
                if (target == null) {
                    return err("Unrecognized date.\nUse: on <DATE or DATE TIME>\nExamples: on 2019-12-02 | on 2/12/2019");
                }
                List<Task> hits = tasks.onDate(target);
                if (hits.isEmpty()) {
                    return block("No deadlines/events on " + DateTimeUtil.pretty(target) + ".");
                }
                StringBuilder sb = new StringBuilder();
                sb.append("Deadlines/events on ").append(DateTimeUtil.pretty(target)).append(":\n");
                for (int i = 0; i < hits.size(); i++) {
                    sb.append(i + 1).append(". ").append(hits.get(i)).append('\n');
                }
                return block(sb.toString().trim());
            }

            case FIND: {
                String keyword = parsed.argOrEmpty().trim();
                if (keyword.isEmpty()) {
                    return err("Find requires a keyword.\nUse: find <KEYWORD>");
                }
                List<Task> hits = tasks.find(keyword);
                if (hits.isEmpty()) {
                    return block("No matching tasks found.");
                }
                StringBuilder sb = new StringBuilder("Matching tasks:\n");
                for (int i = 0; i < hits.size(); i++) {
                    sb.append(i + 1).append(". ").append(hits.get(i)).append('\n');
                }
                return block(sb.toString().trim());
            }

            case UNKNOWN:
            default:
                return err("Unknown command: \"" + line + "\".\n"
                        + "Try: list, todo, deadline, event, mark N, unmark N, delete N, on <DATE>, find <KEYWORD>, bye.");
            }
        } catch (TkitException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("Error: " + e.getMessage());
        }
    }

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

    private static String renderList(List<Task> tasks) {
        StringBuilder sb = new StringBuilder();
        if (tasks.isEmpty()) {
            sb.append("There are no entries yet.");
        } else {
            sb.append("Here are the tasks in your list:\n");
            for (int i = 0; i < tasks.size(); i++) {
                sb.append(i + 1).append(". ").append(tasks.get(i)).append('\n');
            }
        }
        return block(sb.toString().trim());
    }

    private static String block(String body) {
        return "____________________\n" + body + "\n____________________";
    }

    private static String err(String body) {
        return block(body);
    }

    private static String added(Task t, int total) {
        return block("Added:\n  " + t + "\nNow you have " + total + " task(s) in the list.");
    }
}
