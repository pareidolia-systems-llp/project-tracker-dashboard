package com.pareidolia.projecttracker.mapper;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import org.springframework.stereotype.Component;

@Component
public class ProjectMapper {

    public Project toEntity(GoogleSheetRowDto dto) {

        Project project = new Project();

        updateEntity(project, dto);

        return project;
    }

    public void updateEntity(Project project, GoogleSheetRowDto dto) {

        project.setProjectName(dto.getProjectName());
        project.setProjectType(dto.getProjectType());
        project.setPriority(dto.getPriority());
        project.setVendor(dto.getVendor());

        /* ==========================
           Reporting Month
           ========================== */

        project.setReportingMonth(dto.getReportingMonth());
        project.setReportingMonthDate(dto.getReportingMonthDate());

        /* ==========================
           Dataset Information
           ========================== */

        project.setTotalDataRequestedByAml(dto.getTotalDataRequestedByAml());
        project.setDataRequested(dto.getDataRequested());
        project.setDataReceived(dto.getDataReceived());

        project.setNumberOfCasesReceived(
                parseInteger(dto.getNumberOfCasesReceived())
        );

        /* ==========================
           Workflow Information
           ========================== */

        project.setCasesInGroundTruth(
                parseInteger(dto.getCasesInGroundTruth())
        );

        project.setOngoingRedbrickUploads(
                parseInteger(dto.getOngoingRedbrickUploads())
        );

        project.setAssignedToPareidolia(
                parseInteger(dto.getAssignedToPareidolia())
        );

        project.setAnnotationPendingFromPareidolia(
                parseInteger(dto.getAnnotationPendingFromPareidolia())
        );

        project.setReviewsPendingFromPareidolia(
                parseInteger(dto.getReviewsPendingFromPareidolia())
        );

        project.setPendingRapidAiReview(
                parseInteger(dto.getPendingRapidAiReview())
        );

        project.setRapidReviewWeeklyAssigned(
                parseInteger(dto.getRapidReviewWeeklyAssigned())
        );

        project.setAnnotationsSubmittedPareidolia(
                parseInteger(dto.getAnnotationsSubmittedPareidolia())
        );

        project.setTotalAnnotationsSubmitted(
                parseInteger(dto.getTotalAnnotationsSubmitted())
        );

        /* ==========================
           Quality Information
           ========================== */

        project.setCasesNotAnnotatedDueToQuality(
                parseInteger(dto.getCasesNotAnnotatedDueToQuality())
        );

        project.setIssueCases(
                parseInteger(dto.getIssueCases())
        );

        /* ==========================
           Delivery Information
           ========================== */

        project.setCasesMovedToLabels(
                parseInteger(dto.getCasesMovedToLabels())
        );

        project.setEtd(dto.getEtd());

        project.setAnnotationsCompleted(
                parseInteger(dto.getAnnotationsCompleted())
        );

        System.out.println(
                "COLUMN Z -> Project="
                        + dto.getProjectName()
                        + " | Period="
                        + dto.getReportingPeriod()
                        + " | Raw Z=["
                        + dto.getPendingAnnotations()
                        + "]"
        );

        project.setPendingAnnotations(
                parseInteger(dto.getPendingAnnotations())
        );

        /* ==========================
           Reporting Information
           ========================== */

        project.setWeekOf(dto.getWeekOf());
        project.setAssignedDate(dto.getAssignedDate());
        project.setReportingPeriod(dto.getReportingPeriod());

        /* ==========================
           Status Information
           ========================== */

        project.setCurrentStatus(dto.getNotes());

        System.out.println(
                "DTO Month = " + dto.getReportingMonth() +
                        " | Month Date = " + dto.getReportingMonthDate() +
                        " | Project = " + dto.getProjectName()
        );

        if (project.getProjectHealth() == null) {
            project.setProjectHealth("GREEN");
        }
    }

    private Integer parseInteger(String value) {

        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(value.replace(",", "").trim());
        } catch (Exception e) {
            return 0;
        }
    }
}