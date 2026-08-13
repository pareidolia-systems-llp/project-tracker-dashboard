package com.pareidolia.projecttracker.controller;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardCalculationService dashboardCalculationService;
    private final DashboardKpiPopulationService dashboardKpiPopulationService;
    @GetMapping("/ptd")
    public DashboardPtdResponseDto getProjectPtd(

            @RequestParam String projectName,

            @RequestParam String reportingMonth,

            @RequestParam String weekOf) {

        return dashboardCalculationService.calculateProjectPtd(
                projectName,
                reportingMonth,
                weekOf
        );
    }

    @PostMapping("/kpi/populate")
    public String populateDashboardKpis() {

        int processed =
                dashboardKpiPopulationService.populateExistingKpis();

        return "Dashboard KPI population completed. Processed: "
                + processed;
    }
}