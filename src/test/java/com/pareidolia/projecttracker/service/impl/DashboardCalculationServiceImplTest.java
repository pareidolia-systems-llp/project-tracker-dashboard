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
class DashboardCalculationServiceImplTest {

    @Mock
    private HistoryService historyService;

    @Mock
    private DashboardKpiService dashboardKpiService;

    @Mock
    private MonthlyProjectSummaryService monthlyProjectSummaryService;

    @InjectMocks
    private DashboardCalculationServiceImpl calculationService;


    @Test
    void shouldIgnorePeriodZeroAndCountBlankWeekIssueInPeriodFour() {

        String projectName = "Pilot Coronary CTA";

        /*
         * Previous genuine issue history = 117
         */
        Project junePeriod4 = project(
                projectName,
                "June, 2026",
                LocalDate.of(2026, 6, 1),
                4,
                "23-30",
                0,
                null,
                0,
                117
        );

        /*
         * Period 0 = cumulative issue entering July.
         * This 117 must NOT be added again.
         */
        Project julyPeriod0 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                0,
                "",
                0,
                null,
                0,
                117
        );

        Project julyPeriod1 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                1,
                "01-05",
                0,
                "Previous Cases",
                70,
                53
        );

        Project julyPeriod2 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                2,
                "06-12",
                0,
                null,
                203,
                23
        );

        Project julyPeriod3 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                3,
                "13-19",
                0,
                null,
                31,
                21
        );

        /*
         * Important regression case:
         * Period 4 has blank Week Of,
         * but Issue = 4 is genuine and must be counted.
         */
        Project julyPeriod4 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                4,
                "",
                0,
                null,
                15,
                4
        );

        List<Project> history = List.of(
                junePeriod4,
                julyPeriod0,
                julyPeriod1,
                julyPeriod2,
                julyPeriod3,
                julyPeriod4
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(history);

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "July, 2026",
                        4
                );

        /*
         * 117 previous genuine history
         * + 53
         * + 23
         * + 21
         * + 4
         *
         * = 218
         *
         * July Period 0's 117 must NOT be counted again.
         */
        assertEquals(218, result.getIssuePtd());

        /*
         * Current reporting-period issue
         */
        assertEquals(4, result.getIssueWeekly());
    }

    @Test
    void shouldNotDoubleCountCarryForwardIssueInNextMonth() {

        String projectName = "Pilot Coronary CTA";

        /*
         * Genuine issue from January.
         * This is what established the PTD = 21.
         */
        Project januaryPeriod4 = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                4,
                "26-31",
                0,
                null,
                0,
                21
        );

        /*
         * February Period 0 contains the same 21
         * as cumulative/carry-forward context.
         *
         * It must NOT be added again.
         */
        Project februaryPeriod0 = project(
                projectName,
                "Feb 2026",
                LocalDate.of(2026, 2, 1),
                0,
                "",
                0,
                null,
                0,
                21
        );

        Project februaryPeriod1 = project(
                projectName,
                "Feb 2026",
                LocalDate.of(2026, 2, 1),
                1,
                "01-08",
                203,
                "Previous Cases",
                0,
                0
        );

        Project februaryPeriod2 = project(
                projectName,
                "Feb 2026",
                LocalDate.of(2026, 2, 1),
                2,
                "09-15",
                0,
                null,
                0,
                0
        );

        Project februaryPeriod3 = project(
                projectName,
                "Feb 2026",
                LocalDate.of(2026, 2, 1),
                3,
                "16-22",
                0,
                null,
                0,
                0
        );

        Project februaryPeriod4 = project(
                projectName,
                "Feb 2026",
                LocalDate.of(2026, 2, 1),
                4,
                "23-28",
                0,
                null,
                23,
                0
        );

        List<Project> history = List.of(
                januaryPeriod4,
                februaryPeriod0,
                februaryPeriod1,
                februaryPeriod2,
                februaryPeriod3,
                februaryPeriod4
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(history);

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Feb 2026",
                        4
                );

        /*
         * January genuine issue = 21
         * February Period 0 = 21, but it is only carry-forward.
         *
         * Therefore PTD remains 21.
         */
        assertEquals(21, result.getIssuePtd());

        /*
         * No new issue in February Period 4.
         */
        assertEquals(0, result.getIssueWeekly());
    }

    @Test
    void shouldCalculateTroutJulyPeriodFourIncludingBlankWeekValues() {

        String projectName = "Trout_CT_Abdomen_Pelvis(Pilot)";

        /*
         * Simplified June history representing the PTD
         * entering July.
         *
         * Assigned PTD entering July = 525
         * Submitted PTD entering July = 243
         * Issue PTD entering July = 35
         */
        Project junePeriod4 = project(
                projectName,
                "June, 2026",
                LocalDate.of(2026, 6, 1),
                4,
                "23-30",
                525,
                "20 Jun 2026",
                243,
                35
        );

        /*
         * July cumulative/carry-forward Issue context.
         * Must not be added again to Issue PTD.
         */
        Project julyPeriod0 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                0,
                "",
                0,
                null,
                0,
                35
        );

        /*
         * Previous Cases only.
         * Must not increase Assigned PTD.
         */
        Project julyPeriod1 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                1,
                "01-05",
                247,
                "Previous Cases",
                22,
                0
        );

        Project julyPeriod2 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                2,
                "06-12",
                0,
                null,
                60,
                0
        );

        Project julyPeriod3 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                3,
                "13-19",
                0,
                null,
                45,
                15
        );

        /*
         * Actual troublesome source row:
         * Week Of is blank, but the values are genuine.
         */
        Project julyPeriod4 = project(
                projectName,
                "July, 2026",
                LocalDate.of(2026, 7, 1),
                4,
                "",
                219,
                "21.07.2026",
                201,
                14
        );

        List<Project> history = List.of(
                junePeriod4,
                julyPeriod0,
                julyPeriod1,
                julyPeriod2,
                julyPeriod3,
                julyPeriod4
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(history);

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "July, 2026",
                        4
                );

        // Weekly/current-period values
        assertEquals(219, result.getAssignedWeekly());
        assertEquals(219, result.getFreshCases());
        assertEquals(0, result.getCarryForwardedCases());

        assertEquals(201, result.getSubmittedWeekly());
        assertEquals(14, result.getIssueWeekly());

        // PTD values
        assertEquals(744, result.getAssignedPtd());
        assertEquals(571, result.getSubmittedPtd());
        assertEquals(64, result.getIssuePtd());
    }
    @Test
    void shouldCalculatePilotAssignedPtdThroughMarchAs480() {

        String projectName = "Pilot Coronary CTA";

        /*
         * OCTOBER
         * Fresh Assigned = 32
         * Submitted = 5
         * Carry entering November = 27
         */
        Project october = project(
                projectName,
                "Oct 2025",
                LocalDate.of(2025, 10, 1),
                1,
                "01-07",
                32,
                "10 Oct 2025",
                5,
                0
        );

        /*
         * NOVEMBER
         *
         * Raw Assigned = 231
         * Previous carry = 27
         *
         * Fresh = 231 - 27 = 204
         */
        Project november = project(
                projectName,
                "Nov 2025",
                LocalDate.of(2025, 11, 1),
                1,
                "01-07",
                231,
                "Previous Cases + 12th Nov",
                7,
                0
        );

        /*
         * DECEMBER
         * Pure Previous Cases.
         * Fresh = 0.
         */
        Project december = project(
                projectName,
                "Dec 2025",
                LocalDate.of(2025, 12, 1),
                1,
                "01-07",
                224,
                "Previous Cases",
                0,
                0
        );

        /*
         * JANUARY
         * Previous Cases portion = 143
         */
        Project januaryPreviousCases = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-11",
                143,
                "Previous Cases",
                84,
                0
        );

        /*
         * January genuine fresh assignment = 144.
         */
        Project januaryFresh = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                2,
                "12-18",
                144,
                "16 Jan 2026",
                0,
                0
        );

        /*
         * FEBRUARY
         * Previous Cases only = 203.
         * Fresh = 0.
         */
        Project february = project(
                projectName,
                "Feb 2026",
                LocalDate.of(2026, 2, 1),
                1,
                "01-08",
                203,
                "Previous Cases",
                0,
                0
        );

        /*
         * MARCH
         * Previous Cases = 178.
         */
        Project marchPreviousCases = project(
                projectName,
                "Mar 2026",
                LocalDate.of(2026, 3, 1),
                1,
                "01-08",
                178,
                "Previous Cases",
                0,
                0
        );

        /*
         * March fresh assignment = 100.
         */
        Project marchFresh = project(
                projectName,
                "Mar 2026",
                LocalDate.of(2026, 3, 1),
                2,
                "09-15",
                100,
                "17 Mar 2026",
                0,
                0
        );

        List<Project> history = List.of(
                october,
                november,
                december,
                januaryPreviousCases,
                januaryFresh,
                february,
                marchPreviousCases,
                marchFresh
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(history);

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Mar 2026",
                        2
                );

        /*
         * Genuine fresh assignments:
         *
         * Oct = 32
         * Nov = 204
         * Dec = 0
         * Jan = 144
         * Feb = 0
         * Mar = 100
         *
         * Total = 480
         */
        assertEquals(480, result.getAssignedPtd());

        /*
         * Current March Period 2 is a normal fresh assignment.
         */
        assertEquals(100, result.getAssignedWeekly());
        assertEquals(100, result.getFreshCases());
        assertEquals(0, result.getCarryForwardedCases());

    }

    @Test
    void shouldTreatPurePreviousCasesAsCarryForwardInsteadOfFreshAssigned() {

        String projectName = "Carry Forward Project";

        Project currentPeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                25,
                " Previous Cases ",
                null,
                null
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(currentPeriod));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        1
                );

        assertEquals(0, result.getFreshCases());
        assertEquals(0, result.getAssignedWeekly());
        assertEquals(25, result.getCarryForwardedCases());
        assertEquals(0, result.getAssignedPtd());
    }

    @Test
    void shouldCountNormalCurrentAssignmentAsFreshAssigned() {

        String projectName = "Normal Assignment Project";

        Project currentPeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                30,
                "03 Jan 2026",
                0,
                0
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(currentPeriod));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        1
                );

        assertEquals(30, result.getFreshCases());
        assertEquals(30, result.getAssignedWeekly());
        assertEquals(0, result.getCarryForwardedCases());
        assertEquals(30, result.getAssignedPtd());
    }

    @Test
    void shouldTreatPositiveAssignedWithBlankAssignedDateAsFreshWhenWeekIsPresent() {

        String projectName = "Blank Assigned Date Project";

        Project currentPeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                12,
                "",
                0,
                0
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(currentPeriod));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        1
                );

        assertEquals(12, result.getFreshCases());
        assertEquals(12, result.getAssignedWeekly());
        assertEquals(0, result.getCarryForwardedCases());
        assertEquals(12, result.getAssignedPtd());
    }

    @Test
    void shouldStopAssignedPtdAtSelectedReportingPeriod() {

        String projectName = "Period Cutoff Project";

        Project periodOne = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                10,
                "02 Jan 2026",
                0,
                0
        );

        Project periodTwo = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                2,
                "08-14",
                20,
                "09 Jan 2026",
                0,
                0
        );

        Project futurePeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                3,
                "15-21",
                300,
                "16 Jan 2026",
                0,
                0
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(periodOne, periodTwo, futurePeriod));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        2
                );

        assertEquals(30, result.getAssignedPtd());
    }

    @Test
    void shouldAccumulateSubmittedPtdThroughSelectedPeriodAndTreatNullAsZero() {

        String projectName = "Submitted Period Project";

        Project periodOne = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                0,
                null,
                4,
                0
        );

        Project periodTwo = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                2,
                "08-14",
                0,
                null,
                null,
                0
        );

        Project futurePeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                3,
                "15-21",
                0,
                null,
                99,
                0
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(periodOne, periodTwo, futurePeriod));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        2
                );

        assertEquals(0, result.getSubmittedWeekly());
        assertEquals(4, result.getSubmittedPtd());
    }

    @Test
    void shouldCalculateIssuePtdThroughPeriodFiveWithoutCountingPeriodZero() {

        String projectName = "Period Five Project";

        Project decemberPeriodFive = project(
                projectName,
                "Dec 2025",
                LocalDate.of(2025, 12, 1),
                5,
                "22-31",
                null,
                null,
                null,
                3
        );

        Project januaryPeriodZero = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                0,
                "",
                null,
                null,
                null,
                3
        );

        Project januaryPeriodOne = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                null,
                null,
                null,
                2
        );

        Project januaryPeriodFive = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                5,
                "22-31",
                null,
                null,
                null,
                5
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(
                        decemberPeriodFive,
                        januaryPeriodZero,
                        januaryPeriodOne,
                        januaryPeriodFive
                ));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        5
                );

        assertEquals(5, result.getIssueWeekly());
        assertEquals(10, result.getIssuePtd());
    }

    @Test
    void shouldTreatNullNumericValuesAsZero() {

        String projectName = "Null Numeric Project";

        Project currentPeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                1,
                "01-07",
                null,
                null,
                null,
                null
        );

        Project futurePeriod = project(
                projectName,
                "Jan 2026",
                LocalDate.of(2026, 1, 1),
                2,
                "08-14",
                400,
                "09 Jan 2026",
                500,
                600
        );

        when(historyService.getWeeklyHistory(projectName))
                .thenReturn(List.of(currentPeriod, futurePeriod));

        DashboardPtdResponseDto result =
                calculationService.calculateProjectPtdByPeriod(
                        projectName,
                        "Jan 2026",
                        1
                );

        assertEquals(0, result.getAssignedWeekly());
        assertEquals(0, result.getAssignedPtd());
        assertEquals(0, result.getSubmittedWeekly());
        assertEquals(0, result.getSubmittedPtd());
        assertEquals(0, result.getIssueWeekly());
        assertEquals(0, result.getIssuePtd());
    }

    private Project project(
            String projectName,
            String reportingMonth,
            LocalDate reportingMonthDate,
            Integer reportingPeriod,
            String weekOf,
            Integer assigned,
            String assignedDate,
            Integer submitted,
            Integer issue) {

        return Project.builder()
                .projectName(projectName)
                .reportingMonth(reportingMonth)
                .reportingMonthDate(reportingMonthDate)
                .reportingPeriod(reportingPeriod)
                .weekOf(weekOf)
                .assignedToPareidolia(assigned)
                .assignedDate(assignedDate)
                .annotationsSubmittedPareidolia(submitted)
                .issueCases(issue)

                // Keep unrelated calculations neutral
                .casesMovedToLabels(0)
                .annotationsCompleted(0)

                .build();
    }


}
