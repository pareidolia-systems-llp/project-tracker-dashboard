package com.pareidolia.projecttracker.mapper;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.entity.Project;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProjectMapperTest {

    private final ProjectMapper projectMapper = new ProjectMapper();

    @Test
    void shouldMapBlankNumericValueToZero() {

        GoogleSheetRowDto dto = new GoogleSheetRowDto();
        dto.setAssignedToPareidolia("");

        Project project = projectMapper.toEntity(dto);

        assertEquals(0, project.getAssignedToPareidolia());
    }

    @Test
    void shouldMapNullNumericValueToZero() {

        GoogleSheetRowDto dto = new GoogleSheetRowDto();
        dto.setAnnotationsSubmittedPareidolia(null);

        Project project = projectMapper.toEntity(dto);

        assertEquals(0, project.getAnnotationsSubmittedPareidolia());
    }

    @Test
    void shouldMapCommaFormattedNumericValue() {

        GoogleSheetRowDto dto = new GoogleSheetRowDto();
        dto.setIssueCases("1,234");

        Project project = projectMapper.toEntity(dto);

        assertEquals(1234, project.getIssueCases());
    }

    @Test
    void shouldMapInvalidNumericValueToExistingFallbackOfZero() {

        GoogleSheetRowDto dto = new GoogleSheetRowDto();
        dto.setCasesMovedToLabels("not a number");

        Project project = projectMapper.toEntity(dto);

        assertEquals(0, project.getCasesMovedToLabels());
    }
}
