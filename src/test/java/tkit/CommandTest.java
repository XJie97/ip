package tkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Command}.
 */
class CommandTest {

    /**
     * Ensures mapping from case-insensitive token to {@link Command}.
     */
    @Test
    void getCommandFromInput_mapsKnownTokens_caseInsensitive() {
        assertEquals(Command.BYE, Command.getCommandFromInput("bye"));
        assertEquals(Command.BYE, Command.getCommandFromInput("ByE"));
        assertEquals(Command.LIST, Command.getCommandFromInput("LIST"));
        assertEquals(Command.UNKNOWN, Command.getCommandFromInput("nonsense"));
        assertEquals(Command.UNKNOWN, Command.getCommandFromInput(""));
    }

    /**
     * Ensures {@link Command#keyword()} returns the configured keyword.
     */
    @Test
    void keyword_returnsConfiguredKeyword() {
        assertEquals("todo", Command.TODO.keyword());
        assertEquals("", Command.UNKNOWN.keyword());
    }
}
