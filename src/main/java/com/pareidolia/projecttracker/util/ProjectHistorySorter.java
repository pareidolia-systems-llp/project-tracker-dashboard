package com.pareidolia.projecttracker.util;

import com.pareidolia.projecttracker.entity.Project;

import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
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

                        .thenComparing(ProjectHistorySorter::getWeekStart)
        );

    }

    private static YearMonth getYearMonth(Project project){

        return YearMonth.parse(
                project.getReportingMonth(),
                FORMATTER
        );

    }

    private static Integer getWeekStart(Project project){

        String week = project.getWeekOf();

        return Integer.parseInt(
                week.split("-")[0]
        );

    }

}