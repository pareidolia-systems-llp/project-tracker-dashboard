package com.pareidolia.projecttracker.repository;

import com.pareidolia.projecttracker.entity.DashboardKpi;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface DashboardKpiRepository extends JpaRepository<DashboardKpi, Long> {

    Optional<DashboardKpi> findByProjectNameAndReportingMonthAndReportingPeriod(
            String projectName,
            String reportingMonth,
            Integer reportingPeriod
    );
}