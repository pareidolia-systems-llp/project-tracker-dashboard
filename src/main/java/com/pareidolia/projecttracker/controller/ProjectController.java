package com.pareidolia.projecttracker.controller;

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

    @PostMapping("/sync")
    public String synchronizeProjects()
            throws GeneralSecurityException, IOException {

        projectService.synchronizeProjects();

        return "Projects synchronized successfully.";

    }
}