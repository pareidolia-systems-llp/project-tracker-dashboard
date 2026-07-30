package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.dashboard.DashboardPtdResponseDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardCalculationServiceImpl
        implements DashboardCalculationService {

    private final ProjectRepository projectRepository;

    @Override
    public DashboardPtdResponseDto calculateProjectPtd(
            String projectName,
            String reportingMonth,
            String weekOf) {

        List<Project> weeklyHistory =
                projectRepository.findByProjectName(projectName)
                        .stream()
                        .filter(project ->
                                project.getWeekOf() != null &&
                                        !project.getWeekOf().isBlank())
                        .toList();

        return null;
    }

}