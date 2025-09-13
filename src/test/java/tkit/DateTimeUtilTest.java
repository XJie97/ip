package tkit;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Month;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link DateTimeUtil}.
 */
class DateTimeUtilTest {

    /**
     * Verifies accepted input formats parse to the expected {@link LocalDateTime}.
     */
    @Test
    void parseToLdt_acceptsIsoAndSlashFormats_withOptionalTime() {
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0),
                DateTimeUtil.parseToLdt("2019-12-02 1800"));
        assertEquals(LocalDateTime.of(2019, 12, 2, 0, 0),
                DateTimeUtil.parseToLdt("2019-12-02"));
        assertEquals(LocalDateTime.of(2019, 12, 2, 18, 0),
                DateTimeUtil.parseToLdt("2/12/2019 1800"));
        assertEquals(LocalDateTime.of(2019, 12, 2, 0, 0),
                DateTimeUtil.parseToLdt("2/12/2019"));
    }

    /**
     * Verifies pretty-printing hides midnight and shows time for non-midnight.
     */
    @Test
    void pretty_formatsDateOrDateTime() {
        LocalDateTime midnight = LocalDateTime.of(2020, Month.JANUARY, 5, 0, 0);
        LocalDateTime sixPm = LocalDateTime.of(2020, Month.JANUARY, 5, 18, 0);
        assertEquals("Jan 5 2020", DateTimeUtil.pretty(midnight));
        assertEquals("Jan 5 2020 18:00", DateTimeUtil.pretty(sixPm));
        assertEquals("Jan 5 2020", DateTimeUtil.pretty(LocalDate.of(2020, 1, 5)));
    }

    /**
     * Verifies date intersection by calendar date including swapped endpoints.
     */
    @Test
    void dateIntersects_inclusiveAndSwapsEndpoints() {
        LocalDateTime a = LocalDateTime.of(2020, 1, 10, 10, 0);
        LocalDateTime b = LocalDateTime.of(2020, 1, 12, 9, 0);
        assertTrue(DateTimeUtil.dateIntersects(LocalDate.of(2020, 1, 10), a, b));
        assertTrue(DateTimeUtil.dateIntersects(LocalDate.of(2020, 1, 11), b, a)); // swapped
        assertTrue(DateTimeUtil.dateIntersects(LocalDate.of(2020, 1, 12), a, b));
        assertFalse(DateTimeUtil.dateIntersects(LocalDate.of(2020, 1, 9), a, b));
        assertFalse(DateTimeUtil.dateIntersects(LocalDate.of(2020, 1, 13), a, b));
    }

    /**
     * Verifies tolerant parsing helpers return null on failure.
     */
    @Test
    void tryParsers_returnNullOnFailure() {
        assertNull(DateTimeUtil.tryParseToLdt("bad"));
        assertNull(DateTimeUtil.tryParseStorageOrInput("still-bad"));
        assertNull(DateTimeUtil.tryParseToLocalDate("nonsense"));
    }

    /**
     * Verifies storage format round-trip.
     */
    @Test
    void storage_roundTrip() {
        LocalDateTime ldt = LocalDateTime.of(2021, 6, 1, 7, 30);
        String stored = DateTimeUtil.toStorage(ldt);
        assertEquals(ldt, DateTimeUtil.parseStorageOrInput(stored));
    }
}
