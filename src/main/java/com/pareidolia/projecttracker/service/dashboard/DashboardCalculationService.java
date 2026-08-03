package com.pareidolia.projecttracker.service.dashboard;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;

public interface DashboardCalculationService {



    DashboardPtdResponseDto calculateProjectPtd(
            String projectName,
            String reportingMonth,
            String weekOf
    );

}