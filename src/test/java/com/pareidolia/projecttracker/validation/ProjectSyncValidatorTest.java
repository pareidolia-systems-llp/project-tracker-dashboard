package com.pareidolia.projecttracker.validation;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ProjectSyncValidatorTest {

    private final ProjectSyncValidator validator = new ProjectSyncValidator();

    @Test
    void shouldPermitReportingPeriodsZeroThroughFive() {

        List<GoogleSheetRowDto> rows = List.of(
                row(0, ""),
                row(1, "01-07"),
                row(2, "08-14"),
                row(3, "15-21"),
                row(4, "22-28"),
                row(5, "29-31")
        );

        assertDoesNotThrow(() -> validator.validate(rows));
    }

    @Test
    void shouldRejectReportingPeriodSix() {

        assertThrows(
                IllegalStateException.class,
                () -> validator.validate(List.of(row(6, "29-31")))
        );
    }

    @Test
    void shouldAllowPositivePeriodsWhenPeriodZeroIsAbsent() {

        assertDoesNotThrow(() -> validator.validate(List.of(
                row(1, "01-07"),
                row(2, "08-14")
        )));
    }

    @Test
    void shouldRejectMissingPositiveReportingPeriod() {

        assertThrows(
                IllegalStateException.class,
                () -> validator.validate(List.of(
                        row(0, ""),
                        row(1, "01-07"),
                        row(3, "15-21")
                ))
        );
    }

    @Test
    void shouldNotTreatBlankWeekOfAsPeriodZero() {

        assertDoesNotThrow(() -> validator.validate(List.of(
                row(1, "")
        )));
    }

    private GoogleSheetRowDto row(Integer reportingPeriod, String weekOf) {

        GoogleSheetRowDto dto = new GoogleSheetRowDto();
        dto.setProjectName("Validator Project");
        dto.setReportingMonth("Jan 2026");
        dto.setReportingMonthDate(LocalDate.of(2026, 1, 1));
        dto.setReportingPeriod(reportingPeriod);
        dto.setWeekOf(weekOf);
        dto.setSourceRowNumber(reportingPeriod + 2);
        return dto;
    }
}
