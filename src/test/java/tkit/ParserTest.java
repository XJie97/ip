package tkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Parser}.
 */
class ParserTest {

    /**
     * Verifies parsing splits the first token and preserves the remainder.
     */
    @Test
    void parse_splitsTokenAndRemainder() {
        Parser.SplitCommand sc = Parser.parse("deadline return book /by 2019-12-02 1800");
        assertEquals(Command.DEADLINE, sc.command);
        assertEquals("return book /by 2019-12-02 1800", sc.argOrEmpty());
    }

    /**
     * Verifies parsing trims spaces and handles single-token lines.
     */
    @Test
    void parse_trimsAndHandlesSingleToken() {
        Parser.SplitCommand sc = Parser.parse("   list   ");
        assertEquals(Command.LIST, sc.command);
        assertEquals("", sc.argOrEmpty());
    }

    /**
     * Verifies unknown token mapping for empty input.
     */
    @Test
    void parse_empty_becomesUnknown() {
        Parser.SplitCommand sc = Parser.parse("   ");
        assertEquals(Command.UNKNOWN, sc.command);
        assertEquals("", sc.argOrEmpty());
    }
}
