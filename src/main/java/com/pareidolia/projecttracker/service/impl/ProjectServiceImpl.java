package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.mapper.ProjectMapper;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import com.pareidolia.projecttracker.service.ProjectService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {

    private final GoogleSheetsService googleSheetsService;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    @Override
    @Transactional
    public void synchronizeProjects()
            throws GeneralSecurityException, IOException {

        List<GoogleSheetRowDto> rows = googleSheetsService.readSheet();

        log.info("Rows read from Google Sheet : {}", rows.size());
        // ADDING THIS TEMPORARY LOOP
        for (GoogleSheetRowDto dto : rows) {

            log.info(
                    "Project='{}' | Week='{}'",
                    dto.getProjectName(),
                    dto.getWeekOf()
            );
        }

        log.info("Synchronization started. Total rows received: {}", rows.size());

        int processed = 0;

        for (GoogleSheetRowDto dto : rows) {

            if (synchronizeProject(dto)) {
                processed++;
            }

        }

        log.info("Synchronization completed successfully. Processed {} rows.", processed);
    }

    /**
     * Synchronizes a single Google Sheet row.
     */
    private boolean synchronizeProject(GoogleSheetRowDto dto) {

        if ("Pilot Coronary CTA".equals(dto.getProjectName())
                && "February, 2026".equals(dto.getReportingMonth())) {

            System.out.println(
                    "Project = " + dto.getProjectName()
                            + " | Week = [" + dto.getWeekOf() + "]"
                            + " | Assigned = " + dto.getAssignedToPareidolia()
                            + " | Submitted = " + dto.getAnnotationsSubmittedPareidolia()
                            + " | Issue = " + dto.getIssueCases()
                            + " | Assigned Date = " + dto.getAssignedDate()
            );
        }

        if (!isValidProject(dto)) {
            return false;
        }

        Project project = getOrCreateProject(dto);

        System.out.println(
                "Before Mapping -> " +
                        dto.getProjectName() +
                        " | Month = " +
                        dto.getReportingMonth()
        );

        mapProject(project, dto);

        System.out.println(
                "After Mapping -> Entity Month = "
                        + project.getReportingMonth()
        );

        updateProjectHealth(project);

        saveProject(project);

        log.debug("Synchronized Project : {} | Week : {}",
                dto.getProjectName(),
                dto.getWeekOf());

        return true;
    }

    /**
     * Validates mandatory project information.
     */
    private boolean isValidProject(GoogleSheetRowDto dto) {

        return dto.getProjectName() != null
                && !dto.getProjectName().isBlank();
    }

    /**
     * Finds an existing project or creates a new one.
     */
    private Project getOrCreateProject(GoogleSheetRowDto dto) {

        return projectRepository
                .findByProjectNameAndReportingMonthAndWeekOf(
                        dto.getProjectName(),
                        dto.getReportingMonth(),
                        dto.getWeekOf())
                .orElse(new Project());

    }

    /**
     * Maps DTO into Entity.
     */
    private void mapProject(Project project,
                            GoogleSheetRowDto dto) {

        projectMapper.updateEntity(project, dto);

    }

    /**
     * Updates calculated fields.
     */
    private void updateProjectHealth(Project project) {

        project.setProjectHealth(
                calculateProjectHealth(project)
        );

    }



    /**
     * Saves project into database.
     */
    private void saveProject(Project project) {

        System.out.println(
                "Saving -> "
                        + project.getReportingMonth()
                        + " | "
                        + project.getProjectName()
                        + " | Week=[" + project.getWeekOf() + "]"
                        + " | Assigned="
                        + project.getAssignedToPareidolia()
                        + " | Submitted="
                        + project.getAnnotationsSubmittedPareidolia()
                        + " | Issue="
                        + project.getIssueCases()
                        + " | Assigned Date="
                        + project.getAssignedDate()
        );

        projectRepository.save(project);

    }

    /**
     * Version 1 Project Health Calculation.
     */
    private String calculateProjectHealth(Project project) {

        if (project.getPendingAnnotations() == null
                || project.getPendingAnnotations() == 0) {

            return "GREEN";
        }

        // Future Version
        // ETD
        // Priority
        // Delay
        // SLA
        // Completion %

        return "GREEN";
    }

}