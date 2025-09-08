package tkit;

/**
 * Parses raw user input lines into structured commands and arguments.
 * Responsibilities:
 *   Tokenize input into a {@link SplitCommand}
 *   Map the first token to a {@link Command}
 * Parsing is case-insensitive for command keywords; the remainder is preserved.
 */
final class Parser {

    private Parser() { }

    /**
     * Lightweight representation of a parsed command line.
     * Consists of a {@link Command} and the raw remainder string.
     */
    static final class SplitCommand {
        final Command command;
        final String remainder;

        SplitCommand(Command command, String remainder) {
            this.command = command;
            this.remainder = remainder == null ? "" : remainder;
        }

        String argOrEmpty() {
            return remainder;
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
}
