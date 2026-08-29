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

        /*
         * Make sure all synchronized rows are written before
         * project-month Health is calculated.
         */
        projectRepository.flush();

        /*
         * Second pass:
         * calculate one Health for each project + reporting month.
         */
        recalculateProjectMonthHealth(rows);

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
                .findByProjectNameAndReportingMonthAndReportingPeriod(
                        dto.getProjectName(),
                        dto.getReportingMonth(),
                        dto.getReportingPeriod())
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
//    private void updateProjectHealth(Project project) {
//
//        project.setProjectHealth(
//                calculateProjectHealth(project)
//        );
//
//    }


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
//    private String calculateProjectHealth(Project project) {
//
//        if (project.getPendingAnnotations() == null
//                || project.getPendingAnnotations() == 0) {
//
//            return "GREEN";
//        }
//
//        // Future Version
//        // ETD
//        // Priority
//        // Delay
//        // SLA
//        // Completion %
//
//        return "GREEN";
//    }
    private void recalculateProjectMonthHealth(
            List<GoogleSheetRowDto> sourceRows) {

        /*
         * Get each Project + Reporting Month combination only once.
         */
        var projectMonths = sourceRows.stream()

                .filter(this::isValidProject)

                .filter(dto ->
                        dto.getReportingMonth() != null
                                && !dto.getReportingMonth().isBlank()
                )

                .map(dto ->
                        new ProjectMonthKey(
                                dto.getProjectName(),
                                dto.getReportingMonth()
                        )
                )

                .distinct()

                .toList();


        for (ProjectMonthKey key : projectMonths) {

            List<Project> monthRows =
                    projectRepository
                            .findByProjectNameAndReportingMonth(
                                    key.projectName(),
                                    key.reportingMonth()
                            );

            if (monthRows.isEmpty()) {
                continue;
            }

            /*
             * Period 0 is context/carry-forward.
             *
             * Health should be calculated from genuine
             * reporting rows only.
             */
            List<Project> reportingRows =
                    monthRows.stream()
                            .filter(project ->
                                    project.getReportingPeriod() != null
                                            && project.getReportingPeriod() > 0
                            )
                            .toList();

            if (reportingRows.isEmpty()) {
                continue;
            }


            /*
             * Pending can appear on a different row from ETD.
             *
             * We only need to know whether pending cases exist.
             * MAX is safer than SUM because we do not want to
             * accidentally double-count repeated values.
             */
            int pendingCases =
                    reportingRows.stream()

                            .map(Project::getPendingAnnotations)

                            .filter(value -> value != null)

                            .max(Integer::compareTo)

                            .orElse(0);


            /*
             * Find the most recent reporting-period row
             * containing a nonblank ETD.
             *
             * Example:
             *
             * Pilot
             * P1 -> Pending 161
             * P2 -> ETD 31st July
             *
             * Effective ETD = P2's ETD.
             */
            Project etdRow =
                    reportingRows.stream()

                            .filter(project ->
                                    project.getEtd() != null
                                            && !project.getEtd().isBlank()
                            )

                            .max(
                                    java.util.Comparator.comparingInt(
                                            project ->
                                                    project.getReportingPeriod() == null
                                                            ? -1
                                                            : project.getReportingPeriod()
                                    )
                            )

                            .orElse(null);


            String effectiveEtd =
                    etdRow == null
                            ? null
                            : etdRow.getEtd();


            /*
             * reportingMonthDate should be the same for all
             * rows in this project/month.
             */
            java.time.LocalDate reportingMonthDate =
                    monthRows.get(0).getReportingMonthDate();


            String health =
                    projectMapper.calculateProjectHealth(
                            pendingCases,
                            effectiveEtd,
                            reportingMonthDate
                    );


            /*
             * Health describes the PROJECT for that month,
             * not an individual reporting row.
             *
             * Therefore apply the same Health to all rows
             * including Period 0.
             */
            for (Project project : monthRows) {

                project.setProjectHealth(health);
            }


            projectRepository.saveAll(monthRows);

            log.info(
                    "Project Health -> Project='{}' | Month='{}' | Pending={} | ETD='{}' | Health={}",
                    key.projectName(),
                    key.reportingMonth(),
                    pendingCases,
                    effectiveEtd,
                    health
            );
        }


    }

    private record ProjectMonthKey(
            String projectName,
            String reportingMonth
    ) {
    }

}