package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import com.pareidolia.projecttracker.service.dashboard.HistoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCalculationServiceImpl
        implements DashboardCalculationService {

    private final HistoryService historyService;

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

        // Calculate PTD
        return calculatePtd(
                weeklyHistory,
                currentProject
        );
    }

    // Finds the selected week from the project's history
    private Project getCurrentWeek(
            List<Project> weeklyHistory,
            String reportingMonth,
            String weekOf) {

        return weeklyHistory.stream()
                .filter(project ->
                        project.getReportingMonth().equals(reportingMonth)
                                &&
                                project.getWeekOf().equals(weekOf))
                .findFirst()
                .orElseThrow(() ->
                        new RuntimeException("Project week not found"));
    }

    // Calculates cumulative PTD values
    private DashboardPtdResponseDto calculatePtd(
            List<Project> weeklyHistory,
            Project currentProject) {

        int assignedPtd = 0;
        int submittedPtd = 0;
        int issuePtd = 0;

        for (Project project : weeklyHistory) {

            assignedPtd += project.getAssignedToPareidolia();
            submittedPtd += project.getAnnotationsSubmittedPareidolia();
            issuePtd += project.getIssueCases();

            if (project.getReportingMonth().equals(currentProject.getReportingMonth())
                    && project.getWeekOf().equals(currentProject.getWeekOf())) {
                break;
            }
        }

        return DashboardPtdResponseDto.builder()
                .projectName(currentProject.getProjectName())
                .assignedWeekly(currentProject.getAssignedToPareidolia())
                .assignedPtd(assignedPtd)
                .submittedWeekly(currentProject.getAnnotationsSubmittedPareidolia())
                .submittedPtd(submittedPtd)
                .issueWeekly(currentProject.getIssueCases())
                .issuePtd(issuePtd)
                .build();
    }
}