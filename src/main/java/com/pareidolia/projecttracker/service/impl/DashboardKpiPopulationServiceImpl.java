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
        int failed = 0;

        for (Project project : projects) {

            try {

                /*
                 * Normal case:
                 * Week Of is available, so use the existing
                 * senior-verified week-based PTD calculation.
                 */
                if (project.getWeekOf() != null
                        && !project.getWeekOf().trim().isEmpty()) {

                    dashboardCalculationService.calculateProjectPtd(
                            project.getProjectName(),
                            project.getReportingMonth(),
                            project.getWeekOf()
                    );

                }

                /*
                 * Special case:
                 * Senior forgot to enter Week Of.
                 *
                 * The row is still legitimate because it belongs
                 * to a reporting period and contains actual values.
                 *
                 * Therefore calculate PTD using:
                 * Project + Reporting Month + Reporting Period
                 */
                else {

                    dashboardCalculationService.calculateProjectPtdByPeriod(
                            project.getProjectName(),
                            project.getReportingMonth(),
                            project.getReportingPeriod()
                    );
                }

                processed++;

            } catch (Exception ex) {

                failed++;

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
                "Dashboard KPI population completed. Processed={}, Failed={}",
                processed,
                failed
        );

        return processed;
    }
}