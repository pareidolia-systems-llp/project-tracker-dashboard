package com.pareidolia.projecttracker.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.mapper.ColumnIndexMapper;
import com.pareidolia.projecttracker.mapper.GoogleSheetRowMapper;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.pareidolia.projecttracker.util.ReportingMonthUtil;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;

import java.util.List;
import java.util.Map;

import static com.pareidolia.projecttracker.constants.SheetColumns.*;

@Service
public class GoogleSheetsServiceImpl implements GoogleSheetsService {

    private final Sheets sheets;
    private final GoogleSheetRowMapper mapper;
    private final ColumnIndexMapper columnIndexMapper;


    @Value("${google.sheet.id}")
    private String spreadsheetId;

    public GoogleSheetsServiceImpl(
            Sheets sheets,
            GoogleSheetRowMapper mapper,
            ColumnIndexMapper columnIndexMapper) {

        this.sheets = sheets;
        this.mapper = mapper;
        this.columnIndexMapper = columnIndexMapper;
    }

    @Override
    public List<GoogleSheetRowDto> readSheet()
            throws GeneralSecurityException, IOException {

        List<GoogleSheetRowDto> allRows = new ArrayList<>();

        for (String sheetName : getMonthlySheetNames()) {

            System.out.println("Reading sheet : " + sheetName);

            allRows.addAll(processSheet(sheetName));
        }

        return allRows;
    }

    private void validateHeaders(
            Map<String, Integer> columnIndexMap) {

        List<String> requiredHeaders = List.of(
                PROJECT_NAME,
                PROJECT_TYPE,
                PRIORITY,
                VENDOR,
                WEEK_OF
        );

        for (String header : requiredHeaders) {

            if (!columnIndexMap.containsKey(normalize(header))) {

                throw new IllegalStateException(
                        "Required column '" + header +
                                "' is missing from the Google Sheet."
                );
            }
        }
    }

    private List<String> getMonthlySheetNames()
            throws IOException {

        Spreadsheet spreadsheet = sheets.spreadsheets()
                .get(spreadsheetId)
                .execute();

        List<String> monthlySheets = new ArrayList<>();

        for (Sheet sheet : spreadsheet.getSheets()) {

            String sheetName =
                    sheet.getProperties().getTitle();

            System.out.println(
                    "Found Sheet : " + sheetName
            );

            // Ignore non-reporting sheets
            if (!sheetName.matches(".*\\d{4}$")) {
                continue;
            }

            monthlySheets.add(sheetName);
        }

        return monthlySheets;
    }

