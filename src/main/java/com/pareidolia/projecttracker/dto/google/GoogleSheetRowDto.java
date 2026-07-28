package com.pareidolia.projecttracker.dto.google;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class GoogleSheetRowDto {

    /* ==========================
       Project Information
       ========================== */

    private String serialNumber;

    private String projectName;

    private String projectType;

    private String priority;

    private String totalDataRequestedByAml;

    private String vendor;

    private String reportingMonth;

    /* ==========================
       Dataset Information
       ========================== */

    private String dataRequested;

    private String dataReceived;

    private String numberOfCasesReceived;

    /* ==========================
       Workflow Information
       ========================== */

    private String casesInGroundTruth;

    private String ongoingRedbrickUploads;

    private String assignedToPareidolia;

    private String weekOf;

    private String assignedDate;

    private String annotationPendingFromPareidolia;

    private String reviewsPendingFromPareidolia;

    private String pendingRapidAiReview;

    private String rapidReviewWeeklyAssigned;

    private String annotationsSubmittedPareidolia;

    private String totalAnnotationsSubmitted;

    /* ==========================
       Quality Information
       ========================== */

    private String casesNotAnnotatedDueToQuality;

    private String issueCases;

    /* ==========================
       Delivery Information
       ========================== */

    private String casesMovedToLabels;

    private String etd;

    private String annotationsCompleted;

    private String pendingAnnotations;

    /* ==========================
       Remarks
       ========================== */

    private String notes;

}