package com.pareidolia.projecttracker.controller;

import com.pareidolia.projecttracker.security.RefreshApiKeyValidator;
import com.pareidolia.projecttracker.service.ProjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;

@RestController
@RequestMapping("/api/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final RefreshApiKeyValidator refreshApiKeyValidator;


    /*
     * Normal inexpensive refresh.
     *
     * Eventually:
     * Google Sheets latest two months
     * -> validate
     * -> replace recent DB months
     * -> rebuild KPI
     */
    @PostMapping("/sync")
    public String synchronizeProjects(
            @RequestHeader("X-Refresh-Key") String refreshKey)
            throws GeneralSecurityException, IOException {

        refreshApiKeyValidator.validate(refreshKey);

        projectService.synchronizeProjects();

        return "Recent project synchronization completed successfully.";
    }


    /*
     * Historical reconciliation.
     *
     * This will be run infrequently,
     * for example every alternate day.
     */
    @PostMapping("/sync/historical-audit")
    public String auditHistoricalProjects(
            @RequestHeader("X-Refresh-Key") String refreshKey)
            throws GeneralSecurityException, IOException {

        refreshApiKeyValidator.validate(refreshKey);

        projectService.auditHistoricalProjects();

        return "Historical project audit completed successfully.";
    }
}