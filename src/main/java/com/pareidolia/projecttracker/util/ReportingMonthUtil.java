package com.pareidolia.projecttracker.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.util.Locale;

public final class ReportingMonthUtil {

    private ReportingMonthUtil() {
    }

    private static final DateTimeFormatter THREE_LETTER_MONTH =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMMyyyy")
                    .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter FULL_MONTH =
            new DateTimeFormatterBuilder()
                    .parseCaseInsensitive()
                    .appendPattern("MMMMyyyy")
                    .toFormatter(Locale.ENGLISH);

    private static final DateTimeFormatter NORMALIZED_FORMAT =
            DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

    /**
     * Converts different sheet/tab naming formats into
     * a consistent reporting month format.
     *
     * Examples:
     *
     * Feb2025       -> Feb 2025
     * APR2025       -> Apr 2025
     * MAR2025       -> Mar 2025
     * July2025      -> Jul 2025
     * August2025    -> Aug 2025
     * Feb 2026      -> Feb 2026
     * July, 2026    -> Jul 2026
     */
    public static String normalize(String sheetName) {

        YearMonth yearMonth = parseYearMonth(sheetName);

        if (yearMonth == null) {
            return sheetName;
        }

        return yearMonth.format(NORMALIZED_FORMAT);
    }

    /**
     * Converts the sheet/tab name into the first day
     * of that reporting month.
     *
     * Example:
     *
     * July, 2026 -> 2026-07-01
     * Feb2025    -> 2025-02-01
     * APR2025    -> 2025-04-01
     */
    public static LocalDate parse(String sheetName) {

        YearMonth yearMonth = parseYearMonth(sheetName);

        if (yearMonth == null) {
            return null;
        }

        return yearMonth.atDay(1);
    }

    /**
     * Parses the different naming formats used by the
     * Google Sheet tabs.
     */
    private static YearMonth parseYearMonth(String sheetName) {

        if (sheetName == null || sheetName.isBlank()) {
            return null;
        }

        String value = sheetName
                .replace(",", "")
                .replaceAll("\\s+", "")
                .trim();

        try {
            return YearMonth.parse(value, THREE_LETTER_MONTH);
        } catch (DateTimeParseException ignored) {
            // Try full month name below
        }

        try {
            return YearMonth.parse(value, FULL_MONTH);
        } catch (DateTimeParseException ignored) {
            throw new IllegalArgumentException(
                    "Unsupported reporting month format: " + sheetName
            );
        }
    }
}