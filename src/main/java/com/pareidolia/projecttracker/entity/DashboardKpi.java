package com.pareidolia.projecttracker.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "dashboard_kpi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DashboardKpi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "project_name", nullable = false)
    private String projectName;

    @Column(name = "reporting_month", nullable = false)
    private String reportingMonth;

    @Column(name = "reporting_period", nullable = false)
    private Integer reportingPeriod;


    @Column(name = "reporting_month_date")
    private LocalDate reportingMonthDate;

    @Column(name = "week_of")
    private String weekOf;

    @Column(name = "assigned_weekly")
    private Integer assignedWeekly;

    @Column(name = "assigned_ptd")
    private Integer assignedPtd;

    @Column(name = "submitted_weekly")
    private Integer submittedWeekly;

    @Column(name = "submitted_ptd")
    private Integer submittedPtd;

    @Column(name = "issue_weekly")
    private Integer issueWeekly;

    @Column(name = "issue_ptd")
    private Integer issuePtd;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "accepted_ptd")
    private Integer acceptedPtd;

    @Column(name = "rejected_weekly")
    private Integer rejectedWeekly;


    @Column(name = "rejected_ptd")
    private Integer rejectedPtd;

    @Column(name = "accepted_ptd_percentage")
    private Double acceptedPtdPercentage;

    @Column(name = "rejected_ptd_percentage")
    private Double rejectedPtdPercentage;

}