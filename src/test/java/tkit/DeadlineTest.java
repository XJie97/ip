package tkit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Deadline}.
 */
class DeadlineTest {

    /**
     * Ensures fields and rendering are consistent.
     */
    @Test
    void toString_includesDueDate() {
        LocalDateTime by = LocalDateTime.of(2019, 12, 2, 18, 0);
        Deadline d = new Deadline("return book", by);
        assertEquals(by, d.getDueDate());
        String s = d.toString();
        assertTrue(s.startsWith("[D][ ]"));
        assertTrue(s.contains("return book"));
        assertTrue(s.contains("Dec 2 2019 18:00"));
    }
}
