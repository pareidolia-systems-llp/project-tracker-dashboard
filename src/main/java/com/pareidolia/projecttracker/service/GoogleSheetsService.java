package com.pareidolia.projecttracker.service;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.dto.sync.HistoricalSheetFingerprintDto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface GoogleSheetsService {

    /*
     * Routine synchronization:
     * parse only the latest two monthly tabs.
     */
    List<GoogleSheetRowDto> readSheet()
            throws GeneralSecurityException, IOException;


    /*
     * Historical audit:
     * read raw historical sheet contents and
     * calculate fingerprints WITHOUT parsing
     * Project/reporting-period business logic.
     */
    List<HistoricalSheetFingerprintDto>
    readHistoricalFingerprints()
            throws GeneralSecurityException, IOException;
}