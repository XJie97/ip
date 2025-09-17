package tkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link TaskType}.
 */
class TaskTypeTest {

    /**
     * Ensures tags are the expected one-letter codes.
     */
    @Test
    void tag_returnsSingleLetter() {
        assertEquals("T", TaskType.TODO.tag());
        assertEquals("D", TaskType.DEADLINE.tag());
        assertEquals("E", TaskType.EVENT.tag());
    }
}
