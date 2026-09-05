package com.pareidolia.projecttracker.repository;

import com.pareidolia.projecttracker.entity.Project;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ProjectRepository
        extends JpaRepository<Project, Long> {


    Optional<Project>
    findByProjectNameAndReportingMonthAndReportingPeriod(
            String projectName,
            String reportingMonth,
            Integer reportingPeriod
    );


    List<Project> findByProjectName(
            String projectName
    );


    /*
     * Used for project-month level Health calculation.
     */
    List<Project> findByProjectNameAndReportingMonth(
            String projectName,
            String reportingMonth
    );


    /*
     * Find all Project rows belonging to one
     * normalized reporting month.
     */
    List<Project> findByReportingMonthDate(
            LocalDate reportingMonthDate
    );


    /*
     * Used by the snapshot safety check.
     *
     * Before replacing a month, compare the number
     * of incoming rows with the number already stored.
     */
    long countByReportingMonthDate(
            LocalDate reportingMonthDate
    );


    /*
     * Used by safe snapshot replacement.
     *
     * Deletes only the months included in the
     * successfully parsed + validated source snapshot.
     */
    long deleteByReportingMonthDateIn(
            Collection<LocalDate> reportingMonthDates
    );
}