package com.pareidolia.projecttracker.service.dashboard;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.entity.Project;

public interface DashboardKpiService {
    void savePtdKpi(Project project, DashboardPtdResponseDto response);
}
