package com.pareidolia.projecttracker.controller;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import com.pareidolia.projecttracker.security.RefreshApiKeyValidator;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardCalculationService dashboardCalculationService;
    private final DashboardKpiPopulationService dashboardKpiPopulationService;
    private final RefreshApiKeyValidator refreshApiKeyValidator;
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

    @GetMapping("/ptd/period")
    public DashboardPtdResponseDto getProjectPtdByPeriod(

            @RequestParam String projectName,

            @RequestParam String reportingMonth,

            @RequestParam Integer reportingPeriod) {

        return dashboardCalculationService.calculateProjectPtdByPeriod(
                projectName,
                reportingMonth,
                reportingPeriod
        );
    }

//    @PostMapping("/kpi/populate")
//    public String populateDashboardKpis() {
//
//        int processed =
//                dashboardKpiPopulationService.populateExistingKpis();
//
//        return "Dashboard KPI population completed. Processed: "
//                + processed;
//    }

    @PostMapping("/kpi/rebuild")
    public String rebuildDashboardKpis(
            @RequestHeader("X-Refresh-Key") String refreshKey) {

        refreshApiKeyValidator.validate(refreshKey);

        int processed =
                dashboardKpiPopulationService.rebuildAllKpis();

        return "Dashboard KPI rebuild completed. Processed: "
                + processed;
    }
}