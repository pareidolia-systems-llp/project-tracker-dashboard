package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.entity.DashboardKpi;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.repository.DashboardKpiRepository;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;


import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class DashboardKpiServiceImpl implements DashboardKpiService {

    private final DashboardKpiRepository dashboardKpiRepository;


    @Override
    @Transactional
    public void savePtdKpi(Project project, DashboardPtdResponseDto response) {

        DashboardKpi kpi = dashboardKpiRepository
                .findByProjectNameAndReportingMonthAndReportingPeriod(
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getReportingPeriod()
                )
                .orElseGet(DashboardKpi::new);

        kpi.setProjectName(project.getProjectName());
        kpi.setReportingMonth(project.getReportingMonth());
        kpi.setReportingPeriod(project.getReportingPeriod());
        kpi.setWeekOf(project.getWeekOf());

        kpi.setAssignedWeekly(response.getAssignedWeekly());
        kpi.setAssignedPtd(response.getAssignedPtd());

        kpi.setSubmittedWeekly(response.getSubmittedWeekly());
        kpi.setSubmittedPtd(response.getSubmittedPtd());

        kpi.setIssueWeekly(response.getIssueWeekly());
        kpi.setIssuePtd(response.getIssuePtd());

        kpi.setAcceptedPtd(response.getAcceptedPtd());

        kpi.setRejectedWeekly(response.getRejectedWeekly());
        kpi.setRejectedPtd(response.getRejectedPtd());



        kpi.setAcceptedPtdPercentage(response.getAcceptedPtdPercentage());
        kpi.setRejectedPtdPercentage(response.getRejectedPtdPercentage());

        if (kpi.getCreatedAt() == null) {
            kpi.setCreatedAt(LocalDateTime.now());
        }

        kpi.setUpdatedAt(LocalDateTime.now());

        dashboardKpiRepository.save(kpi);
    }
}