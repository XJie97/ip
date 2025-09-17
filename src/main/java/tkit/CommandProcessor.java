package tkit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

final class CommandProcessor {
    private final Storage storage = new Storage();
    private final TaskList tasks;

    CommandProcessor() {
        List<Task> loaded = storage.load();
        assert loaded != null : "Storage.load() must return non-null list";
        this.tasks = new TaskList(loaded);
    }

    boolean isExit(String rawLine) {
        Parser.SplitCommand parsed = Parser.parse(rawLine);
        assert parsed != null;
        return parsed.command == Command.BYE;
    }

    String handle(String rawLine) {
        String line = rawLine == null ? "" : rawLine.trim();
        Parser.SplitCommand parsed = Parser.parse(line);
        assert parsed != null : "Parser must not return null";

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
                assert idx >= 0 && idx < tasks.size();
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
                    return err("Delete requires an index.\nUse: delete <TASK_NUMBER> or delete N, M, ...");
                }
                // Accept single or multiple indices seamlessly.
                List<Integer> indices = parseMultipleIndices(arg, tasks.size()); // zero-based, descending, unique
                if (indices.size() == 1) {
                    int idx = indices.get(0);
                    Task removed = tasks.removeAt(idx);
                    storage.save(tasks.view());
                    return block("Removed:\n  " + removed + "\nNow you have " + tasks.size() + " task(s).");
                } else {
                    List<Task> removed = tasks.removeManyDescending(indices);
                    storage.save(tasks.view());
                    return formatRemovedMany(removed, tasks.size());
                }
            }


            case ON: {
                String raw = parsed.argOrEmpty().trim();
                LocalDate target = DateTimeUtil.tryParseToLocalDate(raw);
                if (target == null) {
                    return err("Unrecognized date.\nUse: on <DATE or DATE TIME>\n"
                            + "Examples: on 2019-12-02 | on 2/12/2019");
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
                        + "Try: list, todo, deadline, event, mark N, unmark N, delete N,"
                        + " on <DATE>, find <KEYWORD>, bye.");
            }
        } catch (TkitException e) {
            return err(e.getMessage());
        } catch (Exception e) {
            return err("Error: " + e.getMessage());
        }
    }

    private static int parseIndex(String userSuppliedIndex, int currentSize) throws TkitException {
        assert currentSize >= 0 : "currentSize must be non-negative";
        String trimmed = userSuppliedIndex.trim();
        assert !trimmed.isEmpty() : "index string pre-validated to be non-empty";
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

    /**
     * Parses one or more 1-based indices separated by commas and/or whitespace.
     * Duplicates are de-duplicated. On any out-of-range index, throws with a message
     * listing the offending 1-based numbers and guarantees no mutation should occur.
     */
    private static List<Integer> parseMultipleIndices(String arg, int currentSize) throws TkitException {
        assert currentSize >= 0 : "currentSize must be non-negative";
        String s = arg == null ? "" : arg.trim();
        if (s.isEmpty()) {
            throw new TkitException("Delete requires at least one index. Use: delete <N[, M, ...]>");
        }
        String[] tokens = s.split("[,\\s]+");
        List<Integer> oneBased = new java.util.ArrayList<>();
        for (String tok : tokens) {
            if (tok.isBlank()) {
                continue;
            }
            try {
                oneBased.add(Integer.parseInt(tok));
            } catch (NumberFormatException nfe) {
                throw new TkitException("Task number must be of type int. Received: \"" + tok + "\"");
            }
        }
        if (oneBased.isEmpty()) {
            throw new TkitException("Delete requires at least one index. Use: delete <N[, M, ...]>");
        }
        // Collect out-of-range 1-based numbers
        List<Integer> oob = new java.util.ArrayList<>();
        for (int ob : oneBased) {
            int zb = ob - 1;
            if (zb < 0 || zb >= currentSize) {
                oob.add(ob);
            }
        }
        if (!oob.isEmpty()) {
            String bad = oob.stream()
                    .sorted()
                    .map(String::valueOf)
                    .reduce((a, b) -> a + ", " + b)
                    .orElse("");
            throw new TkitException("These task number(s) do not exist: " + bad + ". No tasks were deleted.");
        }
        // De-duplicate and convert to zero-based descending for safe deletion
        java.util.Set<Integer> uniq = new java.util.HashSet<>(oneBased);
        java.util.List<Integer> zeroBased = new java.util.ArrayList<>();
        for (int ob : uniq) {
            zeroBased.add(ob - 1);
        }
        zeroBased.sort(java.util.Collections.reverseOrder());
        return zeroBased;
    }

    private static String formatRemovedMany(java.util.List<Task> removed, int totalAfter) {
        StringBuilder sb = new StringBuilder();
        sb.append("Removed ").append(removed.size()).append(" task(s):\n");
        for (Task t : removed) {
            sb.append("  ").append(t).append('\n');
        }
        sb.append("Now you have ").append(totalAfter).append(" task(s).");
        return block(sb.toString().trim());
    }


    private static String renderList(List<Task> tasks) {
        assert tasks != null : "renderList(): tasks must not be null";
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
