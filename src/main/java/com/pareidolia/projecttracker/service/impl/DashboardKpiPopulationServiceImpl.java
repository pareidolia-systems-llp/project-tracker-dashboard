package com.pareidolia.projecttracker.service.impl;

import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.repository.DashboardKpiRepository;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.service.dashboard.DashboardCalculationService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Slice;

import java.time.LocalDate;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardKpiPopulationServiceImpl
        implements DashboardKpiPopulationService {

    private final ProjectRepository projectRepository;

    private final DashboardCalculationService
            dashboardCalculationService;

    private final DashboardKpiRepository
            dashboardKpiRepository;

    @PersistenceContext
    private EntityManager entityManager;


    /**
     * Existing/manual population behaviour.
     *
     * This method keeps the existing KPI rows
     * and updates/inserts KPI values for all Projects.
     */
    @Override
    @Transactional
    public int populateExistingKpis() {

        List<Project> projects =
                projectRepository.findAll();

        int processed = 0;
        int failed = 0;

        for (Project project : projects) {

            try {

                populateKpiForProject(project);

                processed++;

            } catch (Exception ex) {

                failed++;

                log.error(
                        "Failed KPI calculation"
                                + " -> Project='{}'"
                                + " | Month='{}'"
                                + " | Period={}"
                                + " | Week='{}'",
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getReportingPeriod(),
                        project.getWeekOf(),
                        ex
                );
            }
        }

        log.info(
                "Dashboard KPI population completed."
                        + " Processed={}, Failed={}",
                processed,
                failed
        );

        return processed;
    }


    /**
     * Safe complete KPI rebuild.
     *
     * Used after synchronization because a source
     * correction in an older month can affect PTD
     * values in later months.
     *
     * IMPORTANT:
     * This operation is fail-fast.
     *
     * If even one KPI calculation fails,
     * the transaction is rolled back.
     */
    @Override
    @Transactional
    public int rebuildAllKpis() {

        log.info(
                "Starting complete Dashboard KPI rebuild."
        );

        /*
         * Remove existing calculated KPI rows.
         *
         * Because this method is transactional,
         * these deletes are rolled back if any later
         * KPI calculation fails.
         */
        dashboardKpiRepository.deleteAllInBatch();

        List<Project> projects =
                projectRepository.findAll();

        int processed = 0;

        for (Project project : projects) {

            try {

                populateKpiForProject(project);

                processed++;

            } catch (Exception ex) {

                log.error(
                        "KPI rebuild aborted"
                                + " -> Project='{}'"
                                + " | Month='{}'"
                                + " | Period={}"
                                + " | Week='{}'",
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getReportingPeriod(),
                        project.getWeekOf(),
                        ex
                );

                /*
                 * Throwing from this @Transactional method
                 * causes the rebuild to roll back instead
                 * of leaving dashboard_kpi partially rebuilt.
                 */
                throw new IllegalStateException(
                        "Dashboard KPI rebuild failed"
                                + " | Project=["
                                + project.getProjectName()
                                + "]"
                                + " | Month=["
                                + project.getReportingMonth()
                                + "]"
                                + " | Period=["
                                + project.getReportingPeriod()
                                + "]",
                        ex
                );
            }
        }

        dashboardKpiRepository.flush();

        log.info(
                "Dashboard KPI rebuild completed successfully."
                        + " Processed={}",
                processed
        );

        return processed;
    }

    @Override
    @Transactional
    public int rebuildKpisFromMonth(
            LocalDate startMonth) {

        if (startMonth == null) {
            throw new IllegalArgumentException(
                    "KPI rebuild start month must not be null."
            );
        }

        log.info(
                "Starting incremental Dashboard KPI rebuild."
                        + " StartMonth={}",
                startMonth
        );

        /*
         * Delete only KPI rows whose cumulative values
         * may have been affected by the source change.
         *
         * Months before startMonth remain untouched.
         */
        int deleted =
                dashboardKpiRepository
                        .deleteFromReportingMonth(
                                startMonth
                        );

        log.info(
                "Dashboard KPI rows removed from {} onward: {}",
                startMonth,
                deleted
        );

        /*
         * Keep each batch deliberately small.
         *
         * This prevents Hibernate from retaining the
         * entire KPI rebuild in its persistence context.
         */
        final int batchSize = 25;

        int pageNumber = 0;
        int processed = 0;

        while (true) {

            Slice<Project> projectBatch =
                    projectRepository
                            .findByReportingMonthDateGreaterThanEqualOrderByReportingMonthDateAscIdAsc(
                                    startMonth,
                                    PageRequest.of(
                                            pageNumber,
                                            batchSize
                                    )
                            );

            for (Project project
                    : projectBatch.getContent()) {

                try {

                    populateKpiForProject(project);

                    processed++;

                } catch (Exception ex) {

                    log.error(
                            "Incremental KPI rebuild aborted"
                                    + " -> Project='{}'"
                                    + " | Month='{}'"
                                    + " | Period={}"
                                    + " | Week='{}'",
                            project.getProjectName(),
                            project.getReportingMonth(),
                            project.getReportingPeriod(),
                            project.getWeekOf(),
                            ex
                    );

                    throw new IllegalStateException(
                            "Incremental Dashboard KPI rebuild failed"
                                    + " | Project=["
                                    + project.getProjectName()
                                    + "]"
                                    + " | Month=["
                                    + project.getReportingMonth()
                                    + "]"
                                    + " | Period=["
                                    + project.getReportingPeriod()
                                    + "]",
                            ex
                    );
                }
            }

            /*
             * Push completed work to MySQL and release
             * managed Hibernate entities before the
             * next batch is loaded.
             */
            entityManager.flush();
            entityManager.clear();

            log.info(
                    "Incremental KPI batch completed."
                            + " Batch={}"
                            + " | ProcessedSoFar={}",
                    pageNumber + 1,
                    processed
            );

            if (!projectBatch.hasNext()) {
                break;
            }

            pageNumber++;
        }

        log.info(
                "Incremental Dashboard KPI rebuild completed."
                        + " StartMonth={}"
                        + " | Processed={}",
                startMonth,
                processed
        );

        return processed;
    }


    /**
     * Common KPI calculation for one Project row.
     *
     * Week-based calculation remains untouched.
     *
     * Blank Week Of rows use Reporting Period,
     * preserving the existing business rule.
     */
    private void populateKpiForProject(
            Project project) {

        if (project.getWeekOf() != null
                && !project.getWeekOf().trim().isEmpty()) {

            dashboardCalculationService
                    .calculateProjectPtd(
                            project.getProjectName(),
                            project.getReportingMonth(),
                            project.getWeekOf()
                    );

        } else {

            dashboardCalculationService
                    .calculateProjectPtdByPeriod(
                            project.getProjectName(),
                            project.getReportingMonth(),
                            project.getReportingPeriod()
                    );
        }
    }

    @Override
    @Transactional
    public int rebuildKpisForMonth(
            LocalDate reportingMonth) {

        if (reportingMonth == null) {
            throw new IllegalArgumentException(
                    "Reporting month must not be null."
            );
        }

        log.info(
                "Starting Dashboard KPI month rebuild."
                        + " Month={}",
                reportingMonth
        );

        int deleted =
                dashboardKpiRepository
                        .deleteByReportingMonthDate(
                                reportingMonth
                        );

        log.info(
                "Existing KPI rows removed."
                        + " Month={}"
                        + " | Rows={}",
                reportingMonth,
                deleted
        );

        List<Project> projects =
                projectRepository
                        .findByReportingMonthDate(
                                reportingMonth
                        );

        int processed = 0;

        for (Project project : projects) {

            try {

                populateKpiForProject(project);
                processed++;

            } catch (Exception ex) {

                log.error(
                        "Monthly KPI rebuild aborted"
                                + " -> Project='{}'"
                                + " | Month='{}'"
                                + " | Period={}"
                                + " | Week='{}'",
                        project.getProjectName(),
                        project.getReportingMonth(),
                        project.getReportingPeriod(),
                        project.getWeekOf(),
                        ex
                );

                throw new IllegalStateException(
                        "Monthly Dashboard KPI rebuild failed"
                                + " | Month=["
                                + reportingMonth
                                + "]"
                                + " | Project=["
                                + project.getProjectName()
                                + "]",
                        ex
                );
            }
        }

        dashboardKpiRepository.flush();

        log.info(
                "Dashboard KPI month rebuild completed."
                        + " Month={}"
                        + " | Processed={}",
                reportingMonth,
                processed
        );

        return processed;
    }
}