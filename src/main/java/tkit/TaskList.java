package tkit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;


/**
 * Mutable container for {@link Task} objects that provides domain operations.
 * Responsibilities:
 *   Maintain an ordered list of tasks
 *   Provide add/remove/mark/unmark operations
 *   Provide query helpers (keyword search, by-date filter)
 */
final class TaskList {
    private final List<Task> tasks;

    /** Creates an empty task list. */
    TaskList() {
        this.tasks = new ArrayList<>();
        assert this.tasks.isEmpty();
    }

    /**
     * Creates a task list initialized from an existing collection.
     * The contents are defensively copied.
     *
     * @param initial initial tasks; may be {@code null}
     */
    TaskList(List<Task> initial) {
        this.tasks = new ArrayList<>(initial == null ? List.of() : initial);
        assert this.tasks != null;
    }

    /**
     * Returns the current number of tasks.
     *
     * @return size of the list
     */
    int size() {
        return tasks.size();
    }

    /**
     * True if there are no tasks.
     *
     * @return whether the list is empty
     */
    boolean isEmpty() {
        return tasks.isEmpty();
    }

    /**
     * Returns the task at the given zero-based index.
     *
     * @param idx position in the list
     * @return task at the index
     * @throws IndexOutOfBoundsException if {@code idx} is invalid
     */
    Task get(int idx) {
        assert idx >= 0 && idx < tasks.size() : "get(): index out of bounds";
        return tasks.get(idx);
    }

    /**
     * Returns an unmodifiable view of the current tasks.
     *
     * @return read-only list view
     */
    List<Task> view() {
        return Collections.unmodifiableList(tasks);
    }

    /**
     * Appends a task to the end of the list.
     *
     * @param t task to add
     */
    void add(Task t) {
        assert t != null : "add(): task must not be null";
        int before = tasks.size();
        tasks.add(t);
        assert tasks.size() == before + 1 : "add(): size must increase by 1";
    }

    /**
     * Removes and returns the task at the given index.
     *
     * @param idx zero-based index to remove
     * @return removed task
     * @throws IndexOutOfBoundsException if {@code idx} is invalid
     */
    Task removeAt(int idx) {
        assert idx >= 0 && idx < tasks.size() : "removeAt(): index out of bounds";
        return tasks.remove(idx);
    }

    /**
     * Marks the task at the index as done.
     *
     * @param idx zero-based index
     */
    void mark(int idx) {
        assert idx >= 0 && idx < tasks.size() : "mark(): index out of bounds";
        tasks.get(idx).markAsDone();
    }

    /**
     * Marks the task at the index as not done.
     *
     * @param idx zero-based index
     */
    void unmark(int idx) {
        assert idx >= 0 && idx < tasks.size() : "unmark(): index out of bounds";
        tasks.get(idx).markAsUndone();
    }

    /**
     * Returns tasks whose description contains the keyword, case-insensitively.
     *
     * @param keyword search string; leading/trailing spaces are ignored
     * @return ordered list of matching tasks
     */
    List<Task> find(String keyword) {
        List<Task> hits = new ArrayList<>();
        for (Task t : tasks) {
            assert t != null : "List must not contain null tasks";
            if (t.containsKeyword(keyword)) {
                hits.add(t);
            }
        }
        assert hits.size() <= tasks.size();
        return hits;
    }

    /**
     * Returns tasks that occur on the given calendar date.
     * For a {@link Deadline}, compares the due date. For an {@link Event},
     * returns the task if the date intersects the inclusive range
     * [{@code from}, {@code to}] by calendar date.
     *
     * @param date target date
     * @return ordered list of tasks on the date
     */
    List<Task> onDate(LocalDate date) {
        List<Task> hits = new ArrayList<>();
        for (Task t : tasks) {
            if (t instanceof Deadline) {
                if (((Deadline) t).getDueDate().toLocalDate().equals(date)) {
                    hits.add(t);
                }
            } else if (t instanceof Event) {
                LocalDateTime from = ((Event) t).getFromDate();
                LocalDateTime to = ((Event) t).getToDate();
                if (DateTimeUtil.dateIntersects(date, from, to)) {
                    hits.add(t);
                }
            }
        }
        return hits;
    }

    /**
     * Removes tasks at the given zero-based indices. Indices must be unique and sorted in
     * strictly descending order to avoid reindexing issues. The method assumes indices
     * have already been range-checked by the caller.
     *
     * @param zeroBasedDescending unique indices in strictly descending order
     * @return tasks removed, in the same order as indices provided
     */
    List<Task> removeManyDescending(List<Integer> zeroBasedDescending) {
        assert zeroBasedDescending != null : "removeManyDescending(): indices null";
        // Defensive checks in assertions for development; callers should pre-validate.
        int last = Integer.MAX_VALUE;
        for (Integer idx : zeroBasedDescending) {
            assert idx != null : "removeManyDescending(): null index";
            assert idx >= 0 && idx < tasks.size() : "removeManyDescending(): index out of bounds";
            assert idx < last : "removeManyDescending(): indices must be strictly descending";
            last = idx;
        }
        List<Task> removed = new ArrayList<>();
        for (int idx : zeroBasedDescending) {
            removed.add(tasks.remove(idx));
        }
        return removed;
    }

}
