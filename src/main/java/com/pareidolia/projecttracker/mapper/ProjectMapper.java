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

        /*
         * Temporary row-level health calculation.
         * We are debugging ETD parsing first.
         */
        project.setProjectHealth(
                calculateProjectHealth(
                        project.getPendingAnnotations(),
                        project.getEtd(),
                        project.getReportingMonthDate()
                )
        );

        project.setProjectHealth(
                calculateProjectHealth(
                        project.getPendingAnnotations(),
                        project.getEtd(),
                        project.getReportingMonthDate()
                )
        );

        // TEMPORARY DEBUG TEST - REMOVE AFTER TESTING
//        if ("ICH 4.0 Multi-compartmental Hemorrhage"
//                .equalsIgnoreCase(project.getProjectName())
//                && LocalDate.of(2026, 7, 1)
//                .equals(project.getReportingMonthDate())
//                && Integer.valueOf(1).equals(dto.getReportingPeriod())) {
//
//            project.setProjectHealth("RED");
//        }

        if ("ICH 4.0 Multi-compartmental Hemorrhage"
                .equalsIgnoreCase(project.getProjectName())
                && project.getReportingMonthDate() != null
                && project.getReportingMonthDate().equals(LocalDate.of(2026, 7, 1))) {

            System.out.println(
                    ">>> JULY ICH HEALTH CHECK"
                            + " | Project=" + project.getProjectName()
                            + " | Period=" + dto.getReportingPeriod()
                            + " | Pending=" + project.getPendingAnnotations()
                            + " | ETD=[" + project.getEtd() + "]"
                            + " | Health=" + project.getProjectHealth()
            );
        }

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

    public String calculateProjectHealth(Integer pendingCases,
                                          String etd,
                                          LocalDate reportingMonthDate) {

        /*
         * No pending cases = GREEN regardless of ETD.
         */
        if (pendingCases == null || pendingCases <= 0) {

            System.out.println(
                    "HEALTH DEBUG -> Pending=" + pendingCases
                            + " | Raw ETD=[" + etd + "]"
                            + " | Result=GREEN (no pending cases)"
            );

            return "GREEN";
        }

        LocalDate etdDate =
                parseEtdDate(etd, reportingMonthDate);

        System.out.println(
                "HEALTH DEBUG -> Pending=" + pendingCases
                        + " | Raw ETD=[" + etd + "]"
                        + " | Parsed ETD=" + etdDate
                        + " | Today=" + LocalDate.now()
        );

        /*
         * Pending exists but ETD cannot be determined.
         * Keep GREEN for now.
         *
         * We can revise this later if senior wants
         * UNKNOWN / AMBER for missing ETD.
         */
        if (etdDate == null) {

            System.out.println(
                    "HEALTH DEBUG -> ETD PARSE FAILED -> GREEN"
            );

            return "GREEN";
        }

        long daysRemaining =
                ChronoUnit.DAYS.between(
                        LocalDate.now(),
                        etdDate
                );

        System.out.println(
                "HEALTH DEBUG -> Days Remaining="
                        + daysRemaining
        );

        /*
         * RED must be evaluated first.
         *
         * This also makes overdue ETDs RED,
         * because negative values are <= 2.
         */
        if (daysRemaining <= 2) {

            System.out.println(
                    "HEALTH DEBUG -> Result=RED"
            );

            return "RED";
        }

        /*
         * Pending cases and ETD within 5 days.
         */
        if (daysRemaining <= 5) {

            System.out.println(
                    "HEALTH DEBUG -> Result=AMBER"
            );

            return "AMBER";
        }

        System.out.println(
                "HEALTH DEBUG -> Result=GREEN"
        );

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