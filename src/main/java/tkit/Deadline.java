package tkit;

import java.time.LocalDateTime;

/** Task type with a deadline date/time. */
class Deadline extends Task {
    private final LocalDateTime dueAt;

    /**
     * Creates a deadline task.
     *
     * @param description user-visible text
     * @param dueAt due date/time
     */
    public Deadline(String description, LocalDateTime dueAt) {
        super(TaskType.DEADLINE, description);

        assert dueAt != null : "Event endpoints must not be null";

        this.dueAt = dueAt;
    }

    /** Returns the due date/time. */
    public LocalDateTime getDueAt() {
        return dueAt;
    }

    @Override
    public String toString() {
        assert dueAt != null : "Deadline must have dueAt";
        return super.toString() + " (by: " + DateTimeUtil.pretty(dueAt) + ")";
    }
}