    private List<GoogleSheetRowDto> processSheet(
            String sheetName)
            throws IOException {

        String sheetRange =
                "'" + sheetName + "'!A2:Z";

        ValueRange response = sheets.spreadsheets()
                .values()
                .get(spreadsheetId, sheetRange)
                .execute();

        List<List<Object>> rows =
                response.getValues();

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> headerRow = rows.get(0);

        System.out.println("--------------------------------");
        System.out.println(
                "Sheet : " + sheetName
        );
        System.out.println(
                "Header Row : " + headerRow
        );
        System.out.println("--------------------------------");

        Map<String, Integer> columnIndexMap =
                columnIndexMapper.buildColumnIndexMap(
                        headerRow
                );

        System.out.println(
                "### RAW Y HEADER ### "
                        + " | Index=24"
                        + " | Value=["
                        + (headerRow.size() > 24
                        ? headerRow.get(24)
                        : "COLUMN NOT FOUND")
                        + "]"
        );

        if ("Feb 2026".equals(sheetName)) {

            System.out.println(
                    "### FEB 2026 HEADER DEBUG ###"
                            + " | W(22)=["
                            + (headerRow.size() > 22
                            ? headerRow.get(22)
                            : "NOT FOUND")
                            + "]"
                            + " | X(23)=["
                            + (headerRow.size() > 23
                            ? headerRow.get(23)
                            : "NOT FOUND")
                            + "]"
                            + " | Y(24)=["
                            + (headerRow.size() > 24
                            ? headerRow.get(24)
                            : "NOT FOUND")
                            + "]"
                            + " | Z(25)=["
                            + (headerRow.size() > 25
                            ? headerRow.get(25)
                            : "NOT FOUND")
                            + "]"
            );
        }
        System.out.println("Headers:");

        columnIndexMap.keySet()
                .forEach(System.out::println);

        validateHeaders(columnIndexMap);

        List<GoogleSheetRowDto> dtoList =
                new ArrayList<>();

        /*
         * These variables remember the values from the
         * most recent project header.
         *
         * Google Sheets often leaves these cells blank
         * for the following weekly rows.
         */
        String lastProjectName = null;
        String lastProjectType = null;
        String lastPriority = null;
        String lastTotalDataRequestedByAml = null;

        int reportingPeriod = 0;

        for (int i = 1; i < rows.size(); i++) {

            List<Object> row = rows.get(i);

            if ("Feb 2026".equals(sheetName)) {

                System.out.println(
                        "### FEB 2026 DATA Y ###"
                                + " | RowSize=[" + row.size() + "]"
                                + " | Project=["
                                + (row.size() > 1 ? row.get(1) : "N/A")
                                + "]"
                                + " | W(22)=["
                                + (row.size() > 22 ? row.get(22) : "NOT PRESENT")
                                + "]"
                                + " | X(23)=["
                                + (row.size() > 23 ? row.get(23) : "NOT PRESENT")
                                + "]"
                                + " | Y(24)=["
                                + (row.size() > 24 ? row.get(24) : "NOT PRESENT")
                                + "]"
                                + " | Z(25)=["
                                + (row.size() > 25 ? row.get(25) : "NOT PRESENT")
                                + "]"
                );
            }

            if (row.isEmpty()) {
                continue;
            }

            /*
             * STEP 1
             *
             * Map the raw Google Sheet row into DTO.
             */
            GoogleSheetRowDto dto =
                    mapper.mapRow(
                            row,
                            columnIndexMap
                    );

            if (dto.getProjectName() != null
                    && dto.getProjectName().contains("Neuro3D")) {

                System.out.println(
                        "========== NEURO3D FOUND ==========" +
                                "\nSheet = [" + sheetName + "]" +
                                "\nRaw Row Index = [" + i + "]" +
                                "\nProject = [" + dto.getProjectName() + "]" +
                                "\nWeek = [" + dto.getWeekOf() + "]" +
                                "\nAssigned = [" + dto.getAssignedToPareidolia() + "]" +
                                "\nSubmitted = [" + dto.getAnnotationsSubmittedPareidolia() + "]" +
                                "\nIssue = [" + dto.getIssueCases() + "]"
                );
            }

            /*
             * STEP 2
             *
             * Reporting month comes directly from
             * the sheet/tab name.
             */
            dto.setReportingMonth(sheetName);
            dto.setReportingMonthDate(
                    ReportingMonthUtil.parse(sheetName)
            );

            /*
             * STEP 3
             *
             * Determine the current project BEFORE
             * checking whether the row is a spacer.
             *
             * This is important because the project name
             * changes at the beginning of a new project
             * block.
             */

            // Project Name
            // Project Name
            if (dto.getProjectName() != null
                    && !dto.getProjectName().isBlank()) {

                // New project block
                lastProjectName = dto.getProjectName().trim();

                // Restart reporting period for the new project
                reportingPeriod = 0;

            } else {

                /*
                 * Blank project name.
                 *
                 * Normally this is a continuation of the current project.
                 * However, if the previous project has already completed
                 * all 4 reporting periods and this row has no Week Of,
                 * this can be a project-level row belonging to the
                 * NEXT project.
                 */

                boolean projectLevelRowBeforeNextProject =
                        reportingPeriod >= 4
                                && dto.getWeekOf() != null
                                && dto.getWeekOf().isBlank()
                                && hasProjectLevelData(dto);

                if (projectLevelRowBeforeNextProject) {

                    String nextProjectName =
                            findNextProjectName(rows, i, columnIndexMap);

                    if (nextProjectName != null) {

                        dto.setProjectName(nextProjectName);

                        // This is a project-level row, not Week 1.
                        reportingPeriod = 0;

                        lastProjectName = nextProjectName;

                    } else {

                        dto.setProjectName(lastProjectName);
                    }

                } else {

                    // Normal continuation row
                    dto.setProjectName(lastProjectName);
                }
            }
            // Project Type
            if (dto.getProjectType() != null
                    && !dto.getProjectType().isBlank()) {

                lastProjectType =
                        dto.getProjectType().trim();

            } else {

                dto.setProjectType(
                        lastProjectType
                );
            }

            // Priority
            if (dto.getPriority() != null
                    && !dto.getPriority().isBlank()) {

                lastPriority =
                        dto.getPriority().trim();

            } else {

                dto.setPriority(
                        lastPriority
                );
            }

            // Total Data Requested By AML
            if (dto.getTotalDataRequestedByAml() != null
                    && !dto.getTotalDataRequestedByAml().isBlank()) {

                lastTotalDataRequestedByAml =
                        dto.getTotalDataRequestedByAml().trim();

            } else {

                dto.setTotalDataRequestedByAml(
                        lastTotalDataRequestedByAml
                );
            }

            /*
             * STEP 4
             *
             * NOW check whether this is a spacer row.
             *
             * We deliberately do this AFTER project
             * identification.
             */
            if (isSpacerRow(dto)) {

                continue;
            }
            /*
             * Determine the business reporting period.
             *
             * A project can have a maximum of 4 reporting periods:
             *
             * 1 = 01-07
             * 2 = 08-14
             * 3 = 15-21
             * 4 = 22-last day
             *
             * If we already processed 4 periods and encounter
             * another data-bearing row with a blank Week Of,
             * treat it as a project-level row rather than
             * creating Period 5.
             */

            if (reportingPeriod >= 4
                    && dto.getWeekOf() != null
                    && dto.getWeekOf().isBlank()) {

                dto.setReportingPeriod(0);

                System.out.println(
                        "PROJECT LEVEL ROW -> "
                                + "Sheet=[" + sheetName + "]"
                                + " | Project=[" + dto.getProjectName() + "]"
                                + " | Week=[]"
                                + " | Issue=[" + dto.getIssueCases() + "]"
                                + " | Submitted=["
                                + dto.getAnnotationsSubmittedPareidolia()
                                + "]"
                );

            } else {

                reportingPeriod++;

                if (reportingPeriod > 4) {

                    System.out.println(
                            "WARNING -> More than 4 reporting rows found"
                                    + " | Project=[" + dto.getProjectName() + "]"
                                    + " | Sheet=[" + sheetName + "]"
                                    + " | Week=[" + dto.getWeekOf() + "]"
                    );

                    /*
                     * Do not create Period 5.
                     *
                     * Skip this unexpected row for now.
                     */
                    continue;
                }

                dto.setReportingPeriod(reportingPeriod);
            }

            /*
             * STEP 6
             *
             * Debug information.
             */
            System.out.println(
                    "### ACCEPTED REJECTED TEST ###"
                            + " | Sheet=[" + sheetName + "]"
                            + " | Project=[" + dto.getProjectName() + "]"
                            + " | Week=[" + dto.getWeekOf() + "]"
                            + " | Assigned=[" + dto.getAssignedToPareidolia() + "]"
                            + " | AssignedDate=[" + dto.getAssignedDate() + "]"
                            + " | Submitted=[" + dto.getAnnotationsSubmittedPareidolia() + "]"
                            + " | Rejected(W)=[" + dto.getCasesMovedToLabels() + "]"
                            + " | Accepted(Y)=[" + dto.getAnnotationsCompleted() + "]"
                            + " | Issue=[" + dto.getIssueCases() + "]"
                            + " | ReportingPeriod=[" + dto.getReportingPeriod() + "]"
            );

            System.out.println(
                    "DTO -> "
                            + sheetName
                            + " | Project="
                            + dto.getProjectName()
                            + " | Week=["
                            + dto.getWeekOf()
                            + "]"
                            + " | Assigned="
                            + dto.getAssignedToPareidolia()
                            + " | Submitted="
                            + dto.getAnnotationsSubmittedPareidolia()
                            + " | Issue="
                            + dto.getIssueCases()
                            + " | Assigned Date="
                            + dto.getAssignedDate()
            );

            /*
             * STEP 7
             *
             * Special November debugging for Pilot Coronary CTA.
             */
            if ("November 2025".equals(sheetName)
                    && "Pilot Coronary CTA".equals(
                    dto.getProjectName())) {

                System.out.println(
                        "========== NOVEMBER PILOT DTO =========="
                                + "\nSheet = [" + sheetName + "]"
                                + "\nProject = [" + dto.getProjectName() + "]"
                                + "\nWeek = [" + dto.getWeekOf() + "]"
                                + "\nAssigned = [" + dto.getAssignedToPareidolia() + "]"
                                + "\nAssigned Date = [" + dto.getAssignedDate() + "]"
                                + "\nSubmitted = [" + dto.getAnnotationsSubmittedPareidolia() + "]"
                                + "\nIssue = [" + dto.getIssueCases() + "]"
                );
            }

            /*
             * STEP 8
             *
             * Add the completed DTO to the list.
             */
            dtoList.add(dto);
        }

        return dtoList;
    }

