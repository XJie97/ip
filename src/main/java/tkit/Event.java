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
        assert fromDate != null && toDate != null : "Event endpoints must not be null";
        assert !toDate.isBefore(fromDate) : "Event end must be >= start";
        this.fromDate = fromDate;
        this.toDate = toDate;
    }

    /** Returns the start date/time. */
    public LocalDateTime getFromDate() {
        assert fromDate != null;
        return fromDate;
    }

    /** Returns the end date/time. */
    public LocalDateTime getToDate() {
        assert toDate != null;
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
