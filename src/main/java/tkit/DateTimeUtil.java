package tkit;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.temporal.ChronoField;

/**
 * Date/time parsing and formatting helpers for Level 8.
 *
 * Supported input examples:
 *  - 2019-12-02
 *  - 2019-12-02 1800
 *  - 2/12/2019
 *  - 2/12/2019 1800
 *
 * Storage format is ISO-8601 LocalDateTime (e.g., 2019-12-02T18:00).
 * Display format is "MMM d yyyy" or "MMM d yyyy HH:mm" if time is non-midnight.
 */
final class DateTimeUtil {

    // Accept ISO yyyy-MM-dd[ HHmm] OR d/M/yyyy[ HHmm]
    private static final DateTimeFormatter INPUT_FMT = new DateTimeFormatterBuilder()
            // First optional branch: yyyy-MM-dd[ HHmm]
            .appendOptional(new DateTimeFormatterBuilder()
                    .appendPattern("yyyy-MM-dd")
                    .optionalStart()
                    .appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalEnd()
                    .toFormatter())
            // Second optional branch: d/M/yyyy[ HHmm]
            .appendOptional(new DateTimeFormatterBuilder()
                    .appendValue(ChronoField.DAY_OF_MONTH)
                    .appendLiteral('/')
                    .appendValue(ChronoField.MONTH_OF_YEAR)
                    .appendLiteral('/')
                    .appendValue(ChronoField.YEAR)
                    .optionalStart()
                    .appendLiteral(' ')
                    .appendValue(ChronoField.HOUR_OF_DAY, 2)
                    .appendValue(ChronoField.MINUTE_OF_HOUR, 2)
                    .optionalEnd()
                    .toFormatter())
            .toFormatter();

    private static final DateTimeFormatter OUT_DATE =
            DateTimeFormatter.ofPattern("MMM d yyyy");

    private static final DateTimeFormatter OUT_DATE_TIME =
            DateTimeFormatter.ofPattern("MMM d yyyy HH:mm");

    private DateTimeUtil() { }

    /**
     * Strict parse; throws on failure.
     *
     * @param raw input text
     * @return parsed LocalDateTime
     * @throws IllegalArgumentException if parsing fails
     */
    public static LocalDateTime parseToLdt(String raw) {
        assert raw != null && !raw.isBlank() : "parseToLdt(): raw must be non-blank";
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, INPUT_FMT);
        } catch (Exception ignore) {
            // fall through
        }
        try {
            LocalDate d = LocalDate.parse(s, INPUT_FMT);
            return d.atStartOfDay();
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "I do not understand this date/time format\nPlease try this instead: \"" + raw + "\"",
                    e
            );
        }
    }

    /**
     * Non-throwing parse; returns null on failure.
     *
     * @param raw input text
     * @return LocalDateTime or null
     */
    public static LocalDateTime tryParseToLdt(String raw) {
        String s = raw.trim();
        try {
            return LocalDateTime.parse(s, INPUT_FMT);
        } catch (Exception ignore) {
            // fall through
        }
        try {
            LocalDate d = LocalDate.parse(s, INPUT_FMT);
            return d.atStartOfDay();
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * Pretty-print: omit time if midnight, else include HH:mm.
     *
     * @param ldt date-time
     * @return formatted string
     */
    public static String pretty(LocalDateTime ldt) {
        assert ldt != null : "pretty(ldt): null";
        if (ldt.toLocalTime().equals(LocalTime.MIDNIGHT)) {
            return ldt.format(OUT_DATE);
        }
        return ldt.format(OUT_DATE_TIME);
    }

    /** Pretty-print for a LocalDate using the same OUT_DATE pattern. */
    public static String pretty(LocalDate date) {
        assert date != null : "pretty(date): null";
        return date.format(OUT_DATE);
    }

    /**
     * Lossless storage format (ISO-8601).
     *
     * @param ldt date-time
     * @return ISO-8601 string
     */
    public static String toStorage(LocalDateTime ldt) {
        return ldt.toString();
    }

    /**
     * Strict storage parse with fallback to input; throws on failure.
     *
     * @param text text to parse
     * @return LocalDateTime
     * @throws IllegalArgumentException if parsing fails
     */
    public static LocalDateTime parseStorageOrInput(String text) {
        String s = text.trim();
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {
            return parseToLdt(s);
        }
    }

    /**
     * Non-throwing storage parse with fallback to input; returns null on failure.
     *
     * @param text text to parse
     * @return LocalDateTime or null
     */
    public static LocalDateTime tryParseStorageOrInput(String text) {
        String s = text.trim();
        try {
            return LocalDateTime.parse(s);
        } catch (Exception ignore) {
            // fall through
        }
        return tryParseToLdt(s);
    }

    /**
     * Non-throwing parse to LocalDate; returns null on failure.
     *
     * @param raw input text
     * @return LocalDate or null
     */
    public static LocalDate tryParseToLocalDate(String raw) {
        if (raw == null) {
            return null;
        }
        String s = raw.trim();
        // Try full date-time first, then date-only
        try {
            return LocalDateTime.parse(s, INPUT_FMT).toLocalDate();
        } catch (Exception ignore) {
            // fall through
        }
        try {
            return LocalDate.parse(s, INPUT_FMT);
        } catch (Exception ignore) {
            return null;
        }
    }

    /**
     * True if the given calendar date intersects [start, end] by date (inclusive).
     * If start > end, the method swaps them.
     *
     * @param date date to test
     * @param start start date-time (inclusive)
     * @param end end date-time (inclusive)
     * @return true if date is within or at the bounds by calendar date
     */
    public static boolean dateIntersects(LocalDate date, LocalDateTime start, LocalDateTime end) {
        if (start.isAfter(end)) {
            LocalDateTime tmp = start;
            start = end;
            end = tmp;
        }
        LocalDate s = start.toLocalDate();
        LocalDate e = end.toLocalDate();
        return !(date.isBefore(s) || date.isAfter(e));
    }
}
