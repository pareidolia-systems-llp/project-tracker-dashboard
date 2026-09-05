package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.mapper.ProjectMapper;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import com.pareidolia.projecttracker.service.ProjectService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import com.pareidolia.projecttracker.validation.ProjectSyncValidator;

import java.util.Objects;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;
import java.util.stream.Collectors;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;


import com.pareidolia.projecttracker.dto.sync.HistoricalSheetFingerprintDto;
import com.pareidolia.projecttracker.entity.SheetSyncState;
import com.pareidolia.projecttracker.repository.SheetSyncStateRepository;

import java.time.LocalDateTime;
import java.util.HashSet;


@Service
@RequiredArgsConstructor
@Slf4j
public class ProjectServiceImpl implements ProjectService {




    private final GoogleSheetsService googleSheetsService;
    private final ProjectRepository projectRepository;
    private final ProjectMapper projectMapper;

    private final SheetSyncStateRepository
            sheetSyncStateRepository;


    private final ProjectSyncValidator projectSyncValidator;

    private final DashboardKpiPopulationService
            dashboardKpiPopulationService;

    /*
     * Prevent manual refresh and scheduler refresh
     * from running simultaneously.
     */
    private final ReentrantLock syncLock =
            new ReentrantLock();




    /**
     * Synchronizes a single Google Sheet row.
     */
    @Override
    @Transactional
    public void synchronizeProjects()
            throws GeneralSecurityException, IOException {

        /*
         * Never allow two synchronization operations
         * to modify the database simultaneously.
         */
        if (!syncLock.tryLock()) {

            throw new IllegalStateException(
                    "Project synchronization is already running."
            );
        }

        try {

            log.info(
                    "========== RECENT PROJECT SYNC STARTED =========="
            );

            /*
             * readSheet() now reads only the latest
             * two reporting months.
             */
            List<GoogleSheetRowDto> rows =
                    googleSheetsService.readSheet();

            log.info(
                    "Recent Google Sheet rows parsed: {}",
                    rows.size()
            );


            /*
             * FIRST WALL:
             *
             * Nothing touches MySQL until the entire
             * parsed snapshot passes validation.
             */
            projectSyncValidator.validate(rows);

            log.info(
                    "Recent Google Sheet validation passed."
            );


            /*
             * SECOND WALL:
             *
             * Protect against accidental catastrophic
             * row loss caused by a broken parser or
             * unexpectedly damaged source tab.
             */
            validateSnapshotSizes(rows);


            /*
             * Replace only the months contained in
             * this parsed snapshot.
             *
             * For routine synchronization that means
             * the latest two months only.
             */
            replaceSourceMonths(rows);


            /*
             * Source changes can alter cumulative PTD.
             *
             * Rebuild calculated KPI rows after the
             * source snapshot is known to be valid.
             */
            int kpiProcessed =
                    dashboardKpiPopulationService
                            .rebuildAllKpis();


            log.info(
                    "Recent Project Sync completed successfully."
                            + " SourceRows={}"
                            + " | KpiRows={}",
                    rows.size(),
                    kpiProcessed
            );

        } finally {

            syncLock.unlock();
        }
    }

