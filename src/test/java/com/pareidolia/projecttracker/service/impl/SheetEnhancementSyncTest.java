package com.pareidolia.projecttracker.service.impl;

import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import com.pareidolia.projecttracker.mapper.ColumnIndexMapper;
import com.pareidolia.projecttracker.mapper.GoogleSheetRowMapper;
import com.pareidolia.projecttracker.mapper.ProjectMapper;
import com.pareidolia.projecttracker.repository.ProjectRepository;
import com.pareidolia.projecttracker.repository.SheetSyncStateRepository;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiPopulationService;
import com.pareidolia.projecttracker.service.dashboard.DashboardKpiService;
import com.pareidolia.projecttracker.service.dashboard.HistoryService;
import com.pareidolia.projecttracker.service.dashboard.MonthlyProjectSummaryService;
import com.pareidolia.projecttracker.validation.ProjectSyncValidator;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class SheetEnhancementSyncTest {
    private static final String OLD = "Annotations Completed (GT+Rapid Review)";
    private static final String URL = "https://drive.google.com/file/d/example/view?usp=sharing&x=1";

    private List<Object> row() {
        return new ArrayList<>(Collections.nCopies(28, ""));
    }

    private List<Object> header(String alias, int instructionColumn) {
        List<Object> header = row();
        header.set(1, "Project Name");
        header.set(2, "Project Type");
        header.set(3, "Priority");
        header.set(4, "Vendor");
        header.set(5, "Week Of");
        header.set(24, alias);
        header.set(25, "Pending Annotations + Cases Moved to Labels + Not Assigned Cases");
        header.set(26, "Notes");
        if (instructionColumn >= 0) header.set(instructionColumn, "Project Instruction");
        return header;
    }

    private List<List<Object>> fixture(String alias, int instructionColumn) {
        List<Object> first = row();
        first.set(1, "Project A");
        first.set(5, "01-07");
        first.set(25, "42");
        if (instructionColumn >= 0) first.set(instructionColumn, URL);
        List<Object> continuation = row();
        continuation.set(5, "08-14");
        continuation.set(24, "878");
        return List.of(List.of(), header(alias, instructionColumn), first, continuation);
    }

    @SuppressWarnings("unchecked")
    private List<GoogleSheetRowDto> parse(String sheetName, List<List<Object>> fixture) throws Exception {
        Sheets sheets = mock(Sheets.class, RETURNS_DEEP_STUBS);
        String range = "'" + sheetName + "'" + (sheetName.equals("Feb2025") ? "!A2:Z" : "");
        when(sheets.spreadsheets().values().get("fixture", range).execute())
                .thenReturn(new ValueRange().setValues(fixture));
        GoogleSheetsServiceImpl service = new GoogleSheetsServiceImpl(sheets, new GoogleSheetRowMapper(), new ColumnIndexMapper());
        ReflectionTestUtils.setField(service, "spreadsheetId", "fixture");
        return ReflectionTestUtils.invokeMethod(service, "processSheet", sheetName);
    }

    @Test
    void oldAndNewHeadersKeepContinuationPeriodsAndAcceptedPtd() throws Exception {
        List<GoogleSheetRowDto> oldRows = parse("Jan2026", fixture(OLD, 27));
        List<GoogleSheetRowDto> newRows = parse("Jan2026", fixture("Ground Truth", 27));
        assertEquals(2, oldRows.size());
        assertEquals(2, newRows.size());
        for (List<GoogleSheetRowDto> rows : List.of(oldRows, newRows)) {
            assertEquals("Project A", rows.get(1).getProjectName());
            assertEquals(1, rows.get(0).getReportingPeriod());
            assertEquals(2, rows.get(1).getReportingPeriod());
            assertEquals("42", rows.get(0).getPendingAnnotations());
            assertEquals(URL, rows.get(0).getProjectInstruction());
            assertNull(rows.get(1).getProjectInstruction());
            List<Project> projects = rows.stream().map(new ProjectMapper()::toEntity).toList();
            HistoryService history = mock(HistoryService.class);
            when(history.getWeeklyHistory("Project A")).thenReturn(projects);
            var calculation = new DashboardCalculationServiceImpl(history, mock(DashboardKpiService.class),
                    mock(MonthlyProjectSummaryService.class));
            assertEquals(878, calculation.calculateProjectPtd("Project A", "Jan2026", "08-14").getAcceptedPtd());
        }
    }

    @Test
    void acceptsInstructionWithinRangeAndIgnoresInstructionOnlySpacer() throws Exception {
        for (int instructionColumn : List.of(20, 27)) {
            List<List<Object>> original = fixture("Ground Truth", instructionColumn);
            List<Object> instructionOnly = row();
            instructionOnly.set(instructionColumn, URL);
            List<List<Object>> withSpacer = new ArrayList<>(original);
            withSpacer.add(3, instructionOnly);
            List<GoogleSheetRowDto> rows = parse("Jan2026", withSpacer);
            assertEquals(2, rows.size());
            assertEquals(2, rows.get(1).getReportingPeriod());
            assertEquals(URL, rows.get(0).getProjectInstruction());
        }
    }

    @Test
    void instructionOnlySpacerDoesNotChangePreProjectContextOwnership() throws Exception {
        for (int instructionColumn : List.of(20, 27)) {
            List<List<Object>> data = new ArrayList<>(fixture("Ground Truth", instructionColumn));
            data.get(1).set(10, "Issue Cases");
            List<Object> instructionOnly = row();
            instructionOnly.set(instructionColumn, URL);
            List<Object> context = row();
            context.set(10, "35");
            List<Object> projectB = row();
            projectB.set(1, "Project B");
            projectB.set(5, "01-07");
            data.add(instructionOnly);
            data.add(context);
            data.add(projectB);
            List<GoogleSheetRowDto> rows = parse("Jan2026", data);
            assertEquals(4, rows.size());
            assertEquals("Project B", rows.get(2).getProjectName());
            assertEquals(0, rows.get(2).getReportingPeriod());
            assertEquals(1, rows.get(3).getReportingPeriod());
        }
    }

    @Test
    void zeroAndBlankRetainAcceptedPtdBehaviorForBothAliases() throws Exception {
        for (String alias : List.of(OLD, "Ground Truth")) {
            for (String value : List.of("0", "")) {
                List<List<Object>> data = fixture(alias, -1);
                data.get(3).set(24, value);
                List<Project> projects = parse("Jan2026", data).stream().map(new ProjectMapper()::toEntity).toList();
                HistoryService history = mock(HistoryService.class);
                when(history.getWeeklyHistory("Project A")).thenReturn(projects);
                var calculation = new DashboardCalculationServiceImpl(history, mock(DashboardKpiService.class),
                        mock(MonthlyProjectSummaryService.class));
                assertEquals(0, calculation.calculateProjectPtd("Project A", "Jan2026", "08-14").getAcceptedPtd());
            }
        }
    }

    @Test
    void oldTabsWithoutInstructionAndFeb2025KeepOriginalRange() throws Exception {
        List<List<Object>> old = fixture(OLD, -1);
        assertNull(parse("Jan2026", old).get(0).getProjectInstruction());
        List<List<Object>> feb = new ArrayList<>();
        for (List<Object> entry : old.subList(1, old.size())) {
            feb.add(new ArrayList<>(entry.subList(0, 26)));
        }
        assertNull(parse("Feb2025", feb).get(0).getProjectInstruction());
    }

    @Test
    void sparseRowsAndUnrelatedColumnsPreserveAlignmentAndProjectPeriods() throws Exception {
        List<Object> header = new ArrayList<>(header("Ground Truth", 27));
        header.add("Unrelated hidden header");
        List<Object> first = row();
        first.set(1, "Project A");
        first.set(5, "01-07");
        first.set(27, URL);
        first.add("unrelated value");
        List<Object> continuation = row();
        continuation.set(5, "08-14");
        continuation.set(24, "878");
        List<Object> hiddenOnly = new ArrayList<>(Collections.nCopies(29, ""));
        hiddenOnly.set(28, "unrelated value");
        List<Object> nextProject = List.of("", "Project B", "", "", "", "15-21");
        List<List<Object>> data = List.of(List.of(), header, List.of(), first, List.of(),
                new ArrayList<>(continuation.subList(0, 25)), hiddenOnly, nextProject, hiddenOnly);

        List<GoogleSheetRowDto> rows = parse("Jan2026", data);
        assertEquals(3, rows.size());
        assertEquals(List.of("Project A", "Project A", "Project B"),
                rows.stream().map(GoogleSheetRowDto::getProjectName).toList());
        assertEquals(List.of(1, 2, 1), rows.stream().map(GoogleSheetRowDto::getReportingPeriod).toList());
        assertEquals(List.of(4, 6, 8), rows.stream().map(GoogleSheetRowDto::getSourceRowNumber).toList());
        assertEquals(URL, rows.get(0).getProjectInstruction());
        assertEquals("878", rows.get(1).getAnnotationsCompleted());
        assertNull(rows.get(1).getProjectInstruction());
        assertNull(rows.get(2).getProjectInstruction());
    }

    @Test
    void rejectsConflictingAndDuplicateNormalizedHeaders() throws Exception {
        for (String duplicate : List.of("Ground Truth", "  ANNOTATIONS   COMPLETED (GT+RAPID REVIEW)  ")) {
            List<List<Object>> data = new ArrayList<>(fixture(OLD, -1));
            data.get(1).set(27, duplicate);
            assertThrows(IllegalStateException.class, () -> parse("Jan2026", data));
        }
        List<List<Object>> duplicates = new ArrayList<>(fixture("Ground Truth", -1));
        duplicates.get(1).set(27, " ground   truth ");
        assertThrows(IllegalStateException.class, () -> parse("Jan2026", duplicates));
    }

    @Test
    void duplicateInstructionHeadersStopBeforeSnapshotReplacement() throws Exception {
        for (int duplicateColumn : List.of(20, 28)) {
            List<List<Object>> data = new ArrayList<>(fixture("Ground Truth", 27));
            if (duplicateColumn == 28) data.get(1).add("");
            data.get(1).set(duplicateColumn, "  PROJECT   INSTRUCTION  ");
            GoogleSheetsService sheetService = mock(GoogleSheetsService.class);
            ProjectRepository repository = mock(ProjectRepository.class);
            when(sheetService.readSheet()).thenAnswer(ignored -> parse("Jan2026", data));
            ProjectServiceImpl service = new ProjectServiceImpl(sheetService, repository, new ProjectMapper(),
                    mock(SheetSyncStateRepository.class), mock(ProjectSyncValidator.class),
                    mock(DashboardKpiPopulationService.class));
            IllegalStateException error = assertThrows(IllegalStateException.class, service::synchronizeProjects);
            assertTrue(error.getMessage().contains("Duplicate Project Instruction headers"));
            verifyNoInteractions(repository);
        }
    }

    @Test
    void invalidContinuationInstructionDoesNotPreventSnapshotPersistence() throws Exception {
        List<List<Object>> data = fixture("Ground Truth", 27);
        data.get(3).set(27, "https://example.com/?token=private");
        List<GoogleSheetRowDto> rows = parse("Jan2026", data);
        assertEquals(2, rows.size());
        assertEquals("Project A", rows.get(1).getProjectName());
        assertEquals(2, rows.get(1).getReportingPeriod());
        assertEquals("878", rows.get(1).getAnnotationsCompleted());
        assertNull(rows.get(1).getProjectInstruction());

        GoogleSheetsService sheetService = mock(GoogleSheetsService.class);
        ProjectRepository repository = mock(ProjectRepository.class);
        when(sheetService.readSheet()).thenReturn(rows);
        ProjectServiceImpl service = new ProjectServiceImpl(sheetService, repository, new ProjectMapper(),
                mock(SheetSyncStateRepository.class), new ProjectSyncValidator(),
                mock(DashboardKpiPopulationService.class));
        service.synchronizeProjects();
        verify(repository).deleteByReportingMonthDateIn(anyCollection());
        verify(repository).saveAll(argThat(projects -> {
            List<Project> saved = new ArrayList<>();
            projects.forEach(saved::add);
            return saved.size() == 2 && URL.equals(saved.get(0).getProjectInstruction())
                    && saved.get(1).getProjectInstruction() == null
                    && saved.get(1).getAnnotationsCompleted() == 878
                    && saved.get(1).getReportingPeriod() == 2;
        }));
    }

    @Test
    void snapshotSavesMappedInstructionAndExistingAnnotationValue() throws Exception {
        GoogleSheetsService sheetService = mock(GoogleSheetsService.class);
        ProjectRepository repository = mock(ProjectRepository.class);
        List<GoogleSheetRowDto> rows = parse("Jan2026", fixture("Ground Truth", 27));
        when(sheetService.readSheet()).thenReturn(rows);
        ProjectServiceImpl service = new ProjectServiceImpl(sheetService, repository, new ProjectMapper(),
                mock(SheetSyncStateRepository.class), mock(ProjectSyncValidator.class),
                mock(DashboardKpiPopulationService.class));
        service.synchronizeProjects();
        verify(repository).saveAll(argThat(projects -> {
            List<Project> saved = new ArrayList<>();
            projects.forEach(saved::add);
            return saved.size() == 2 && URL.equals(saved.get(0).getProjectInstruction())
                    && saved.get(1).getAnnotationsCompleted() == 878;
        }));
    }
}
