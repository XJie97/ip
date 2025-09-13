package tkit;

/** Command keywords recognized by the parser. */
enum Command {
    BYE("bye"),
    UNKNOWN(""),
    LIST("list"),
    TODO("todo"),
    DEADLINE("deadline"),
    EVENT("event"),
    MARK("mark"),
    UNMARK("unmark"),
    DELETE("delete"),
    ON("on"),
    FIND("find");

    private final String keyword;

    Command(String keyword) {
        assert keyword != null : "Command keyword must not be null";
        this.keyword = keyword;
    }

    /** Returns the keyword string that triggers this command. */
    public String keyword() {
        return keyword;
    }

    /**
     * Maps the first token of an input line to a {@code Command}.
     *
     * @param input token (case-insensitive)
     * @return matching command or {@code UNKNOWN}
     */
    public static Command fromInput(String input) {
        String s = input == null ? "" : input.toLowerCase();
        assert s.equals(s.toLowerCase()) : "Lowercasing should be idempotent";
        for (Command c : values()) {
            if (!c.keyword.isEmpty() && c.keyword.equals(s)) {
                return c;
            }
        }
        return UNKNOWN;
    }
}
