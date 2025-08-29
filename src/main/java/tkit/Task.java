package tkit;

/** Common base type for all tasks. */
abstract class Task {
    protected final String description;
    protected Status status;
    protected final TaskType type;

    /**
     * Constructs a task with a type and description.
     *
     * @param type task category
     * @param description user-visible text
     */
    protected Task(TaskType type, String description) {
        this.type = type;
        this.description = description;
        this.status = Status.NOT_DONE;
    }

    /** Marks this task as done. */
    public void markAsDone() {
        this.status = Status.DONE;
    }

    /** Marks this task as not done. */
    public void markAsUndone() {
        this.status = Status.NOT_DONE;
    }

    /** Renders as {@code [Type][State] Description}. */
    @Override
    public String toString() {
        return "[" + type.tag() + "][" + status.stateIcon() + "] " + description;
    }
}
