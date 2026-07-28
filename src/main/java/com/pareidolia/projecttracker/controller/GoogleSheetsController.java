package com.pareidolia.projecttracker.controller;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;
import com.pareidolia.projecttracker.service.GoogleSheetsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

@RestController
@RequestMapping("/api/google-sheet")
public class GoogleSheetsController {

    private final GoogleSheetsService googleSheetsService;

    public GoogleSheetsController(GoogleSheetsService googleSheetsService) {
        this.googleSheetsService = googleSheetsService;
    }

    @GetMapping("/test")
    public List<GoogleSheetRowDto> testGoogleSheet()
            throws GeneralSecurityException, IOException {

        return googleSheetsService.readSheet();
    }
}