package com.pareidolia.projecttracker.scheduler;

import com.pareidolia.projecttracker.service.ProjectService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.GeneralSecurityException;

@Component
@RequiredArgsConstructor
@Slf4j
public class ProjectSyncScheduler {

    private final ProjectService projectService;
    private final DashboardKpiPopulationService dashboardKpiPopulationService;

   //@Scheduled(cron = "0 */10 * * * *")
 @Scheduled(cron = "0 */5 * * * *")
//     Keeping this commented only for testing
 @Scheduled(cron = "0 */5 * * * *")
 public void synchronizeProjects() {

     log.info("Automatic Project Synchronization Started");

     long startTime = System.currentTimeMillis();

     try {

         // 1. Synchronize Google Sheet → projects
         projectService.synchronizeProjects();

         // 2. Refresh Dashboard KPIs
         int processed =
                 dashboardKpiPopulationService.populateExistingKpis();

         long endTime = System.currentTimeMillis();

         log.info("Dashboard KPI population completed. Processed={}", processed);

         log.info("Automatic Project Synchronization Completed");
         log.info("Execution Time : {} ms", (endTime - startTime));

     } catch (GeneralSecurityException | IOException e) {

         log.error("Synchronization Failed", e);

     } catch (Exception e) {

         log.error("Dashboard KPI population failed", e);
     }
 }

}

