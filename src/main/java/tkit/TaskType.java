package tkit;

/** Supported task categories. Each value carries a single-letter tag. */
enum TaskType {
    TODO("T"),
    DEADLINE("D"),
    EVENT("E");

    private final String tag;

    TaskType(String tag) {
        this.tag = tag;
    }

    /** Returns the single-letter tag for this type. */
    public String tag() {
        return tag;
    }
}
