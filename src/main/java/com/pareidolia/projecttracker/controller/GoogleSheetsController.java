package com.pareidolia.projecttracker.controller;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.security.RefreshApiKeyValidator;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("/api/google-sheet")
@RequiredArgsConstructor
public class GoogleSheetsController {

    private final GoogleSheetsService googleSheetsService;
    private final RefreshApiKeyValidator refreshApiKeyValidator;


    /*
     * Diagnostic endpoint only.
     *
     * After the sync redesign, readSheet()
     * reads only the latest two reporting tabs.
     */
    @GetMapping("/test")
    public List<GoogleSheetRowDto> testGoogleSheet(
            @RequestHeader("X-Refresh-Key") String refreshKey)
            throws GeneralSecurityException, IOException {

        refreshApiKeyValidator.validate(refreshKey);

        return googleSheetsService.readSheet();
    }
}