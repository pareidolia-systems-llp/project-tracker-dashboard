package com.pareidolia.projecttracker.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.InputStream;
import java.util.Collections;

@Configuration
public class GoogleSheetsConfig {

    @Value("${google.credentials.path}")
    private String credentialsPath;

    @Bean
    public Sheets sheetsService() throws Exception {

        InputStream inputStream =
                new ClassPathResource(credentialsPath).getInputStream();

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(inputStream)
                        .createScoped(Collections.singleton(SheetsScopes.SPREADSHEETS_READONLY));

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                new HttpCredentialsAdapter(credentials))
                .setApplicationName("Project Tracker Dashboard")
                .build();
    }
}