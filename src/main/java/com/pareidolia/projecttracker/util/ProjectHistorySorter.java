package com.pareidolia.projecttracker.util;

import com.pareidolia.projecttracker.entity.Project;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class ProjectHistorySorter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("MMMM, yyyy", Locale.ENGLISH);

    public static void sort(List<Project> projects){

        projects.sort(
                Comparator
                        .comparing(ProjectHistorySorter::getYearMonth)
                        .thenComparing(ProjectHistorySorter::getReportingPeriod)
                        .thenComparing(ProjectHistorySorter::getWeekStart)
        );

    }

    private static YearMonth getYearMonth(Project project) {

        String reportingMonth = project.getReportingMonth()
                .replace(",", "")
                .trim();

        DateTimeFormatter[] formatters = {

                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("MMMM yyyy")
                        .toFormatter(Locale.ENGLISH),

                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("MMM yyyy")
                        .toFormatter(Locale.ENGLISH),

                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("MMMMyyyy")
                        .toFormatter(Locale.ENGLISH),

                new DateTimeFormatterBuilder()
                        .parseCaseInsensitive()
                        .appendPattern("MMMyyyy")
                        .toFormatter(Locale.ENGLISH)
        };

        for (DateTimeFormatter formatter : formatters) {

            try {
                return YearMonth.parse(reportingMonth, formatter);

            } catch (Exception ignored) {
                // Try the next format
            }
        }

        throw new RuntimeException(
                "Unsupported reporting month format: " + reportingMonth
        );
    }
    private static Integer getWeekStart(Project project) {

        String week = project.getWeekOf();

        if (week == null || week.trim().isEmpty()) {
            return Integer.MAX_VALUE;
        }

        try {
            return Integer.parseInt(
                    week.trim().split("-")[0]
            );
        } catch (NumberFormatException ex) {

            return Integer.MAX_VALUE;

        }

    }
    private static Integer getReportingPeriod(Project project) {

        Integer period = project.getReportingPeriod();

        return period == null
                ? Integer.MAX_VALUE
                : period;
    }

}