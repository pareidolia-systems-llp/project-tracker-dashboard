package com.pareidolia.projecttracker.scheduler;

import com.pareidolia.projecttracker.service.ProjectService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(
        name = "scheduler.enabled",
        havingValue = "true"
)
public class ProjectSyncScheduler {

    private final ProjectService projectService;


    /**
     * Routine synchronization.
     *
     * GoogleSheetsService selects only:
     *
     * N   = latest reporting month
     * N-1 = previous reporting month
     *
     * ProjectService performs validation,
     * safe month replacement and KPI rebuild.
     */
    @Scheduled(
            cron = "${scheduler.recent.cron}",
            zone = "${scheduler.zone}"
    )
    public void synchronizeRecentProjects() {

        log.info(
                "========== SCHEDULED RECENT SYNC STARTED =========="
        );

        long startTime =
                System.currentTimeMillis();

        try {

            projectService.synchronizeProjects();

            log.info(
                    "Scheduled Recent Sync completed successfully."
                            + " ExecutionTime={} ms",
                    System.currentTimeMillis() - startTime
            );

        } catch (Exception ex) {

            log.error(
                    "Scheduled Recent Sync failed.",
                    ex
            );
        }
    }


    /**
     * Historical audit.
     *
     * Does NOT parse or replace historical Projects.
     * It only compares raw sheet fingerprints.
     */
    @Scheduled(
            cron = "${scheduler.historical.cron}",
            zone = "${scheduler.zone}"
    )
    public void auditHistoricalProjects() {

        log.info(
                "========== SCHEDULED HISTORICAL AUDIT STARTED =========="
        );

        long startTime =
                System.currentTimeMillis();

        try {

            projectService.auditHistoricalProjects();

            log.info(
                    "Scheduled Historical Audit completed successfully."
                            + " ExecutionTime={} ms",
                    System.currentTimeMillis() - startTime
            );

        } catch (Exception ex) {

            log.error(
                    "Scheduled Historical Audit failed.",
                    ex
            );
        }
    }
}