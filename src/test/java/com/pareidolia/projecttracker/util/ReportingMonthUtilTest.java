package com.pareidolia.projecttracker.util;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ReportingMonthUtilTest {

    @Test
    void shouldParseSupportedCompactAndFullMonthLabels() {

        assertEquals(
                LocalDate.of(2025, 2, 1),
                ReportingMonthUtil.parse("Feb2025")
        );
        assertEquals(
                LocalDate.of(2025, 4, 1),
                ReportingMonthUtil.parse("APR2025")
        );
        assertEquals(
                LocalDate.of(2026, 7, 1),
                ReportingMonthUtil.parse("July, 2026")
        );
    }

    @Test
    void shouldNormalizeSupportedMonthLabelsToShortMonthFormat() {

        assertEquals("Feb 2025", ReportingMonthUtil.normalize("Feb2025"));
        assertEquals("Apr 2025", ReportingMonthUtil.normalize("APR2025"));
        assertEquals("Jul 2026", ReportingMonthUtil.normalize("July, 2026"));
    }

    @Test
    void shouldReturnNullForNullOrBlankParseInput() {

        assertNull(ReportingMonthUtil.parse(null));
        assertNull(ReportingMonthUtil.parse(""));
        assertNull(ReportingMonthUtil.normalize(null));
        assertEquals("", ReportingMonthUtil.normalize(""));
    }

    @Test
    void shouldRejectUnsupportedReportingMonthLabels() {

        assertThrows(
                IllegalArgumentException.class,
                () -> ReportingMonthUtil.parse("not a reporting month")
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ReportingMonthUtil.normalize("not a reporting month")
        );
    }
}
