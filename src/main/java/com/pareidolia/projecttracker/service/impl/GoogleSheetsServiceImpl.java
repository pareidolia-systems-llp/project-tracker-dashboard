package com.pareidolia.projecttracker.service.impl;

import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Sheet;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.dto.sync.HistoricalSheetFingerprintDto;
import com.pareidolia.projecttracker.mapper.ColumnIndexMapper;
import com.pareidolia.projecttracker.mapper.GoogleSheetRowMapper;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import com.pareidolia.projecttracker.util.ReportingMonthUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HexFormat;
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

        /*
         * Routine synchronization.
         *
         * Only the latest two valid monthly reporting tabs
         * are processed.
         */
        List<String> monthlySheets =
                getSortedMonthlySheetNames();

        if (monthlySheets.isEmpty()) {

            throw new IllegalStateException(
                    "No valid monthly reporting sheets were found."
            );
        }

        int startIndex =
                Math.max(0, monthlySheets.size() - 2);

        List<String> recentSheets =
                new ArrayList<>(
                        monthlySheets.subList(
                                startIndex,
                                monthlySheets.size()
                        )
                );

        System.out.println(
                "Routine Sync Sheets = " + recentSheets
        );

        return readSelectedSheets(recentSheets);
    }

    private List<GoogleSheetRowDto> readSelectedSheets(
            List<String> sheetNames)
            throws IOException {

        List<GoogleSheetRowDto> allRows =
                new ArrayList<>();

        for (String sheetName : sheetNames) {

            System.out.println(
                    "Reading sheet : " + sheetName
            );

            List<GoogleSheetRowDto> sheetRows =
                    processSheet(sheetName);

            if (sheetRows.isEmpty()) {

                throw new IllegalStateException(
                        "Selected reporting sheet produced no rows"
                                + " | Sheet=[" + sheetName + "]"
                );
            }

            allRows.addAll(sheetRows);
        }

        return allRows;
    }



    @Override
    public List<HistoricalSheetFingerprintDto>
    readHistoricalFingerprints()
            throws GeneralSecurityException, IOException {

        List<String> monthlySheets =
                getSortedMonthlySheetNames();

        if (monthlySheets.size() <= 2) {

            System.out.println(
                    "Historical Audit -> No historical sheets found."
            );

            return Collections.emptyList();
        }


        /*
         * Everything except latest two tabs.
         */
        List<String> historicalSheets =
                new ArrayList<>(
                        monthlySheets.subList(
                                0,
                                monthlySheets.size() - 2
                        )
                );


        System.out.println(
                "Historical Fingerprint Sheets = "
                        + historicalSheets
        );


        List<HistoricalSheetFingerprintDto> results =
                new ArrayList<>();


        for (String sheetName : historicalSheets) {

            LocalDate reportingMonthDate =
                    safelyParseReportingMonth(
                            sheetName
                    );

            if (reportingMonthDate == null) {

                throw new IllegalStateException(
                        "Historical fingerprint failed:"
                                + " reporting month could not be resolved"
                                + " | Sheet=[" + sheetName + "]"
                );
            }


            String contentHash =
                    calculateRawSheetHash(
                            sheetName
                    );


            results.add(
                    new HistoricalSheetFingerprintDto(
                            sheetName,
                            reportingMonthDate
                                    .withDayOfMonth(1),
                            contentHash
                    )
            );


            System.out.println(
                    "Historical Fingerprint Created"
                            + " | Sheet=[" + sheetName + "]"
                            + " | Month=["
                            + reportingMonthDate
                            + "]"
            );
        }


        return results;
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

    private List<String> getSortedMonthlySheetNames()
            throws IOException {

        Spreadsheet spreadsheet =
                executeWithRetry(
                        () ->
                                sheets.spreadsheets()
                                        .get(spreadsheetId)
                                        .execute(),
                        "Read spreadsheet metadata"
                );

        /*
         * One chosen physical sheet per business month.
         */
        Map<LocalDate, MonthlySheet> selectedSheets =
                new HashMap<>();


        for (Sheet sheet : spreadsheet.getSheets()) {

            String sheetName =
                    sheet.getProperties()
                            .getTitle()
                            .trim();

            System.out.println(
                    "Found Sheet : " + sheetName
            );


            LocalDate reportingMonthDate =
                    safelyParseReportingMonth(
                            sheetName
                    );


            /*
             * Ignore things such as:
             *
             * Sheet1
             * Master Data
             * Instructions
             */
            if (reportingMonthDate == null) {

                System.out.println(
                        "Ignoring non-reporting sheet : "
                                + sheetName
                );

                continue;
            }


            reportingMonthDate =
                    reportingMonthDate.withDayOfMonth(1);


            MonthlySheet candidate =
                    new MonthlySheet(
                            sheetName,
                            reportingMonthDate
                    );


            MonthlySheet existing =
                    selectedSheets.get(
                            reportingMonthDate
                    );


            /*
             * First sheet found for this month.
             */
            if (existing == null) {

                selectedSheets.put(
                        reportingMonthDate,
                        candidate
                );

                continue;
            }


            boolean existingVersioned =
                    isVersionedSheetName(
                            existing.sheetName()
                    );

            boolean candidateVersioned =
                    isVersionedSheetName(
                            candidate.sheetName()
                    );


            /*
             * Example:
             *
             * existing  = Feb2025
             * candidate = Feb2025 Ver1.1
             *
             * Keep the plain/canonical sheet.
             */
            if (!existingVersioned
                    && candidateVersioned) {

                System.out.println(
                        "Ignoring versioned duplicate reporting sheet"
                                + " | Month=["
                                + reportingMonthDate
                                + "]"
                                + " | Keeping=["
                                + existing.sheetName()
                                + "]"
                                + " | Ignoring=["
                                + candidate.sheetName()
                                + "]"
                );

                continue;
            }


            /*
             * Reverse order:
             *
             * existing  = Feb2025 Ver1.1
             * candidate = Feb2025
             *
             * Replace the versioned tab with
             * the canonical/plain tab.
             */
            if (existingVersioned
                    && !candidateVersioned) {

                System.out.println(
                        "Canonical reporting sheet selected"
                                + " | Month=["
                                + reportingMonthDate
                                + "]"
                                + " | Replacing=["
                                + existing.sheetName()
                                + "]"
                                + " | With=["
                                + candidate.sheetName()
                                + "]"
                );

                selectedSheets.put(
                        reportingMonthDate,
                        candidate
                );

                continue;
            }


            /*
             * Two plain sheets for the same month,
             * or two versioned sheets for the same month.
             *
             * Do not guess.
             */
            throw new IllegalStateException(
                    "Ambiguous duplicate reporting month tabs detected"
                            + " | Month=["
                            + reportingMonthDate
                            + "]"
                            + " | Sheet1=["
                            + existing.sheetName()
                            + "]"
                            + " | Sheet2=["
                            + candidate.sheetName()
                            + "]"
            );
        }


        List<MonthlySheet> monthlySheets =
                new ArrayList<>(
                        selectedSheets.values()
                );


        monthlySheets.sort(
                Comparator.comparing(
                        MonthlySheet::reportingMonthDate
                )
        );


        return monthlySheets.stream()
                .map(MonthlySheet::sheetName)
                .toList();
    }


    private boolean isVersionedSheetName(
            String sheetName) {

        if (sheetName == null) {
            return false;
        }

        return sheetName
                .trim()
                .matches(
                        "(?i).*\\s+Ver\\s*\\d+(?:\\.\\d+)*\\s*$"
                );
    }

    private LocalDate safelyParseReportingMonth(
            String sheetName) {

        if (sheetName == null
                || sheetName.isBlank()) {

            return null;
        }

        String candidate =
                sheetName.trim();

        /*
         * Historical workbook contains versioned month tabs,
         * for example:
         *
         * Feb2025 Ver1.1
         *
         * Remove only a trailing "Ver..." suffix.
         * We do NOT broadly extract dates from arbitrary tab names.
         */
        candidate =
                candidate.replaceFirst(
                        "(?i)\\s+Ver\\s*\\d+(?:\\.\\d+)*\\s*$",
                        ""
                ).trim();

        try {

            LocalDate parsed =
                    ReportingMonthUtil.parse(candidate);

            return parsed == null
                    ? null
                    : parsed.withDayOfMonth(1);

        } catch (Exception ex) {

            return null;
        }
    }

    private List<GoogleSheetRowDto> processSheet(
            String sheetName)
            throws IOException {

        String endColumn = "AA";

        /*
         * Feb2025 is an older sheet and does not extend to column AA.
         * Reading A2:AA causes Google Sheets API INVALID_ARGUMENT.
         */
        String normalizedSheetName =
                sheetName.trim()
                        .replaceFirst(
                                "(?i)\\s+Ver\\s*\\d+(?:\\.\\d+)*\\s*$",
                                ""
                        )
                        .trim();

        if ("Feb2025".equalsIgnoreCase(normalizedSheetName)) {
            endColumn = "Z";
        }
        String sheetRange =
                "'" + sheetName + "'!A2:" + endColumn;
        ValueRange response =
                executeWithRetry(
                        () ->
                                sheets.spreadsheets()
                                        .values()
                                        .get(
                                                spreadsheetId,
                                                sheetRange
                                        )
                                        .execute(),
                        "Read sheet " + sheetName
                );

        List<List<Object>> rows =
                response.getValues();

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> headerRow = rows.get(0);

        Map<String, Integer> columnIndexMap =
                columnIndexMapper.buildColumnIndexMap(
                        headerRow
                );





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

        /*
         * Current project's running period.
         */
        int reportingPeriod = 0;


        boolean preProjectBlockActive = false;

        for (int i = 1; i < rows.size(); i++) {

            List<Object> row = rows.get(i);



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

            if (dto.getProjectName() == null ||
                    dto.getProjectName().isBlank()
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
            String normalizedReportingMonth =
                    sheetName.trim()
                            .replaceFirst(
                                    "(?i)\\s+Ver\\s*\\d+(?:\\.\\d+)*\\s*$",
                                    ""
                            )
                            .trim();


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
                boolean sameAsCurrentProject =
                        explicitProjectName.equals(lastProjectName);

                boolean confirmsPreProjectBlock =
                        preProjectBlockActive
                                && sameAsCurrentProject;


                if (confirmsPreProjectBlock) {

                    /*
                     * The explicit Project Name confirms a project
                     * already resolved from pre-project/context rows.
                     *
                     * Do NOT restart reporting periods.
                     */
                    preProjectBlockActive = false;


                } else if (sameAsCurrentProject) {

                    /*
                     * Historical sheets may repeat the Project Name
                     * on every reporting row instead of using merged
                     * or blank continuation cells.
                     *
                     * Example:
                     *
                     * Aortic QC   Week 1
                     * Aortic QC   Week 2
                     * Aortic QC   Week 3
                     *
                     * This is still the SAME project block.
                     *
                     * Therefore do NOT reset reportingPeriod.
                     */
                    preProjectBlockActive = false;

                    System.out.println(
                            "REPEATED PROJECT NAME -> CONTINUING PERIODS"
                                    + " | Sheet=[" + sheetName + "]"
                                    + " | Row=[" + dto.getSourceRowNumber() + "]"
                                    + " | Project=[" + explicitProjectName + "]"
                                    + " | CurrentPeriod=[" + reportingPeriod + "]"
                    );


                } else {

                    /*
                     * We are switching to another explicit project.
                     *
                     * It may be the project's FIRST appearance
                     * in this month, or it may be returning later
                     * in the same historical sheet.
                     *
                     * Therefore resume its previous positive
                     * reporting period if we have already seen it.
                     */
                    lastProjectName =
                            explicitProjectName;

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
                    lastProjectName =
                            nextProjectName;

                    /*
                     * The current row itself is Period 0 context.
                     *
                     * But if this project already appeared earlier
                     * in the month, retain its last genuine positive
                     * reporting period so subsequent reporting rows
                     * continue correctly.
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


            /*
             * STEP 7
             *
             * Special November debugging for Pilot Coronary CTA.
             */


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


    private String calculateRawSheetHash(
            String sheetName)
            throws IOException {

        /*
         * Read the raw physical sheet.
         *
         * We intentionally do NOT call processSheet().
         *
         * Therefore old reporting-period/parser quirks
         * cannot break historical change detection.
         */
        String escapedSheetName =
                sheetName.replace(
                        "'",
                        "''"
                );

        String sheetRange =
                "'" + escapedSheetName + "'";


        ValueRange response =
                executeWithRetry(
                        () ->
                                sheets.spreadsheets()
                                        .values()
                                        .get(
                                                spreadsheetId,
                                                sheetRange
                                        )
                                        .setValueRenderOption("UNFORMATTED_VALUE")
                                        .setDateTimeRenderOption("SERIAL_NUMBER")
                                        .execute(),
                        "Fingerprint historical sheet "
                                + sheetName
                );


        List<List<Object>> rows =
                response.getValues();


        String canonicalContent =
                buildCanonicalSheetContent(
                        rows
                );


        try {

            MessageDigest digest =
                    MessageDigest.getInstance(
                            "SHA-256"
                    );

            byte[] hash =
                    digest.digest(
                            canonicalContent.getBytes(
                                    StandardCharsets.UTF_8
                            )
                    );


            return HexFormat.of()
                    .formatHex(hash);


        } catch (NoSuchAlgorithmException ex) {

            /*
             * SHA-256 is mandatory in Java,
             * so this would indicate a JVM problem.
             */
            throw new IllegalStateException(
                    "SHA-256 algorithm is unavailable.",
                    ex
            );
        }
    }


    private String buildCanonicalSheetContent(
            List<List<Object>> rows) {

        if (rows == null || rows.isEmpty()) {
            return "ROWS:0";
        }


        StringBuilder builder =
                new StringBuilder();


        /*
         * Length-prefixed serialization prevents
         * ambiguous values such as:
         *
         * ["AB", "C"]
         * ["A", "BC"]
         *
         * from producing the same canonical string.
         */
        builder.append("ROWS:")
                .append(rows.size())
                .append('|');


        for (List<Object> row : rows) {

            if (row == null) {

                builder.append("ROW:null|");
                continue;
            }


            builder.append("COLS:")
                    .append(row.size())
                    .append('|');


            for (Object value : row) {

                String text =
                        value == null
                                ? ""
                                : value.toString();


                builder.append(text.length())
                        .append(':')
                        .append(text)
                        .append('|');
            }


            builder.append("ENDROW|");
        }


        return builder.toString();
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



    private <T> T executeWithRetry(
            GoogleApiCall<T> operation,
            String operationName)
            throws IOException {

        final int maxAttempts = 4;

        long delayMillis = 1000;

        for (int attempt = 1;
             attempt <= maxAttempts;
             attempt++) {

            try {

                return operation.execute();

            } catch (GoogleJsonResponseException ex) {

                int statusCode =
                        ex.getStatusCode();

                boolean retryable =
                        statusCode == 429
                                || statusCode == 500
                                || statusCode == 502
                                || statusCode == 503
                                || statusCode == 504;

                if (!retryable
                        || attempt == maxAttempts) {

                    throw ex;
                }

                System.out.println(
                        "Google Sheets API temporary failure"
                                + " | Operation=[" + operationName + "]"
                                + " | Status=[" + statusCode + "]"
                                + " | Attempt=[" + attempt + "]"
                                + " | Retrying in "
                                + delayMillis
                                + " ms"
                );

                sleepBeforeRetry(delayMillis);

                delayMillis *= 2;

            } catch (IOException ex) {

                if (attempt == maxAttempts) {
                    throw ex;
                }

                System.out.println(
                        "Google Sheets API I/O failure"
                                + " | Operation=[" + operationName + "]"
                                + " | Attempt=[" + attempt + "]"
                                + " | Retrying in "
                                + delayMillis
                                + " ms"
                );

                sleepBeforeRetry(delayMillis);

                delayMillis *= 2;
            }
        }

        throw new IllegalStateException(
                "Google Sheets API retry loop ended unexpectedly."
        );
    }


    private void sleepBeforeRetry(
            long delayMillis)
            throws IOException {

        try {

            Thread.sleep(delayMillis);

        } catch (InterruptedException ex) {

            Thread.currentThread().interrupt();

            throw new IOException(
                    "Google Sheets API retry interrupted.",
                    ex
            );
        }
    }


    @FunctionalInterface
    private interface GoogleApiCall<T> {

        T execute() throws IOException;
    }

    private record MonthlySheet(
            String sheetName,
            LocalDate reportingMonthDate) {
    }


}