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

    /**
     * Returns true if this task's description contains the given keyword, ignoring case.
     *
     * @param keyword non-null search term; leading/trailing spaces are ignored
     * @return true if the description contains the keyword (case-insensitive), false otherwise
     */
    public boolean containsKeyword(String keyword) {
        if (keyword == null) {
            return false;
        }
        String matchingKeyword = keyword.trim();
        if (matchingKeyword.isEmpty()) {
            return false;
        }
        return this.description.toLowerCase().contains(matchingKeyword.toLowerCase());
    }

    /** Renders as {@code [Type][State] Description}. */
    @Override
    public String toString() {
        return "[" + type.tag() + "][" + status.stateIcon() + "] " + description;
    }
}
