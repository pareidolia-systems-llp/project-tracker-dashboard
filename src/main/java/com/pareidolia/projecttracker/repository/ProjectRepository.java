package com.pareidolia.projecttracker.repository;

import com.pareidolia.projecttracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {


    //This will allow us to:
//
//Find a project by name.
//        Check if it already exists.
//        Save new projects.
//Update existing ones.

    Optional<Project> findByProjectNameAndReportingMonthAndReportingPeriod(
            String projectName,
            String reportingMonth,
            Integer reportingPeriod);

    List<Project> findByProjectName(String projectName);

    // Used for project-month level Health calculation
    List<Project> findByProjectNameAndReportingMonth(
            String projectName,
            String reportingMonth
    );


}

