package com.pareidolia.projecttracker.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.mapper.ColumnIndexMapper;
import com.pareidolia.projecttracker.mapper.GoogleSheetRowMapper;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import static com.pareidolia.projecttracker.constants.SheetColumns.*;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.Sheet;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

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
                                "' is missing from the Google Sheet.");
            }
        }
    }

    private List<String> getMonthlySheetNames() throws IOException {

        Spreadsheet spreadsheet = sheets.spreadsheets()
                .get(spreadsheetId)
                .execute();

        List<String> monthlySheets = new ArrayList<>();

        for (Sheet sheet : spreadsheet.getSheets()) {

            String sheetName = sheet.getProperties().getTitle();


            System.out.println("Found Sheet : " + sheetName);


            // Ignore non-reporting sheets
            if (!sheetName.matches(".*\\d{4}$")) {
                continue;
            }

            monthlySheets.add(sheetName);
        }

        return monthlySheets;
    }

    private List<GoogleSheetRowDto> processSheet(String sheetName)
            throws IOException {

//        String sheetRange = "'" + sheetName + "'!A:Z";
        String sheetRange = "'" + sheetName + "'!A2:Z";

        ValueRange response = sheets.spreadsheets()
                .values()
                .get(spreadsheetId, sheetRange)
                .execute();

        List<List<Object>> rows = response.getValues();

        if (rows == null || rows.isEmpty()) {
            return Collections.emptyList();
        }

        List<Object> headerRow = rows.get(0);

        System.out.println("--------------------------------");
        System.out.println("Sheet : " + sheetName);
        System.out.println("Header Row : " + headerRow);
        System.out.println("--------------------------------");


        Map<String, Integer> columnIndexMap =
                columnIndexMapper.buildColumnIndexMap(headerRow);

        System.out.println("Headers:");
        columnIndexMap.keySet().forEach(System.out::println);



        validateHeaders(columnIndexMap);

        List<GoogleSheetRowDto> dtoList = new ArrayList<>();

        String lastProjectName = null;
        String lastProjectType = null;
        String lastPriority = null;
        String lastTotalDataRequestedByAml = null;

        for (int i = 1; i < rows.size(); i++) {

            List<Object> row = rows.get(i);

            if (row.isEmpty()) {
                continue;
            }

            GoogleSheetRowDto dto =
                    mapper.mapRow(row, columnIndexMap);

            // Set reporting month from sheet name
            dto.setReportingMonth(sheetName);

            // Carry forward Project Name
            if (dto.getProjectName() != null &&
                    !dto.getProjectName().isBlank()) {

                lastProjectName = dto.getProjectName();

            } else {

                dto.setProjectName(lastProjectName);
            }

            // Carry forward Project Type
            if (dto.getProjectType() != null &&
                    !dto.getProjectType().isBlank()) {

                lastProjectType = dto.getProjectType();

            } else {

                dto.setProjectType(lastProjectType);
            }

            // Carry forward Priority
            if (dto.getPriority() != null &&
                    !dto.getPriority().isBlank()) {

                lastPriority = dto.getPriority();

            } else {

                dto.setPriority(lastPriority);
            }

            // Carry forward Total Data Requested By AML
            if (dto.getTotalDataRequestedByAml() != null &&
                    !dto.getTotalDataRequestedByAml().isBlank()) {

                lastTotalDataRequestedByAml =
                        dto.getTotalDataRequestedByAml();

            } else {

                dto.setTotalDataRequestedByAml(
                        lastTotalDataRequestedByAml);
            }

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
}