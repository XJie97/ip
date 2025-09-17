package tkit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Event}.
 */
class EventTest {

    /**
     * Ensures getters and rendering reflect constructor values.
     */
    @Test
    void toString_includesFromAndToDates() {
        LocalDateTime from = LocalDateTime.of(2019, 12, 2, 14, 0);
        LocalDateTime to = LocalDateTime.of(2019, 12, 2, 16, 0);
        Event e = new Event("project meeting", from, to);
        assertEquals(from, e.getFromDate());
        assertEquals(to, e.getToDate());
        String s = e.toString();
        assertTrue(s.startsWith("[E][ ]"));
        assertTrue(s.contains("project meeting"));
        assertTrue(s.contains("Dec 2 2019 14:00"));
        assertTrue(s.contains("Dec 2 2019 16:00"));
    }
}
