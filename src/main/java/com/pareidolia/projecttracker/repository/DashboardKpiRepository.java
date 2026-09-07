package com.pareidolia.projecttracker.repository;

import com.pareidolia.projecttracker.entity.DashboardKpi;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

import java.util.Optional;

public interface DashboardKpiRepository extends JpaRepository<DashboardKpi, Long> {

    Optional<DashboardKpi> findByProjectNameAndReportingMonthAndReportingPeriod(
            String projectName,
            String reportingMonth,
            Integer reportingPeriod
    );


    @Modifying(
            flushAutomatically = true,
            clearAutomatically = true
    )
    @Query("""
        DELETE FROM DashboardKpi d
        WHERE d.reportingMonthDate >= :startMonth
        """)
    int deleteFromReportingMonth(
            @Param("startMonth")
            LocalDate startMonth
    );
}