package com.pareidolia.projecttracker.mapper;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Locale;

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


    }

    private Integer parseInteger(String value) {

        if (value == null || value.isBlank()) {
            return 0;
        }

        try {
            return Integer.parseInt(
                    value.replace(",", "").trim()
            );
        } catch (Exception e) {
            return 0;
        }
    }

    /* =========================================================
       PROJECT HEALTH
       ========================================================= */

    public String calculateProjectHealth(
            Integer pendingCases,
            String etd,
            LocalDate reportingMonthDate) {

        /*
         * No pending cases = GREEN regardless of ETD.
         */
        if (pendingCases == null || pendingCases <= 0) {
            return "GREEN";
        }

        LocalDate etdDate =
                parseEtdDate(etd, reportingMonthDate);

        /*
         * Pending exists but ETD cannot be determined.
         */
        if (etdDate == null) {
            return "GREEN";
        }

        long daysRemaining =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        etdDate
                );

        /*
         * RED must be evaluated first.
         * Negative values also mean overdue.
         */
        if (daysRemaining <= 2) {
            return "RED";
        }

        /*
         * Pending cases and ETD within 5 days.
         */
        if (daysRemaining <= 5) {
            return "AMBER";
        }

        return "GREEN";
    }
    /* =========================================================
       ETD PARSING
       ========================================================= */

    private LocalDate parseEtdDate(String etd,
                                   LocalDate reportingMonthDate) {

        if (etd == null || etd.isBlank()) {
            return null;
        }

        /*
         * Examples handled:
         *
         * 10 Apr 2026
         * 22 Jul 2026
         * 31st July
         * 31 July
         * 01.07.2026
         * 01/07/2026
         */
        String normalized = etd
                .trim()

                // Google Sheet may contain non-breaking spaces
                .replace('\u00A0', ' ')

                // 31st -> 31
                // 22nd -> 22
                // 3rd  -> 3
                // 1st  -> 1
                .replaceAll(
                        "(?i)(\\d{1,2})(st|nd|rd|th)",
                        "$1"
                )

                .replace(",", "")

                .replaceAll("\\s+", " ")

                .trim();

        /*
         * First try formats where the year
         * is already present in the Sheet.
         */
        List<DateTimeFormatter> withYearFormats =
                List.of(
                        DateTimeFormatter.ofPattern(
                                "d MMM uuuu",
                                Locale.ENGLISH
                        ),
                        DateTimeFormatter.ofPattern(
                                "d MMMM uuuu",
                                Locale.ENGLISH
                        ),
                        DateTimeFormatter.ofPattern(
                                "d.M.uuuu",
                                Locale.ENGLISH
                        ),
                        DateTimeFormatter.ofPattern(
                                "d/M/uuuu",
                                Locale.ENGLISH
                        )
                );

        for (DateTimeFormatter formatter : withYearFormats) {

            try {

                return LocalDate.parse(
                        normalized,
                        formatter
                );

            } catch (DateTimeParseException ignored) {
                // Try the next format
            }
        }

        /*
         * Handle ETD values without a year.
         *
         * Example:
         *
         * "31st July"
         *
         * becomes:
         *
         * "31 July 2026"
         *
         * using reportingMonthDate.getYear().
         */
        if (reportingMonthDate != null) {

            String withYear =
                    normalized
                            + " "
                            + reportingMonthDate.getYear();

            List<DateTimeFormatter> withoutYearFormats =
                    List.of(
                            DateTimeFormatter.ofPattern(
                                    "d MMM uuuu",
                                    Locale.ENGLISH
                            ),
                            DateTimeFormatter.ofPattern(
                                    "d MMMM uuuu",
                                    Locale.ENGLISH
                            )
                    );

            for (DateTimeFormatter formatter
                    : withoutYearFormats) {

                try {

                    return LocalDate.parse(
                            withYear,
                            formatter
                    );

                } catch (DateTimeParseException ignored) {
                    // Try the next format
                }
            }
        }

        /*
         * Could not interpret the ETD.
         */
        return null;
    }
}