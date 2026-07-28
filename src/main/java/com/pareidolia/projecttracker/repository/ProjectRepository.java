package com.pareidolia.projecttracker.repository;

import com.pareidolia.projecttracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ProjectRepository extends JpaRepository<Project, Long> {

    Optional<Project> findByProjectNameAndReportingMonthAndWeekOf(
            String projectName,
            String reportingMonth,
            String weekOf);


}

//This will allow us to:
//
//Find a project by name.
//        Check if it already exists.
//        Save new projects.
//Update existing ones.