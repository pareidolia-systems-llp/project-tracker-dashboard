package com.pareidolia.projecttracker.mapper;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class SheetEnhancementMapperTest {
    private final ColumnIndexMapper columns = new ColumnIndexMapper();
    private final GoogleSheetRowMapper rowMapper = new GoogleSheetRowMapper();
    private final ProjectMapper projectMapper = new ProjectMapper();

    @Test
    void mapsBothHeaderVariantsAndPreservesColumnZAndExistingGroundTruth() {
        for (String header : List.of("Annotations Completed (GT+Rapid Review)",
                "Annotations Completed (GT + Rapid Review)", "Ground Truth", "Ground Truth Cases")) {
            for (String value : List.of("878", "4099", "0", "")) {
                GoogleSheetRowDto row = rowMapper.mapRow(List.of(value, "43", "17"),
                        columns.buildColumnIndexMap(List.of(header,
                                "Pending Annotations + Cases Moved to Labels + Not Assigned Cases",
                                "Cases in Ground Truth")));
                Project project = projectMapper.toEntity(row);
                assertEquals(value.isEmpty() ? 0 : Integer.parseInt(value), project.getAnnotationsCompleted());
                assertEquals(43, project.getPendingAnnotations());
                assertEquals(17, project.getCasesInGroundTruth());
            }
        }
    }

    @Test
    void groundTruthCasesIsDistinctFromCasesInGroundTruth() {
        var headers = columns.buildColumnIndexMap(List.of("  GROUND\nTRUTH   CASES  ", "Cases in Ground Truth"));
        GoogleSheetRowDto row = rowMapper.mapRow(List.of("4099", "23"), headers);
        Project project = projectMapper.toEntity(row);
        assertEquals(4099, project.getAnnotationsCompleted());
        assertEquals(23, project.getCasesInGroundTruth());
    }

    @Test
    void validatesPlainUrlsWithoutChangingQueryOrMissingValues() {
        var headers = columns.buildColumnIndexMap(List.of("Project Instruction"));
        String url = "https://drive.google.com/file/d/example/view?usp=sharing&x=a%2Bb";
        assertEquals(url, rowMapper.mapRow(List.of(url), headers).getProjectInstruction());
        assertEquals("https://docs.google.com/document/d/example/edit?tab=t.0",
                rowMapper.mapRow(List.of("https://docs.google.com/document/d/example/edit?tab=t.0"), headers).getProjectInstruction());
        assertNull(rowMapper.mapRow(List.of(""), headers).getProjectInstruction());
        assertNull(rowMapper.mapRow(List.of(), headers).getProjectInstruction());
        assertNull(rowMapper.mapRow(List.of(), columns.buildColumnIndexMap(List.of())).getProjectInstruction());
        for (String invalid : List.of("http://drive.google.com/file", "https://example.com/",
                "https://drive.google.com.evil.example/x", "https://user@drive.google.com/x",
                "https://drive.google.com:444/x", "not a url", "https://drive.google.com/" + "a".repeat(2048))) {
            assertNull(rowMapper.mapRow(List.of(invalid), headers).getProjectInstruction());
        }
    }

    @Test
    void invalidUrlsLogOnlyAFixedSanitizedWarning() {
        Logger logger = (Logger) LoggerFactory.getLogger(GoogleSheetRowMapper.class);
        ListAppender<ILoggingEvent> appender = new ListAppender<>();
        appender.start();
        logger.addAppender(appender);
        try {
            var headers = columns.buildColumnIndexMap(List.of("Project Instruction"));
            for (String invalid : List.of("https://drive.google.com/?token=secret invalid",
                    "https://user:secret@drive.google.com/file?token=private",
                    "https://example.com/?token=private", "https://drive.google.com/" + "x".repeat(2048))) {
                assertNull(rowMapper.mapRow(List.of(invalid), headers).getProjectInstruction());
            }
            assertNull(rowMapper.mapRow(List.of(""), headers).getProjectInstruction());
            assertEquals(4, appender.list.size());
            for (ILoggingEvent event : appender.list) {
                assertEquals(Level.WARN, event.getLevel());
                assertEquals("Project Instruction ignored: invalid, unsupported, or overlength URL.",
                        event.getFormattedMessage());
                assertNull(event.getThrowableProxy());
            }
        } finally {
            logger.detachAppender(appender);
            appender.stop();
        }
    }

    @Test
    void mapsAndUpdatesInstructionIndependently() {
        GoogleSheetRowDto row = new GoogleSheetRowDto();
        row.setAnnotationsCompleted("878");
        row.setProjectInstruction("https://drive.google.com/file/d/one/view?x=1&y=2");
        Project project = projectMapper.toEntity(row);
        assertEquals(878, project.getAnnotationsCompleted());
        assertEquals(row.getProjectInstruction(), project.getProjectInstruction());
        row.setProjectInstruction(null);
        projectMapper.updateEntity(project, row);
        assertNull(project.getProjectInstruction());
        assertEquals(878, project.getAnnotationsCompleted());
    }

    @Test
    void missingAnnotationHeaderKeepsExistingZeroFallback() {
        GoogleSheetRowDto row = rowMapper.mapRow(List.of("some value"),
                columns.buildColumnIndexMap(List.of("Unrelated")));
        assertEquals("", row.getAnnotationsCompleted());
        assertEquals(0, projectMapper.toEntity(row).getAnnotationsCompleted());
    }
}
