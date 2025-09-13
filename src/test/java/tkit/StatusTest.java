package tkit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link Status}.
 */
class StatusTest {

    /**
     * Ensures state icons are stable.
     */
    @Test
    void stateIcon_returnsOneCharacter() {
        assertEquals("X", Status.DONE.stateIcon());
        assertEquals(" ", Status.NOT_DONE.stateIcon());
    }
}
