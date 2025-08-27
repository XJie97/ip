package tkit;

/** Task type representing a simple to-do with only a description. */
class Todo extends Task {
    /** Creates a to-do task with the given description. */
    public Todo(String description) {
        super(TaskType.TODO, description);
    }
}
