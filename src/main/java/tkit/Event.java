package tkit;

import java.time.LocalDateTime;

/** Task type describing a time-ranged event. */
class Event extends Task {
    private final LocalDateTime fromDate;
    private final LocalDateTime toDate;

    /**
     * Creates an event task.
     *
     * @param description user-visible text
     * @param fromDate start date/time
     * @param toDate end date/time
     */
    public Event(String description, LocalDateTime fromDate, LocalDateTime toDate) {
        super(TaskType.EVENT, description);
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    /** Returns the start date/time. */
    public LocalDateTime getFromDate() {
        return fromDate;
    }

    /** Returns the end date/time. */
    public LocalDateTime getToDate() {
        return toDate;
    }

    @Override
    public String toString() {
        return super.toString()
                + " (fromDate: "
                + DateTimeUtil.pretty(fromDate)
                + " toDate: "
                + DateTimeUtil.pretty(toDate)
                + ")";
    }
}
