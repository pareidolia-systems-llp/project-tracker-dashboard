package com.pareidolia.projecttracker.util;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

public final class ReportingMonthUtil {

    private ReportingMonthUtil() {
    }

    public static String normalize(String sheetName) {

        if (sheetName == null || sheetName.isBlank()) {
            return sheetName;
        }

        String value = sheetName.replace(",", "").trim();

        DateTimeFormatter input =
                DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH);

        YearMonth yearMonth = YearMonth.parse(value, input);

        return yearMonth.format(input);
    }
}