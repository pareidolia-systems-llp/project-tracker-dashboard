package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import com.pareidolia.projecttracker.service.dashboard.HistoryService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiService;
import com.pareidolia.projecttracker.service.dashboard.MonthlyProjectSummaryService;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCalculationServiceImpl
        implements DashboardCalculationService {

    private final HistoryService historyService;
    private final DashboardKpiService dashboardKpiService;

    private final MonthlyProjectSummaryService monthlyProjectSummaryService;

    @Override
    public DashboardPtdResponseDto calculateProjectPtd(
            String projectName,
            String reportingMonth,
            String weekOf) {

        // Get the complete weekly history of the project
        List<Project> weeklyHistory =
                historyService.getWeeklyHistory(projectName);

        // Find the selected week
        Project currentProject = getCurrentWeek(
                weeklyHistory,
                reportingMonth,
                weekOf
        );

        int freshCases =
                calculateFreshCases(
                        weeklyHistory,
                        currentProject
                );

        int carryForwardedCases =
                value(currentProject.getAssignedToPareidolia())
                        - freshCases;


        int acceptedPtd =
                calculateAcceptedPtd(weeklyHistory, currentProject);

        int rejectedWeekly =
                value(currentProject.getCasesMovedToLabels());

        int rejectedPtd =
                calculateRejectedPtd(
                        weeklyHistory,
                        currentProject);

        double acceptedPtdPercentage =
                calculateAcceptedPtdPercentage(
                        acceptedPtd,
                        rejectedPtd);

        double rejectedPtdPercentage =
                calculateRejectedPtdPercentage(
                        acceptedPtd,
                        rejectedPtd);

        // Calculate PTD
        DashboardPtdResponseDto response =
                DashboardPtdResponseDto.builder()

                        .projectName(currentProject.getProjectName())

                        .assignedWeekly(freshCases)

                        .freshCases(freshCases)

                        .carryForwardedCases(carryForwardedCases)

                        .assignedPtd(
                                calculateAssignedPtd(
                                        weeklyHistory,
                                        currentProject))

                        .submittedWeekly(
                                value(
                                        currentProject
                                                .getAnnotationsSubmittedPareidolia()))

                        .submittedPtd(
                                calculateSubmittedPtd(
                                        weeklyHistory,
                                        currentProject))

                        .issueWeekly(
                                value(currentProject.getIssueCases()))

                        .issuePtd(
                                calculateIssuePtd(
                                        weeklyHistory,
                                        currentProject))

                        .acceptedPtd(acceptedPtd)

                        .rejectedWeekly(rejectedWeekly)

                        .rejectedPtd(rejectedPtd)

                        .acceptedPtdPercentage(acceptedPtdPercentage)

                        .rejectedPtdPercentage(rejectedPtdPercentage)

                        .build();


        dashboardKpiService.savePtdKpi(currentProject, response);

        return response;

    }

    /**
     * Finds the selected week from the project's history.
     */
    private Project getCurrentWeek(
            List<Project> weeklyHistory,
            String reportingMonth,
            String weekOf) {

        return weeklyHistory.stream()
                .filter(project ->
                        reportingMonth.equals(
                                project.getReportingMonth())
                                &&
                                weekOf.equals(
                                        project.getWeekOf()))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project week not found: "
                                        + reportingMonth
                                        + " / "
                                        + weekOf));
    }

    /**
     * Calculates cumulative annotations submitted
     * from project start through the selected week.
     */
    private int calculateSubmittedPtd(
            List<Project> history,
            Project currentProject) {

        int total = 0;

        for (Project project : history) {

            total += value(
                    project.getAnnotationsSubmittedPareidolia()
            );

            if (sameWeek(project, currentProject)) {
                break;
            }
        }

        return total;
    }

    private int calculateSubmittedPtdByPeriod(
            List<Project> history,
            Project currentProject) {

        int total = 0;

        for (Project project : history) {

            total += value(
                    project.getAnnotationsSubmittedPareidolia()
            );

            if (samePeriod(project, currentProject)) {
                break;
            }
        }

        return total;
    }

    private int calculateIssuePtdByPeriod(
            List<Project> history,
            Project currentProject) {

        int total = 0;

        for (Project project : history) {

            int issue = value(project.getIssueCases());

            Integer reportingPeriod =
                    project.getReportingPeriod();

            /*
             * Reporting Period 0 represents the cumulative /
             * carry-forward Issue value entering the month.
             *
             * Do NOT add it again because the actual weekly
             * issue rows from previous history have already
             * contributed to Issue PTD.
             */
            if (Integer.valueOf(0).equals(reportingPeriod)) {

                /*
                 * If Period 0 itself is the selected row,
                 * stop here so we don't include future periods
                 * from the same month.
                 */
                if (samePeriod(project, currentProject)) {
                    break;
                }

                continue;
            }

            /*
             * Period 1-4 = genuine reporting-period Issue.
             *
             * Week Of may legitimately be blank because the
             * week range may not have been entered.
             */
            total += issue;

            if (samePeriod(project, currentProject)) {
                break;
            }
        }

        return total;
    }

    /**
     * Calculates cumulative issue cases
     * from project start through the selected week.
     */
    private int calculateIssuePtd(
            List<Project> history,
            Project currentProject) {

        int total = 0;

        System.out.println();
        System.out.println(
                "========== ISSUE PTD CALCULATION =========="
        );

        System.out.println(
                "Selected Month = "
                        + currentProject.getReportingMonth()
                        + " | Selected Week = "
                        + currentProject.getWeekOf()
        );

        for (Project project : history) {

            int issue =
                    value(project.getIssueCases());

            Integer reportingPeriod =
                    project.getReportingPeriod();

            String weekOf =
                    project.getWeekOf();

            System.out.println(
                    "CHECKING ISSUE -> "
                            + "Month=["
                            + project.getReportingMonth()
                            + "]"
                            + " | Period=["
                            + reportingPeriod
                            + "]"
                            + " | Week=["
                            + weekOf
                            + "]"
                            + " | Issue=["
                            + issue
                            + "]"
            );

            /*
             * Period 0 is cumulative/carry-forward.
             * It must not be added again.
             */
            if (Integer.valueOf(0).equals(reportingPeriod)) {

                System.out.println(
                        "SKIPPED ISSUE -> Period 0 carry-forward"
                                + " | Issue="
                                + issue
                );

                continue;
            }

            /*
             * Period 1-4 contains genuine Issue Cases.
             * Add them even when Week Of is blank.
             */
            if (issue > 0) {

                total += issue;

                System.out.println(
                        "ADDING ISSUE -> "
                                + "Month=["
                                + project.getReportingMonth()
                                + "]"
                                + " | Period=["
                                + reportingPeriod
                                + "]"
                                + " | Week=["
                                + weekOf
                                + "]"
                                + " | Issue="
                                + issue
                                + " | Running PTD="
                                + total
                );
            }

            /*
             * calculateIssuePtd() is used by the Week-based
             * endpoint. A blank Week Of row cannot itself be
             * the selected week, so only compare actual Week
             * Of values here.
             */
            if (weekOf != null
                    && !weekOf.trim().isEmpty()
                    && sameWeek(project, currentProject)) {

                System.out.println(
                        "SELECTED WEEK REACHED -> stopping calculation"
                );

                break;
            }
        }

        System.out.println(
                "========== FINAL ISSUE PTD = "
                        + total
                        + " =========="
        );

        return total;
    }
    /**
     * Calculates Assigned PTD.
     *
     * BUSINESS RULE
     * ------------
     *
     * Assigned PTD counts ONLY genuinely fresh assignments.
     *
     * A value in "Assigned to Pareidolia" is NOT necessarily
     * a fresh assignment because the sheet can contain:
     *
     *     Previous Cases
     *
     * or
     *
     *     Previous Cases + fresh assignment
     *
     * Example:
     *
     * October:
     *     Assigned = 32
     *     Fresh = 32
     *
     * October carry-forward:
     *     32 - 5 submitted = 27
     *
     * November:
     *     Assigned = 231
     *
     *     27 = carry-forward from October
     *     204 = genuinely fresh assignment
     *
     * Therefore:
     *
     *     November fresh = 231 - 27 = 204
     *
     * December:
     *     Assigned = 224
     *     Previous Cases = 224
     *     Fresh = 0
     *
     * January:
     *     Previous Cases = 143
     *     Fresh Assignment = 144
     *     Fresh = 144
     *
     * February:
     *     Previous Cases = 203
     *     Fresh = 0
     *
     * March:
     *     Previous Cases = 178
     *     Fresh Assignment = 100
     *     Fresh = 100
     *
     * Final PTD through March:
     *
     *     32 + 204 + 144 + 100
     *     = 480
     *
     * IMPORTANT:

     *
     * We calculate carry-forward using:
     *
     *     Assigned - Submitted - Issue

     */
    private int calculateAssignedPtd(
            List<Project> history,
            Project currentProject) {

        int assignedPtd = 0;

        System.out.println(
                "========== ASSIGNED PTD CALCULATION =========="
        );

        System.out.println(
                "Selected Month = "
                        + currentProject.getReportingMonth()
                        + " | Selected Week = "
                        + currentProject.getWeekOf()
        );

        /*
         * Process the history in chronological order.
         */
        for (int i = 0; i < history.size(); i++) {

            Project project = history.get(i);

            int assigned =
                    value(project.getAssignedToPareidolia());

            String assignedDate =
                    project.getAssignedDate();

            boolean previousCases =
                    isPreviousCases(assignedDate);

            System.out.println(
                    "CHECKING -> "
                            + "Month="
                            + project.getReportingMonth()
                            + " | Week=["
                            + project.getWeekOf()
                            + "]"
                            + " | Assigned="
                            + assigned
                            + " | AssignedDate=["
                            + assignedDate
                            + "]"
                            + " | PreviousCases="
                            + previousCases
            );

            /*
             * No assignment on this row.
             */
            if (assigned <= 0) {

                System.out.println(
                        "SKIPPED -> Assigned is 0 or blank"
                );

                if (sameWeek(project, currentProject)) {

                    System.out.println(
                            "SELECTED WEEK REACHED "
                                    + "-> stopping calculation"
                    );

                    break;
                }

                continue;
            }



            /*
             * ---------------------------------------------------------
             * PURE PREVIOUS CASES
             * ---------------------------------------------------------
             *
             * If this is a Previous Cases row, we normally do not
             * count it as a fresh assignment.
             *
             * Example:
             *
             * December:
             *     Assigned = 224
             *     Assigned Date = Previous Cases
             *
             * January:
             *     Assigned = 143
             *     Assigned Date = Previous Cases
             *
             * February:
             *     Assigned = 203
             *     Assigned Date = Previous Cases
             *
             * March:
             *     Assigned = 178
             *     Assigned Date = Previous Cases
             */
            if (isPurePreviousCases(assignedDate)) {

                /*
                 * Pure carry-forward.
                 * Do not add anything to Assigned PTD.
                 */
                System.out.println(
                        "SKIPPED -> Pure Previous Cases"
                                + " | Assigned="
                                + assigned
                );

            } else if (previousCases) {

                /*
                 * Mixed Previous Cases + fresh assignment.
                 * Keep existing logic.
                 */
                int previousMonthCarryForward =
                        findPreviousMonthCarryForward(
                                history,
                                i
                        );

                int freshAssignment =
                        assigned - previousMonthCarryForward;

                freshAssignment =
                        Math.max(freshAssignment, 0);

                if (freshAssignment > 0) {

                    assignedPtd += freshAssignment;

                    System.out.println(
                            "ADDING -> Fresh Assignment inside "
                                    + "mixed Previous Cases row"
                                    + " | Assigned="
                                    + assigned
                                    + " | Previous Carry="
                                    + previousMonthCarryForward
                                    + " | Fresh="
                                    + freshAssignment
                                    + " | Running PTD="
                                    + assignedPtd
                    );
                }

            } else {

                /*
                 * -----------------------------------------------------
                 * NORMAL FRESH ASSIGNMENT
                 * -----------------------------------------------------
                 *
                 * Examples:
                 *
                 * October:
                 *     32
                 *
                 * January:
                 *     144
                 *
                 * March:
                 *     100
                 */
                assignedPtd += assigned;

                System.out.println(
                        "ADDING -> Fresh Assignment"
                                + " | Assigned="
                                + assigned
                                + " | Running PTD="
                                + assignedPtd
                );
            }

            /*
             * Stop once the selected week has been processed.
             */
            if (sameWeek(project, currentProject)) {

                System.out.println(
                        "SELECTED WEEK REACHED "
                                + "-> stopping calculation"
                );

                break;
            }
        }

        System.out.println(
                "========== FINAL ASSIGNED PTD = "
                        + assignedPtd
                        + " =========="
        );

        return assignedPtd;
    }

    private int calculateAssignedPtdByPeriod(
            List<Project> history,
            Project currentProject) {

        int assignedPtd = 0;

        for (int i = 0; i < history.size(); i++) {

            Project project = history.get(i);

            int assigned =
                    value(project.getAssignedToPareidolia());

            String assignedDate =
                    project.getAssignedDate();

            boolean previousCases =
                    isPreviousCases(assignedDate);

            /*
             * No assignment on this row.
             */
            if (assigned <= 0) {

                if (samePeriod(project, currentProject)) {
                    break;
                }

                continue;
            }

            /*
             * Previous Cases row
             */
            if (isPurePreviousCases(assignedDate)) {

                // Pure carry-forward: add nothing to PTD.

            } else if (previousCases) {

                int previousMonthCarryForward =
                        findPreviousMonthCarryForward(
                                history,
                                i
                        );

                int freshAssignment =
                        assigned - previousMonthCarryForward;

                freshAssignment =
                        Math.max(freshAssignment, 0);

                assignedPtd += freshAssignment;

            } else {

                /*
                 * Normal fresh assignment
                 */
                assignedPtd += assigned;
            }

            /*
             * Stop at the selected reporting period.
             */
            if (samePeriod(project, currentProject)) {
                break;
            }
        }

        return assignedPtd;
    }

    /**
     * Finds the actual carry-forward entering the month of the
     * supplied project.
     *
     * IMPORTANT:
     *
     * We deliberately do NOT use Issue Cases here.
     *
     * The carry-forward is determined from the previous month's
     * workload:
     *
     *     Previous month's assigned
     *     - Previous month's submitted
     *
     * For the current project's month, we use the actual
     * Previous Cases value where available.
     *
     * This method is specifically designed for the PTD calculation
     * and should not affect Issue PTD.
     */
    private int findPreviousMonthCarryForward(
            List<Project> history,
            int currentIndex) {

        if (currentIndex <= 0) {
            return 0;
        }

        Project currentProject =
                history.get(currentIndex);

        String currentMonth =
                currentProject.getReportingMonth();

        /*
         * Search backwards for the previous month's
         * Previous Cases row.
         *
         * We use the actual Previous Cases value from the sheet
         * rather than reconstructing it using Issue Cases.
         */
        for (int i = currentIndex - 1; i >= 0; i--) {

            Project previousProject =
                    history.get(i);

            String previousMonth =
                    previousProject.getReportingMonth();

            /*
             * Ignore rows belonging to the current month.
             */
            if (currentMonth.equals(previousMonth)) {
                continue;
            }

            /*
             * We have reached the previous month.
             *
             * Find its last meaningful assigned value.
             */
            for (int j = i; j >= 0; j--) {

                Project candidate =
                        history.get(j);

                if (!previousMonth.equals(
                        candidate.getReportingMonth())) {

                    break;
                }

                int assigned =
                        value(candidate.getAssignedToPareidolia());

                if (assigned <= 0) {
                    continue;
                }

                /*
                 * If the previous month's first assignment row is
                 * a Previous Cases row, use its assigned value.
                 *
                 * Otherwise calculate the remaining workload from
                 * the previous month's assigned/submitted values.
                 */
                if (isPreviousCases(
                        candidate.getAssignedDate())) {

                    return assigned;
                }

                /*
                 * Normal assignment month:
                 *
                 * Carry forward =
                 *     Assigned - Submitted
                 *
                 * We intentionally do NOT subtract Issue Cases.
                 */
                int monthlyAssigned =
                        calculateMonthlyAssigned(
                                history,
                                previousMonth
                        );

                int monthlySubmitted =
                        calculateMonthlySubmitted(
                                history,
                                previousMonth
                        );

                int monthlyNewIssue =
                        calculateMonthlyNewIssue(
                                history,
                                previousMonth
                        );

                return Math.max(
                        monthlyAssigned
                                - monthlySubmitted
                                - monthlyNewIssue,
                        0
                );
            }

            /*
             * Previous month found but no assigned value.
             */
            return 0;
        }

        return 0;
    }

    /**
     * Calculates total Assigned value for a month.
     *
     * This is used only when determining carry-forward.
     */
    private int calculateMonthlyAssigned(
            List<Project> history,
            String reportingMonth) {

        int total = 0;

        for (Project project : history) {

            if (!reportingMonth.equals(
                    project.getReportingMonth())) {

                continue;
            }

            total += value(
                    project.getAssignedToPareidolia()
            );
        }

        return total;
    }

    /**
     * Calculates total Submitted value for a month.
     */
    private int calculateMonthlySubmitted(
            List<Project> history,
            String reportingMonth) {

        int total = 0;

        for (Project project : history) {

            if (!reportingMonth.equals(
                    project.getReportingMonth())) {

                continue;
            }

            total += value(
                    project.getAnnotationsSubmittedPareidolia()
            );
        }

        return total;
    }

    private int calculateMonthlyNewIssue(
            List<Project> history,
            String reportingMonth) {

        int total = 0;

        for (Project project : history) {

            if (!reportingMonth.equals(
                    project.getReportingMonth())) {

                continue;
            }

            /*
             * Period 0 contains cumulative / carry-forward Issue Cases.
             * Do not subtract it again.
             *
             * Only Period > 0 represents genuine new Issue Cases
             * for that month.
             */
            Integer reportingPeriod =
                    project.getReportingPeriod();

            if (reportingPeriod == null
                    || reportingPeriod <= 0) {

                continue;
            }

            total += value(
                    project.getIssueCases()
            );
        }

        return total;
    }

    private boolean sameWeek(
            Project project,
            Project currentProject) {

        return project.getReportingMonth().equals(
                currentProject.getReportingMonth())
                &&
                project.getWeekOf().equals(
                        currentProject.getWeekOf());
    }

    private int value(Integer number) {

        return number == null ? 0 : number;
    }

    private boolean samePeriod(
            Project project,
            Project currentProject) {

        return project.getReportingMonth().equals(
                currentProject.getReportingMonth())
                &&
                project.getReportingPeriod().equals(
                        currentProject.getReportingPeriod());
    }

    private boolean isPreviousCases(
            String assignedDate) {

        if (assignedDate == null) {
            return false;
        }

        return assignedDate
                .trim()
                .toLowerCase()
                .startsWith("previous cases");
    }


    @Override
    @Transactional
    public DashboardPtdResponseDto calculateProjectPtdByPeriod(
            String projectName,
            String reportingMonth,
            Integer reportingPeriod) {

        List<Project> history =
                historyService.getWeeklyHistory(projectName);

        Project currentProject = getCurrentPeriod(
                history,
                reportingMonth,
                reportingPeriod
        );

        int freshCases =
                calculateFreshCases(
                        history,
                        currentProject
                );

        int carryForwardedCases =
                value(currentProject.getAssignedToPareidolia())
                        - freshCases;

        int acceptedPtd =
                calculateAcceptedPtd(history, currentProject);
        int rejectedWeekly =
                value(currentProject.getCasesMovedToLabels());

        int rejectedPtd =
                calculateRejectedPtdByPeriod(
                        history,
                        currentProject);

        double acceptedPtdPercentage =
                calculateAcceptedPtdPercentage(
                        acceptedPtd,
                        rejectedPtd);

        double rejectedPtdPercentage =
                calculateRejectedPtdPercentage(
                        acceptedPtd,
                        rejectedPtd);

        DashboardPtdResponseDto response =
                DashboardPtdResponseDto.builder()

                        .projectName(currentProject.getProjectName())

                        /*
                         * For a blank Week Of row, these values are still
                         * legitimate source values from the Projects table.
                         */
                        .assignedWeekly(freshCases)

                        .freshCases(freshCases)

                        .carryForwardedCases(carryForwardedCases)

                        .assignedPtd(
                                calculateAssignedPtdByPeriod(
                                        history,
                                        currentProject))

                        .submittedWeekly(
                                value(
                                        currentProject
                                                .getAnnotationsSubmittedPareidolia()))

                        .submittedPtd(
                                calculateSubmittedPtdByPeriod(
                                        history,
                                        currentProject))

                        .issueWeekly(
                                value(currentProject.getIssueCases()))

                        .issuePtd(
                                calculateIssuePtdByPeriod(
                                        history,
                                        currentProject))

                        .acceptedPtd(acceptedPtd)

                        .rejectedWeekly(rejectedWeekly)

                        .rejectedPtd(rejectedPtd)

                        .acceptedPtdPercentage(acceptedPtdPercentage)

                        .rejectedPtdPercentage(rejectedPtdPercentage)

                        .build();

        /*
         * This is important:
         * save the result against the actual Project row,
         * even when Week Of is blank.
         */
        dashboardKpiService.savePtdKpi(
                currentProject,
                response
        );

        return response;
    }

    private Project getCurrentPeriod(
            List<Project> history,
            String reportingMonth,
            Integer reportingPeriod) {

        return history.stream()
                .filter(project ->
                        reportingMonth.equals(project.getReportingMonth())
                                &&
                                reportingPeriod.equals(project.getReportingPeriod())
                )
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException(
                                "Project period not found: "
                                        + reportingMonth
                                        + " / Period "
                                        + reportingPeriod
                        )
                );
    }

    private int calculateAcceptedPtd(
            List<Project> history,
            Project currentProject) {

        String reportingMonth = currentProject.getReportingMonth();

        for (Project project : history) {

            if (!reportingMonth.equals(project.getReportingMonth())) {
                continue;
            }

            int acceptedPtd =
                    value(project.getAnnotationsCompleted());


            if (acceptedPtd > 0) {
                return acceptedPtd;
            }
        }

        return 0;
    }
    private int calculateRejectedPtd(
            List<Project> history,
            Project currentProject) {

        int total = 0;

        for (Project project : history) {

            total += value(
                    project.getCasesMovedToLabels());

            if (sameWeek(project, currentProject)) {
                break;
            }
        }

        return total;
    }

    //W → casesMovedToLabels → Rejected PTD
    //Y → annotationsCompleted → Accepted PTD


    private double calculateAcceptedPtdPercentage(
            int acceptedPtd,
            int rejectedPtd) {

        int total = acceptedPtd + rejectedPtd;

        if (total == 0) {
            return 0.0;
        }

        return (acceptedPtd * 100.0) / total;
    }

    private double calculateRejectedPtdPercentage(
            int acceptedPtd,
            int rejectedPtd) {

        int total = acceptedPtd + rejectedPtd;

        if (total == 0) {
            return 0.0;
        }

        return (rejectedPtd * 100.0) / total;
    }

    private int calculateRejectedPtdByPeriod(
            List<Project> history,
            Project currentProject) {

        int total = 0;

        for (Project project : history) {

            total += value(
                    project.getCasesMovedToLabels());

            if (samePeriod(project, currentProject)) {
                break;
            }
        }

        return total;
    }

    private int calculateFreshCases(
            List<Project> history,
            Project currentProject) {

        int assigned =
                value(currentProject.getAssignedToPareidolia());

        if (assigned <= 0) {
            return 0;
        }

        String assignedDate =
                currentProject.getAssignedDate();

        /*
         * Pure Previous Cases:
         * whole Assigned value is carry-forward.
         */
        if (isPurePreviousCases(assignedDate)) {
            return 0;
        }

        /*
         * Normal assignment:
         * entire value is fresh.
         */
        if (!isPreviousCases(assignedDate)) {
            return assigned;
        }

        /*
         * Mixed Previous Cases entry:
         * e.g. "Previous Cases + <date>"
         *
         * Preserve the existing historical split logic.
         */
        int currentIndex =
                history.indexOf(currentProject);

        int previousMonthCarryForward =
                findPreviousMonthCarryForward(
                        history,
                        currentIndex
                );

        return Math.max(
                assigned - previousMonthCarryForward,
                0
        );
    }

    private boolean isPurePreviousCases(String assignedDate) {

        if (assignedDate == null) {
            return false;
        }

        return assignedDate
                .trim()
                .equalsIgnoreCase("previous cases");
    }

    /*
     * Carry-forward is reconstructed from the previous month's
     * workload:
     *
     *     Assigned
     *     - Submitted
     *     - New Issue Cases
     *
     * Period 0 Issue Cases are cumulative/carry-forward values
     * and must not be subtracted again.
     */
}