    @Override
    @Transactional
    public void auditHistoricalProjects()
            throws GeneralSecurityException, IOException {

        if (!syncLock.tryLock()) {

            throw new IllegalStateException(
                    "Project synchronization is already running."
            );
        }

        try {

            log.info(
                    "========== HISTORICAL AUDIT STARTED =========="
            );

            /*
             * IMPORTANT:
             *
             * Historical audit no longer parses old project rows.
             *
             * It only fingerprints the raw monthly sheets.
             */
            List<HistoricalSheetFingerprintDto> fingerprints =
                    googleSheetsService
                            .readHistoricalFingerprints();

            if (fingerprints.isEmpty()) {

                log.info(
                        "Historical Audit completed."
                                + " No historical sheets found."
                );

                return;
            }

            boolean initialBaselineRun =
                    sheetSyncStateRepository.count() == 0;


            LocalDateTime now =
                    LocalDateTime.now();


            int baselined = 0;
            int newlyDiscovered = 0;
            int unchanged = 0;
            int changed = 0;


            Set<LocalDate> currentHistoricalMonths =
                    new HashSet<>();


            for (HistoricalSheetFingerprintDto fingerprint
                    : fingerprints) {

                LocalDate month =
                        fingerprint.reportingMonthDate();

                currentHistoricalMonths.add(month);


                SheetSyncState state =
                        sheetSyncStateRepository
                                .findByReportingMonthDate(
                                        month
                                )
                                .orElse(null);


                /*
                 * FIRST AUDIT:
                 *
                 * We have no earlier fingerprint.
                 *
                 * Store the baseline WITHOUT touching
                 * Project or Dashboard KPI data.
                 */
                if (state == null) {

                    String initialStatus =
                            initialBaselineRun
                                    ? "BASELINED"
                                    : "NEW";

                    state =
                            SheetSyncState.builder()
                                    .reportingMonthDate(month)
                                    .sheetName(
                                            fingerprint.sheetName()
                                    )
                                    .contentHash(
                                            fingerprint.contentHash()
                                    )
                                    .lastCheckedAt(now)
                                    .lastChangedAt(null)
                                    .syncStatus(initialStatus)
                                    .build();

                    sheetSyncStateRepository.save(state);

                    if (initialBaselineRun) {

                        baselined++;

                    } else {

                        newlyDiscovered++;
                    }

                    log.info(
                            "Historical Audit -> {}"
                                    + " | Month={}"
                                    + " | Sheet={}",
                            initialStatus,
                            month,
                            fingerprint.sheetName()
                    );

                    continue;
                }


                /*
                 * IMPORTANT:
                 *
                 * We deliberately DO NOT overwrite
                 * contentHash here.
                 *
                 * The old hash remains the last accepted
                 * baseline, so the change remains visible
                 * on every audit until it is reconciled.
                 */

                state.setLastCheckedAt(now);

                state.setSheetName(
                        fingerprint.sheetName()
                );

                if (Objects.equals(
                        state.getContentHash(),
                        fingerprint.contentHash())) {

                    state.setSyncStatus(
                            "UNCHANGED"
                    );

                    sheetSyncStateRepository.save(
                            state
                    );

                    unchanged++;

                    log.info(
                            "Historical Audit -> UNCHANGED"
                                    + " | Month={}"
                                    + " | Sheet={}",
                            month,
                            fingerprint.sheetName()
                    );

                    continue;
                }
                state.setSyncStatus(
                        "CHANGE_DETECTED"
                );

                state.setLastChangedAt(now);

                sheetSyncStateRepository.save(
                        state
                );

                changed++;

                log.warn(
                        "Historical Audit -> CHANGE DETECTED"
                                + " | Month={}"
                                + " | Sheet={}"
                                + " | Existing database history preserved.",
                        month,
                        fingerprint.sheetName()
                );
            }


            /*
             * A historical tab disappearing from Google Sheets
             * must NEVER delete corresponding MySQL history.
             */
            for (SheetSyncState existingState
                    : sheetSyncStateRepository.findAll()) {

                if (!currentHistoricalMonths.contains(
                        existingState
                                .getReportingMonthDate())) {

                    existingState.setLastCheckedAt(now);

                    existingState.setSyncStatus(
                            "SOURCE_TAB_MISSING"
                    );

                    sheetSyncStateRepository.save(
                            existingState
                    );

                    log.warn(
                            "Historical Audit -> SOURCE TAB MISSING"
                                    + " | Month={}"
                                    + " | Existing MySQL history preserved.",
                            existingState
                                    .getReportingMonthDate()
                    );
                }
            }


            log.info(
                    "Historical Audit completed successfully."
                            + " Checked={}"
                            + " | Baselined={}"
                            + " | New={}"
                            + " | Unchanged={}"
                            + " | Changed={}"
                            + " | ProjectsModified=0"
                            + " | KpiModified=0",
                    fingerprints.size(),
                    baselined,
                    newlyDiscovered,
                    unchanged,
                    changed
            );

        } finally {

            syncLock.unlock();
        }
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







    private void replaceSourceMonths(
            List<GoogleSheetRowDto> rows) {

        /*
         * Determine exactly which reporting months
         * are contained in this source snapshot.
         */
        Set<LocalDate> reportingMonthDates =
                rows.stream()

                        .filter(this::isValidProject)

                        .map(
                                GoogleSheetRowDto::
                                        getReportingMonthDate
                        )

                        .filter(date -> date != null)

                        .collect(
                                Collectors.toCollection(
                                        LinkedHashSet::new
                                )
                        );


        if (reportingMonthDates.isEmpty()) {

            throw new IllegalStateException(
                    "Synchronization aborted:"
                            + " no reporting months were resolved."
            );
        }


        log.info(
                "Replacing source months: {}",
                reportingMonthDates
        );


        /*
         * Delete only these months.
         *
         * Historical months absent from the incoming
         * snapshot remain untouched.
         */
        long deletedRows =
                projectRepository
                        .deleteByReportingMonthDateIn(
                                reportingMonthDates
                        );

        projectRepository.flush();


        log.info(
                "Existing Project rows removed"
                        + " for replacement: {}",
                deletedRows
        );


        List<Project> freshProjects =
                new ArrayList<>();


        for (GoogleSheetRowDto dto : rows) {

            if (!isValidProject(dto)) {
                continue;
            }

            /*
             * Create a completely fresh entity.
             *
             * We deliberately do NOT use getOrCreateProject()
             * during snapshot replacement.
             */
            Project project =
                    new Project();

            projectMapper.updateEntity(
                    project,
                    dto
            );

            freshProjects.add(project);
        }


        if (freshProjects.isEmpty()) {

            throw new IllegalStateException(
                    "Synchronization aborted:"
                            + " no Project entities were created."
            );
        }


        projectRepository.saveAll(
                freshProjects
        );

        projectRepository.flush();


        log.info(
                "Fresh Project snapshot inserted."
                        + " Rows={}",
                freshProjects.size()
        );


        /*
         * Recalculate project-month Health only after
         * the complete fresh snapshot is present.
         */
        recalculateProjectMonthHealth(rows);

        projectRepository.flush();
    }
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

    private void validateSnapshotSizes(
            List<GoogleSheetRowDto> rows) {

        Map<LocalDate, Long> incomingCounts =
                rows.stream()

                        .filter(this::isValidProject)

                        .filter(dto ->
                                dto.getReportingMonthDate()
                                        != null
                        )

                        .collect(
                                Collectors.groupingBy(
                                        GoogleSheetRowDto::
                                                getReportingMonthDate,
                                        Collectors.counting()
                                )
                        );


        for (Map.Entry<LocalDate, Long> entry
                : incomingCounts.entrySet()) {

            LocalDate reportingMonthDate =
                    entry.getKey();

            long incomingCount =
                    entry.getValue();

            long existingCount =
                    projectRepository
                            .countByReportingMonthDate(
                                    reportingMonthDate
                            );


            /*
             * New month:
             * there is nothing meaningful to compare.
             */
            if (existingCount == 0) {
                continue;
            }


            /*
             * Small months can naturally fluctuate,
             * so apply catastrophic-shrink protection
             * only when there is a meaningful existing
             * snapshot.
             */
            if (existingCount >= 10
                    && incomingCount * 2 < existingCount) {

                throw new IllegalStateException(
                        "Synchronization aborted:"
                                + " suspicious source row reduction"
                                + " | Month=["
                                + reportingMonthDate
                                + "]"
                                + " | ExistingRows=["
                                + existingCount
                                + "]"
                                + " | IncomingRows=["
                                + incomingCount
                                + "]"
                );
            }
        }
    }








    private record ProjectMonthKey(
            String projectName,
            String reportingMonth
    ) {
    }


}