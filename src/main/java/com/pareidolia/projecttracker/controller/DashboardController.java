package com.pareidolia.projecttracker.controller;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardCalculationService dashboardCalculationService;

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
}