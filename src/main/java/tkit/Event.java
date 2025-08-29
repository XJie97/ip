package tkit;

import java.time.LocalDateTime;

/** Task type describing a time-ranged event. */
class Event extends Task {
    private final LocalDateTime from;
    private final LocalDateTime to;

    /**
     * Creates an event task.
     *
     * @param description user-visible text
     * @param from start date/time
     * @param to end date/time
     */
    public Event(String description, LocalDateTime from, LocalDateTime to) {
        super(TaskType.EVENT, description);
        this.from = from;
        this.to = to;
    }

    /** Returns the start date/time. */
    public LocalDateTime getFrom() {
        return from;
    }

    /** Returns the end date/time. */
    public LocalDateTime getTo() {
        return to;
    }

    @Override
    public String toString() {
        return super.toString()
                + " (from: "
                + DateTimeUtil.pretty(from)
                + " to: "
                + DateTimeUtil.pretty(to)
                + ")";
    }
}
