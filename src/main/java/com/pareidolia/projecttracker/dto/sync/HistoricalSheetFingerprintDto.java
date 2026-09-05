package com.pareidolia.projecttracker.dto.sync;

import java.time.LocalDate;

public record HistoricalSheetFingerprintDto(
        String sheetName,
        LocalDate reportingMonthDate,
        String contentHash) {
}