package tkit;

/**
 * Parses raw user input lines into structured commands and arguments.
 * Responsibilities:
 *   Tokenize input into a {@link SplitCommand}
 *   Map the first token to a {@link Command}
 * Parsing is case-insensitive for command keywords; the remainder is preserved.
 */
final class Parser {

    /**
     * Lightweight representation of a parsed command line.
     * Consists of a {@link Command} and the raw remainder string.
     */
    static final class SplitCommand {
        final Command command;
        final String remainder;

        /**
         * Constructs a parsed command.
         *
         * @param command parsed command keyword
         * @param remainder remainder of the line (never {@code null})
         */
        SplitCommand(Command command, String remainder) {
            this.command = command;
            this.remainder = remainder == null ? "" : remainder;
        }

        /**
         * Returns the remainder string or the empty string if none.
         *
         * @return non-null remainder
         */
        String argOrEmpty() {
            return remainder;
        }
    }

    /**
     * Command keywords recognized by the parser.
     * Each enum constant carries the canonical triggering keyword.
     */
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
            this.keyword = keyword;
        }

        /**
         * Returns the canonical keyword string for the command.
         *
         * @return keyword used to trigger this command
         */
        String keyword() {
            return keyword;
        }

        /**
         * Maps a token to a {@link Command}, case-insensitively.
         * Returns {@link #UNKNOWN} if no match is found.
         *
         * @param input token to interpret
         * @return matching command or {@link #UNKNOWN}
         */
        static Command fromInput(String input) {
            String s = input == null ? "" : input.toLowerCase();
            for (Command c : values()) {
                if (!c.keyword.isEmpty() && c.keyword.equals(s)) {
                    return c;
                }
            }
            return UNKNOWN;
        }
    }

    /**
     * Parses a full input line into a {@link SplitCommand}.
     * Behavior:
     *   Trims surrounding whitespace
     *   Splits on the first whitespace run into "command token" and "rest"
     *   Classifies the token using {@link Command#fromInput(String)}
     *
     * @param line full input line
     * @return parsed command and remainder; never {@code null}
     */
    static SplitCommand parse(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return new SplitCommand(Command.UNKNOWN, "");
        }
        String[] parts = normalized.split("\\s+", 2);
        Command cmd = Command.fromInput(parts[0]);
        String rest = parts.length > 1 ? parts[1] : "";
        return new SplitCommand(cmd, rest);
    }

    /** Non-instantiable utility class. */
    private Parser() {
    }
}
