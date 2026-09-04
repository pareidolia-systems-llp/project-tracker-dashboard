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

        String endColumn = "AA";

        /*
         * Feb2025 is an older sheet and does not extend to column AA.
         * Reading A2:AA causes Google Sheets API INVALID_ARGUMENT.
         */
        if ("Feb2025".equalsIgnoreCase(sheetName.trim())) {
            endColumn = "Z";
        }

        String sheetRange =
                "'" + sheetName + "'!A2:" + endColumn;
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

        boolean preProjectBlockActive = false;

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

            dto.setSourceRowNumber(i + 2);

            if (dto.getProjectName().isBlank()
                    && hasProjectLevelData(dto)) {

                String nextProjectName =
                        findNextProjectName(rows, i, columnIndexMap);

                System.out.println(
                        "\n========== BOUNDARY CHECK =========="
                                + "\nSheet            = [" + sheetName + "]"
                                + "\nSource Row       = [" + dto.getSourceRowNumber() + "]"
                                + "\nSerial Number    = [" + dto.getSerialNumber() + "]"
                                + "\nRaw Project      = [" + dto.getProjectName() + "]"
                                + "\nPrevious Project = [" + lastProjectName + "]"
                                + "\nNext Project     = [" + nextProjectName + "]"
                                + "\nWeek Of          = [" + dto.getWeekOf() + "]"
                                + "\nAssigned Date    = [" + dto.getAssignedDate() + "]"
                                + "\nAssigned         = [" + dto.getAssignedToPareidolia() + "]"
                                + "\nSubmitted        = [" + dto.getAnnotationsSubmittedPareidolia() + "]"
                                + "\nIssue            = [" + dto.getIssueCases() + "]"
                                + "\n====================================\n"
                );
            }

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
            String normalizedReportingMonth = sheetName.trim();

            dto.setReportingMonth(normalizedReportingMonth);

            dto.setReportingMonthDate(
                    ReportingMonthUtil.parse(normalizedReportingMonth)
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

            boolean preProjectRow = false;
            // Project Name


            boolean explicitProjectContextRow = false;
            // Project Name
            if (dto.getProjectName() != null
                    && !dto.getProjectName().isBlank()) {

                String explicitProjectName =
                        dto.getProjectName().trim();

                /*
                 * If we already discovered this project's rows above
                 * the explicit Project Name, do NOT restart its
                 * reporting periods.
                 *
                 * Example:
                 * Issue 35 -> Aortic P0
                 * Issue 91 -> Aortic P1
                 * Aortic Dissection name appears afterward.
                 */
                boolean confirmsPreProjectBlock =
                        preProjectBlockActive
                                && explicitProjectName.equals(lastProjectName);

                if (confirmsPreProjectBlock) {

                    /*
                     * The Project Name simply confirms the project
                     * we already resolved.
                     */
                    preProjectBlockActive = false;

                } else {

                    /*
                     * Normal new project.
                     */
                    lastProjectName = explicitProjectName;
                    reportingPeriod = 0;
                    preProjectBlockActive = false;
                }

                boolean blankWeek =
                        dto.getWeekOf() == null
                                || dto.getWeekOf().isBlank();

                boolean hasIssue =
                        hasMeaningfulValue(dto.getIssueCases());

                boolean noAssigned =
                        !hasMeaningfulValue(dto.getAssignedToPareidolia());

                boolean noAssignedDate =
                        !hasValue(dto.getAssignedDate());

                boolean noSubmitted =
                        !hasMeaningfulValue(dto.getAnnotationsSubmittedPareidolia());

                /*
                 * Some project blocks begin with a cumulative Issue row.
                 *
                 * Example:
                 *
                 * ICH Small Volume Hemorrhage
                 * Week Of = blank
                 * Assigned = blank
                 * Assigned Date = blank
                 * Submitted = blank
                 * Issue = 516
                 *
                 * The Issue value represents cumulative context
                 * entering the month. It is NOT Reporting Period 1.
                 */
                if (blankWeek
                        && hasIssue
                        && noAssigned
                        && noAssignedDate
                        && noSubmitted) {

                    explicitProjectContextRow = true;

                    System.out.println(
                            "EXPLICIT PROJECT CONTEXT ROW"
                                    + " | Sheet=[" + sheetName + "]"
                                    + " | Row=[" + dto.getSourceRowNumber() + "]"
                                    + " | Project=[" + dto.getProjectName() + "]"
                                    + " | Issue=[" + dto.getIssueCases() + "]"
                    );
                }

            } else {

                boolean blankWeek =
                        dto.getWeekOf() == null
                                || dto.getWeekOf().isBlank();
                String nextProjectName =
                        findNextProjectName(
                                rows,
                                i,
                                columnIndexMap
                        );

                boolean startsPreProjectBlock =
                        !preProjectBlockActive
                                && blankWeek
                                && hasProjectLevelData(dto)
                                && isClearlyPreProjectContext(dto)
                                && nextProjectName != null
                                && !nextProjectName.equals(lastProjectName)
                                && (
                                /*
                                 * Safest case:
                                 * this context row is directly before
                                 * the next explicit project.
                                 */
                                isImmediatelyBeforeNextProject(
                                        rows,
                                        i,
                                        columnIndexMap
                                )

                                        ||

                                        /*
                                         * Historical multi-row pre-project blocks
                                         * are allowed only when the block begins
                                         * after a genuine spacer.
                                         *
                                         * This prevents continuation rows from the
                                         * previous project being stolen by the
                                         * project below.
                                         */
                                        (
                                                isFirstDataRowAfterSpacer(rows, i)
                                                        && hasAnotherDataRowBeforeNextProject(
                                                        rows,
                                                        i,
                                                        columnIndexMap
                                                )
                                        )
                        );
                if (startsPreProjectBlock) {



                    if (nextProjectName == null) {

                        throw new IllegalStateException(
                                "Unable to determine project for boundary row"
                                        + " | Sheet=[" + sheetName + "]"
                                        + " | Row=[" + dto.getSourceRowNumber() + "]"
                        );
                    }

                    dto.setProjectName(nextProjectName);

                    /*
                     * This row belongs to the project that starts
                     * immediately below it.
                     */
                    lastProjectName = nextProjectName;

                    /*
                     * Important:
                     * This is NOT Week 1.
                     */
                    reportingPeriod = 0;
                    preProjectRow = true;

                    preProjectBlockActive = true;

                    System.out.println(
                            "PRE-PROJECT ROW RESOLVED"
                                    + " | Sheet=[" + sheetName + "]"
                                    + " | Row=[" + dto.getSourceRowNumber() + "]"
                                    + " | Project=[" + nextProjectName + "]"
                                    + " | Issue=[" + dto.getIssueCases() + "]"
                                    + " | Submitted=["
                                    + dto.getAnnotationsSubmittedPareidolia()
                                    + "]"
                    );

                } else {

                    /*
                     * Normal weekly/continuation row.
                     */
                    if (lastProjectName == null) {

                        throw new IllegalStateException(
                                "Blank project encountered before any project header"
                                        + " | Sheet=[" + sheetName + "]"
                                        + " | Row=[" + dto.getSourceRowNumber() + "]"
                        );
                    }

                    dto.setProjectName(lastProjectName);
                }
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
             * Ignore genuine spacer rows only after
             * project ownership has been resolved.
             */
            if (isSpacerRow(dto)) {
                continue;
            }


// * Determine the business reporting period.
//                    *
//                    * A project can have a maximum of 5 reporting periods.
// *
// * Example for July:
// *
// * 1 = 01-05
//                    * 2 = 06-12
//                    * 3 = 13-19
//                    * 4 = 20-26
//                    * 5 = 27-31
//                    *
//                    * Period 0 is reserved for cumulative / project-level /
//                    * pre-project context rows.


            if (preProjectRow || explicitProjectContextRow) {

                /*
                 * Row belongs to the project,
                 * but it is NOT one of the genuine
                 * reporting periods.
                 */
                dto.setReportingPeriod(0);

                System.out.println(
                        "PRE-PROJECT PERIOD 0"
                                + " | Sheet=[" + sheetName + "]"
                                + " | Row=[" + dto.getSourceRowNumber() + "]"
                                + " | Project=[" + dto.getProjectName() + "]"
                                + " | Issue=[" + dto.getIssueCases() + "]"
                );

            } else {

                /*
                 * Genuine reporting row.
                 *
                 * Week Of may be filled or blank.
                 * There is no hard maximum number of reporting periods.
                 */
                reportingPeriod++;

                dto.setReportingPeriod(reportingPeriod);

                /*
                 * More than 5 periods is unusual,
                 * but we must NEVER discard business data.
                 */
                if (reportingPeriod > 5) {

                    System.out.println(
                            "WARNING -> More than 5 reporting periods found"
                                    + " | Project=[" + dto.getProjectName() + "]"
                                    + " | Sheet=[" + sheetName + "]"
                                    + " | Period=[" + reportingPeriod + "]"
                                    + " | Week=[" + dto.getWeekOf() + "]"
                    );
                }
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

        return !hasValue(dto.getWeekOf())
                && !hasValue(dto.getAssignedToPareidolia())
                && !hasValue(dto.getAssignedDate())
                && !hasValue(dto.getAnnotationPendingFromPareidolia())
                && !hasValue(dto.getReviewsPendingFromPareidolia())
                && !hasValue(dto.getPendingRapidAiReview())
                && !hasValue(dto.getRapidReviewWeeklyAssigned())
                && !hasValue(dto.getAnnotationsSubmittedPareidolia())
                && !hasValue(dto.getIssueCases())
                && !hasValue(dto.getCasesMovedToLabels())
                && !hasValue(dto.getEtd())
                && !hasValue(dto.getAnnotationsCompleted())
                && !hasValue(dto.getPendingAnnotations())
                && !hasValue(dto.getNotes());
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


    private boolean isClearlyPreProjectContext(
            GoogleSheetRowDto dto) {

        /*
         * "Previous Cases" is an explicit carry-forward/context marker.
         */
        boolean previousCases =
                hasValue(dto.getAssignedDate())
                        && dto.getAssignedDate()
                        .trim()
                        .toLowerCase()
                        .startsWith("previous cases");

        if (previousCases) {
            return true;
        }

        /*
         * Known historical pattern:
         * an Issue-only row can contain cumulative Issue context
         * for the project starting immediately below it.
         */
        boolean issueOnly =
                hasMeaningfulValue(dto.getIssueCases())
                        && !hasMeaningfulValue(dto.getAssignedToPareidolia())
                        && !hasMeaningfulValue(dto.getAnnotationsSubmittedPareidolia())
                        && !hasMeaningfulValue(dto.getAnnotationPendingFromPareidolia())
                        && !hasMeaningfulValue(dto.getReviewsPendingFromPareidolia())
                        && !hasMeaningfulValue(dto.getPendingRapidAiReview())
                        && !hasMeaningfulValue(dto.getCasesMovedToLabels())
                        && !hasMeaningfulValue(dto.getAnnotationsCompleted());

        return issueOnly;
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

    private boolean isImmediatelyBeforeNextProject(
            List<List<Object>> rows,
            int currentIndex,
            Map<String, Integer> columnIndexMap) {

        for (int j = currentIndex + 1; j < rows.size(); j++) {

            List<Object> nextRow = rows.get(j);

            if (!hasAnyRawValue(nextRow)) {
                continue;
            }

            GoogleSheetRowDto nextDto =
                    mapper.mapRow(
                            nextRow,
                            columnIndexMap
                    );

            /*
             * Stop at the first genuinely non-empty row.
             *
             * If it explicitly contains a project name,
             * the current row sits immediately before
             * a new project block.
             */
            return nextDto.getProjectName() != null
                    && !nextDto.getProjectName().isBlank();
        }

        return false;
    }

    private boolean hasAnotherDataRowBeforeNextProject(
            List<List<Object>> rows,
            int currentIndex,
            Map<String, Integer> columnIndexMap) {

        for (int j = currentIndex + 1; j < rows.size(); j++) {

            List<Object> nextRow = rows.get(j);

            if (!hasAnyRawValue(nextRow)) {
                continue;
            }

            GoogleSheetRowDto nextDto =
                    mapper.mapRow(
                            nextRow,
                            columnIndexMap
                    );

            /*
             * Once the next explicit Project Name appears,
             * the pre-project block has ended.
             */
            if (nextDto.getProjectName() != null
                    && !nextDto.getProjectName().isBlank()) {

                return false;
            }

            /*
             * An unnamed data-bearing row exists before
             * the next Project Name.
             *
             * Example:
             *
             * Issue 35  -> Period 0
             * Issue 91  -> Period 1
             * Aortic Dissection project name
             */
            if (hasProjectLevelData(nextDto)
                    || hasValue(nextDto.getWeekOf())
                    || hasValue(nextDto.getAssignedDate())
                    || hasValue(nextDto.getRapidReviewWeeklyAssigned())
                    || hasValue(nextDto.getEtd())
                    || hasValue(nextDto.getPendingAnnotations())
                    || hasValue(nextDto.getNotes())) {

                return true;
            }
        }

        return false;
    }

    private boolean hasAnyRawValue(List<Object> row) {

        if (row == null || row.isEmpty()) {
            return false;
        }

        for (Object value : row) {

            if (value != null
                    && !value.toString().trim().isEmpty()) {

                return true;
            }
        }

        return false;
    }

    private boolean isFirstDataRowAfterSpacer(
            List<List<Object>> rows,
            int currentIndex) {

        if (currentIndex <= 1) {
            return false;
        }

        List<Object> previousRow =
                rows.get(currentIndex - 1);

        return !hasAnyMeaningfulRawValue(previousRow);
    }
    private boolean hasAnyMeaningfulRawValue(
            List<Object> row) {

        if (row == null || row.isEmpty()) {
            return false;
        }

        for (Object value : row) {

            if (value == null) {
                continue;
            }

            String text =
                    value.toString().trim();

            if (text.isEmpty()) {
                continue;
            }

            /*
             * Zero-only cells do not make a separator
             * a meaningful business-data row.
             */
            if (text.matches("0+(\\.0+)?")) {
                continue;
            }

            return true;
        }

        return false;
    }
    private boolean hasMeaningfulValue(String value) {

        if (value == null || value.isBlank()) {
            return false;
        }

        String normalized =
                value.trim()
                        .replace(",", "");

        try {

            return Double.parseDouble(normalized) != 0.0;

        } catch (NumberFormatException ex) {


            // Text such as "Previous Cases" is meaningful.
            return true;
        }
    }
}