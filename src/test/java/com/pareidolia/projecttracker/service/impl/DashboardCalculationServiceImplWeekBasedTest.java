package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiService;
import com.pareidolia.projecttracker.service.dashboard.HistoryService;
import com.pareidolia.projecttracker.service.dashboard.MonthlyProjectSummaryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardCalculationServiceImplWeekBasedTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private DashboardKpiService dashboardKpiService;

    @Mock
    private MonthlyProjectSummaryService monthlyProjectSummaryService;

    @InjectMocks
    private DashboardCalculationServiceImpl calculationService;

    @Test
    void shouldStopAssignedPtdAtSelectedWeek() {

        String projectName = "Week Cutoff Project";

        Project weekOne = project(
                projectName,
                "Jan 2026",
                1,
                "01-07",
                10,
                "02 Jan 2026",
                0,
                0
        );

        Project selectedWeek = project(
                projectName,
                "Jan 2026",
                2,
                "08-14",
                20,
                "09 Jan 2026",
                0,
                0
        );

        Project futureWeek = project(
                projectName,
                "Jan 2026",
                3,
                "15-21",
                300,
                "16 Jan 2026",
                0,
                0
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(weekOne, selectedWeek, futureWeek));

        DashboardPtdResponseDto result = calculationService.calculateProjectPtd(
                projectName,
                "Jan 2026",
                "08-14"
        );

        assertEquals(20, result.getFreshCases());
        assertEquals(20, result.getAssignedWeekly());
        assertEquals(30, result.getAssignedPtd());
    }

    @Test
    void shouldAccumulateSubmittedAndIssuePtdThroughSelectedWeek() {

        String projectName = "Week PTD Project";

        Project decemberWeek = project(
                projectName,
                "Dec 2025",
                5,
                "22-31",
                0,
                null,
                2,
                3
        );

        Project januaryPeriodZero = project(
                projectName,
                "Jan 2026",
                0,
                "",
                0,
                null,
                null,
                3
        );

        Project januaryWeekOne = project(
                projectName,
                "Jan 2026",
                1,
                "01-07",
                0,
                null,
                null,
                2
        );

        Project selectedWeek = project(
                projectName,
                "Jan 2026",
                2,
                "08-14",
                0,
                null,
                5,
                4
        );

        Project futureWeek = project(
                projectName,
                "Jan 2026",
                3,
                "15-21",
                0,
                null,
                50,
                100
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(
                        decemberWeek,
                        januaryPeriodZero,
                        januaryWeekOne,
                        selectedWeek,
                        futureWeek
                ));

        DashboardPtdResponseDto result = calculationService.calculateProjectPtd(
                projectName,
                "Jan 2026",
                "08-14"
        );

        assertEquals(5, result.getSubmittedWeekly());
        assertEquals(7, result.getSubmittedPtd());
        assertEquals(4, result.getIssueWeekly());
        assertEquals(9, result.getIssuePtd());
    }

    @Test
    void shouldTreatPositiveAssignedWithBlankAssignedDateAsFreshForWeekBasedCalculation() {

        String projectName = "Blank Date Week Project";

        Project selectedWeek = project(
                projectName,
                "Jan 2026",
                1,
                "01-07",
                14,
                "",
                0,
                0
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(selectedWeek));

        DashboardPtdResponseDto result = calculationService.calculateProjectPtd(
                projectName,
                "Jan 2026",
                "01-07"
        );

        assertEquals(14, result.getFreshCases());
        assertEquals(14, result.getAssignedWeekly());
        assertEquals(0, result.getCarryForwardedCases());
        assertEquals(14, result.getAssignedPtd());
    }

    private Project project(
            String projectName,
            String reportingMonth,
            Integer reportingPeriod,
            String weekOf,
            Integer assigned,
            String assignedDate,
            Integer submitted,
            Integer issue) {

        return Project.builder()
                .projectName(projectName)
                .reportingMonth(reportingMonth)
                .reportingMonthDate(LocalDate.of(2026, 1, 1))
                .reportingPeriod(reportingPeriod)
                .weekOf(weekOf)
                .assignedToPareidolia(assigned)
                .assignedDate(assignedDate)
                .annotationsSubmittedPareidolia(submitted)
                .issueCases(issue)
                .casesMovedToLabels(0)
                .annotationsCompleted(0)
                .build();
    }
}
