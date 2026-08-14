package com.pareidolia.projecttracker.scheduler;

import com.pareidolia.projecttracker.service.ProjectService;
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

   //@Scheduled(cron = "0 */10 * * * *")
 @Scheduled(cron = "0 */5 * * * *")
//     Keeping this commented only for testing
    public void synchronizeProjects() {

        log.info("Automatic Project Synchronization Started");

        long startTime = System.currentTimeMillis();

        try {

            projectService.synchronizeProjects();

            long endTime = System.currentTimeMillis();

            log.info("Automatic Project Synchronization Completed");
            log.info("Execution Time : {} ms", (endTime - startTime));

        } catch (GeneralSecurityException | IOException e) {

            log.error("Synchronization Failed", e);

        }
    }

}

