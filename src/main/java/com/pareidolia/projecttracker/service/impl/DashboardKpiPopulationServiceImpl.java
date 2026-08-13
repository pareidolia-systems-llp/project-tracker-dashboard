package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardKpiPopulationServiceImpl
        implements DashboardKpiPopulationService {

    private final ProjectRepository projectRepository;
    private final DashboardCalculationService dashboardCalculationService;

    @Override
    public int populateExistingKpis() {

        List<Project> projects = projectRepository.findAll();

        int processed = 0;
        int skipped = 0;

        for (Project project : projects) {

            // Do not attempt the existing PTD calculation
            // when Week Of is blank.
            // Blank Week Of rows are legitimate Period 1-4 data
            // and will be handled separately.
            if (project.getWeekOf() == null
                    || project.getWeekOf().trim().isEmpty()) {

                skipped++;

                log.info(
                        "Skipping blank Week Of -> Project='{}' | Month='{}' | Period={}",
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getReportingPeriod()
                );

                continue;
            }

            try {

                dashboardCalculationService.calculateProjectPtd(
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getWeekOf()
                );

                processed++;

            } catch (Exception ex) {

                log.error(
                        "Failed KPI calculation -> Project='{}' | Month='{}' | Period={} | Week='{}'",
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getReportingPeriod(),
                        project.getWeekOf(),
                        ex
                );
            }
        }

        log.info(
                "Dashboard KPI population completed. Processed={}, SkippedBlankWeek={}",
                processed,
                skipped
        );

        return processed;
    }
}