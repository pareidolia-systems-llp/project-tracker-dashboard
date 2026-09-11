package com.pareidolia.projecttracker.util;

import com.pareidolia.projecttracker.entity.Project;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectHistorySorterTest {

    @Test
    void shouldSortChronologicallyAcrossMonthsAndReportingPeriodsThroughFive() {

        List<Project> history = new ArrayList<>(List.of(
                project(7L, "Feb 2026", 1, "01-07"),
                project(6L, "Jan 2026", 5, ""),
                project(5L, "Jan 2026", 4, "22-31"),
                project(3L, "Jan 2026", 2, "08-14"),
                project(1L, "Jan 2026", 0, ""),
                project(4L, "Jan 2026", 3, "15-21"),
                project(2L, "Jan 2026", 1, "01-07")
        ));

        ProjectHistorySorter.sort(history);

        assertEquals(
                List.of(1L, 2L, 3L, 4L, 5L, 6L, 7L),
                history.stream().map(Project::getId).toList()
        );
    }

    @Test
    void shouldPlaceBlankWeekOfAfterParseableWeekWithinTheSamePeriod() {

        List<Project> history = new ArrayList<>(List.of(
                project(4L, "Jan 2026", 4, "22-31"),
                project(3L, "Jan 2026", 1, ""),
                project(2L, "Jan 2026", 1, "01-07"),
                project(1L, "Jan 2026", 0, "")
        ));

        ProjectHistorySorter.sort(history);

        assertEquals(
                List.of(1L, 2L, 3L, 4L),
                history.stream().map(Project::getId).toList()
        );
    }

    @Test
    void shouldAcceptSupportedReportingMonthLabelVariants() {

        List<Project> history = new ArrayList<>(List.of(
                project(4L, "Jul2026", 4, "22-31"),
                project(2L, "Jul 2026", 2, "08-14"),
                project(1L, "July, 2026", 1, "01-07"),
                project(3L, "July2026", 3, "15-21")
        ));

        ProjectHistorySorter.sort(history);

        assertEquals(
                List.of(1L, 2L, 3L, 4L),
                history.stream().map(Project::getId).toList()
        );
    }

    private Project project(
            Long id,
            String reportingMonth,
            Integer reportingPeriod,
            String weekOf) {

        return Project.builder()
                .id(id)
                .projectName("Sorter Project")
                .reportingMonth(reportingMonth)
                .reportingPeriod(reportingPeriod)
                .weekOf(weekOf)
                .build();
    }
}
