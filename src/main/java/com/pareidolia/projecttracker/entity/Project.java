package com.pareidolia.projecttracker.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "projects",uniqueConstraints = {
@UniqueConstraint(
        columnNames = {
                "project_name",
                "reporting_month",
                "reporting_period"

        }
)
    })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /* ==========================
       Project Information
       ========================== */

    @Column(nullable = false)
    private String projectName;

    private String projectType;

    private String priority;

    private String vendor;



    /* ==========================
       Dataset Information
       ========================== */

    private String totalDataRequestedByAml;

    private String dataRequested;

    private String dataReceived;

    private Integer numberOfCasesReceived;

    /* ==========================
       Workflow Information
       ========================== */

    private Integer casesInGroundTruth;

    private Integer ongoingRedbrickUploads;

    private Integer assignedToPareidolia;

    private Integer annotationPendingFromPareidolia;

    private Integer reviewsPendingFromPareidolia;

    private Integer pendingRapidAiReview;

    private Integer rapidReviewWeeklyAssigned;

    private Integer annotationsSubmittedPareidolia;

    private Integer totalAnnotationsSubmitted;

    /* ==========================
       Quality Information
       ========================== */

    private Integer casesNotAnnotatedDueToQuality;

    private Integer issueCases;

    /* ==========================
       Delivery Information
       ========================== */

    private Integer casesMovedToLabels;

    private String etd;

    private Integer annotationsCompleted;

    private Integer pendingAnnotations;

    /* ==========================
       Reporting Information
       ========================== */

    private String weekOf;

    @Column(name = "reporting_month", nullable = false, length = 20)
    private String reportingMonth;



    private String assignedDate;

    @Column(name = "reporting_period")
    private Integer reportingPeriod;

    /* ==========================
       Status Information
       ========================== */

    private String currentStatus;

    private String projectHealth;


}