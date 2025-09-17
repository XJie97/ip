package tkit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

/**
 * Integration-style unit tests for {@link CommandProcessor}.
 * Uses only command outputs; does not assume storage emptiness.
 */
class CommandProcessorTest {

    /**
     * Verifies list → add → list includes the new task.
     */
    @Test
    void handle_listThenAddTodo_rendersNewItem() {
        CommandProcessor cp = new CommandProcessor();

        String add = cp.handle("todo restore sanity");
        assertTrue(add.contains("Added:"));
        assertTrue(add.contains("restore sanity"));

        String listNow = cp.handle("list");
        assertTrue(listNow.contains("Here are the tasks in your list:")
                || listNow.contains("There are no entries yet."));
        assertTrue(listNow.contains("restore sanity"));
    }

    /**
     * Verifies deadline/event parsing, search, on-date filter, mark/unmark/delete, and unknown.
     * Uses relative indices computed from current list size to avoid assuming prior state.
     */
    @Test
    void handleDeadlineEventFindOnMarkUnmarkDeleteUnknown() {
        CommandProcessor cp = new CommandProcessor();

        // Add deadline and event
        String d = cp.handle("deadline return book /by 2019-12-02 1800");
        assertTrue(d.contains("return book"));
        String e = cp.handle("event project /from 2019-12-02 1400 /to 2019-12-02 1600");
        assertTrue(e.contains("project"));

        // Find and on-date
        String matches = cp.handle("find book");
        assertTrue(matches.contains("Matching tasks"));
        assertTrue(matches.contains("return book"));

        String on = cp.handle("on 2019-12-02");
        assertTrue(on.contains("Deadlines/events on Dec 2 2019"));

        // Add a simple todo and compute its index from the current list length
        String addX = cp.handle("todo x");
        assertTrue(addX.contains("Added:"));
        int lastIdx = extractCount(addX);

        String marked = cp.handle("mark " + lastIdx);
        assertTrue(marked.contains("Marked as done:"));
        String unmarked = cp.handle("unmark " + lastIdx);
        assertTrue(unmarked.contains("Marked as not done:"));
        String removed = cp.handle("delete " + lastIdx);
        assertTrue(removed.contains("Removed:"));
        assertTrue(removed.contains("Now you have "));

        String unknown = cp.handle("abracadabra");
        assertTrue(unknown.contains("Unknown command"));
    }

    /**
     * Verifies exit detection logic is based on parsed command.
     */
    @Test
    void isExit_returnsTrueForByeOnly() {
        CommandProcessor cp = new CommandProcessor();
        assertFalse(cp.isExit("todo x"));
        assertTrue(cp.isExit("bye"));
    }

    private static int extractCount(String block) {
        // Block format: "Now you have N task(s) in the list."
        int i = block.indexOf("Now you have ");
        if (i < 0) {
            fail("Could not locate task count in response: " + block);
        }
        int start = i + "Now you have ".length();
        int end = block.indexOf(' ', start);
        assertTrue(end > start, "Malformed count segment");
        return Integer.parseInt(block.substring(start, end));
    }
}
