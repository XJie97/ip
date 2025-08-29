package tkit;

/** Lightweight representation of a parsed command: verb + remainder. */
final class SplitCommand {
    final Command command;
    final String remainder; // may be empty

    private SplitCommand(Command command, String remainder) {
        this.command = command;
        this.remainder = remainder == null ? "" : remainder;
    }

    /**
     * Parses a full input line into command and trailing argument string.
     *
     * @param line full input
     * @return parsed command and remainder (never null)
     */
    public static SplitCommand parse(String line) {
        String normalized = line == null ? "" : line.trim();
        if (normalized.isEmpty()) {
            return new SplitCommand(Command.UNKNOWN, "");
        }
        String[] parts = normalized.split("\\s+", 2);
        Command cmd = Command.fromInput(parts[0]);
        String rest = parts.length > 1 ? parts[1] : "";
        return new SplitCommand(cmd, rest);
    }

    /** Returns the remainder after the command token, or an empty string. */
    public String argOrEmpty() {
        return remainder;
    }
}
