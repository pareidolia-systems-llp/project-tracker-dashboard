package com.pareidolia.projecttracker.service.dashboard;

import com.pareidolia.projecttracker.dto.dashboard.MonthlyProjectSummary;
import com.pareidolia.projecttracker.entity.Project;

import java.util.List;

public interface MonthlyProjectSummaryService {

    List<MonthlyProjectSummary> buildMonthlySummary(
            List<Project> weeklyHistory
    );

}