    private String normalize(String header) {

        return header == null
                ? ""
                : header.replace("\n", " ")
                .replace("\r", " ")
                .replaceAll("\\s+", " ")
                .trim()
                .toLowerCase();
    }

    private boolean isSpacerRow(
            GoogleSheetRowDto dto) {

        return dto.getWeekOf().isBlank()
                && dto.getAssignedToPareidolia().isBlank()
                && dto.getAnnotationsSubmittedPareidolia().isBlank()
                && dto.getCasesMovedToLabels().isBlank()
                && dto.getAnnotationsCompleted().isBlank()
                && dto.getIssueCases().isBlank();
    }

    private boolean hasProjectLevelData(GoogleSheetRowDto dto) {

        return hasValue(dto.getIssueCases())
                || hasValue(dto.getAnnotationsSubmittedPareidolia())
                || hasValue(dto.getAssignedToPareidolia())
                || hasValue(dto.getAnnotationPendingFromPareidolia())
                || hasValue(dto.getReviewsPendingFromPareidolia())
                || hasValue(dto.getPendingRapidAiReview())
                || hasValue(dto.getCasesMovedToLabels())
                ||hasValue(dto.getAnnotationsCompleted());
    }
    private boolean hasValue(String value) {

        return value != null && !value.isBlank();
    }
    private String findNextProjectName(
            List<List<Object>> rows,
            int currentIndex,
            Map<String, Integer> columnIndexMap) {

        for (int i = currentIndex + 1; i < rows.size(); i++) {

            List<Object> nextRow = rows.get(i);

            if (nextRow == null || nextRow.isEmpty()) {
                continue;
            }

            GoogleSheetRowDto nextDto =
                    mapper.mapRow(
                            nextRow,
                            columnIndexMap
                    );



            String nextProjectName =
                    nextDto.getProjectName();

            if (nextProjectName != null
                    && !nextProjectName.isBlank()) {

                return nextProjectName.trim();
            }
        }

        return null;
    }
}