package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.repository.DashboardKpiRepository;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DashboardKpiPopulationServiceImplTest {

    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private DashboardCalculationService dashboardCalculationService;

    @Mock
    private DashboardKpiRepository dashboardKpiRepository;

    @Test
    void shouldRouteNonblankWeekOfToWeekBasedPtdCalculation() {

        Project project = project(
                "Week Route Project",
                1,
                "01-07"
        );

        when(projectRepository.findAll()).thenReturn(List.of(project));

        DashboardKpiPopulationServiceImpl service =
                new DashboardKpiPopulationServiceImpl(
                        projectRepository,
                        dashboardCalculationService,
                        dashboardKpiRepository
                );

        int processed = service.populateExistingKpis();

        assertEquals(1, processed);
        verify(dashboardCalculationService).calculateProjectPtd(
                "Week Route Project",
                "Jan 2026",
                "01-07"
        );
        verify(dashboardCalculationService, never())
                .calculateProjectPtdByPeriod(
                        "Week Route Project",
                        "Jan 2026",
                        1
                );
    }

    @Test
    void shouldRouteBlankAndWhitespaceWeekOfToPeriodBasedPtdCalculation() {

        Project blankWeek = project(
                "Blank Week Route Project",
                1,
                ""
        );
        Project whitespaceWeek = project(
                "Whitespace Week Route Project",
                2,
                "  "
        );

        when(projectRepository.findAll())
                .thenReturn(List.of(blankWeek, whitespaceWeek));

        DashboardKpiPopulationServiceImpl service =
                new DashboardKpiPopulationServiceImpl(
                        projectRepository,
                        dashboardCalculationService,
                        dashboardKpiRepository
                );

        int processed = service.populateExistingKpis();

        assertEquals(2, processed);
        verify(dashboardCalculationService).calculateProjectPtdByPeriod(
                "Blank Week Route Project",
                "Jan 2026",
                1
        );
        verify(dashboardCalculationService).calculateProjectPtdByPeriod(
                "Whitespace Week Route Project",
                "Jan 2026",
                2
        );
        verify(dashboardCalculationService, never())
                .calculateProjectPtd(anyString(), anyString(), anyString());
    }

    private Project project(
            String projectName,
            Integer reportingPeriod,
            String weekOf) {

        return Project.builder()
                .projectName(projectName)
                .reportingMonth("Jan 2026")
                .reportingMonthDate(LocalDate.of(2026, 1, 1))
                .reportingPeriod(reportingPeriod)
                .weekOf(weekOf)
                .build();
    }
}
