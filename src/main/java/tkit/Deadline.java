package tkit;

import java.time.LocalDateTime;

/** Task type with a deadline date/time. */
class Deadline extends Task {
    private final LocalDateTime dueDate;

    /**
     * Creates a deadline task.
     *
     * @param description user-visible text
     * @param dueDate due date/time
     */
    public Deadline(String description, LocalDateTime dueDate) {
        super(TaskType.DEADLINE, description);

        this.dueDate = dueDate;

        assert dueDate != null : "Event endpoints must not be null";

    }

    /** Returns the due date/time. */
    public LocalDateTime getDueDate() {
        return dueDate;
    }

    @Override
    public String toString() {
        assert dueDate != null : "Deadline must have dueAt";
        return super.toString() + " (by: " + DateTimeUtil.pretty(dueDate) + ")";
    }
}
