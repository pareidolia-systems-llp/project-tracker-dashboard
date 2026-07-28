package com.pareidolia.projecttracker.service;

import com.pareidolia.projecttracker.dto.google.GoogleSheetRowDto;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.List;

public interface GoogleSheetsService {

    List<GoogleSheetRowDto> readSheet() throws GeneralSecurityException, IOException;

}