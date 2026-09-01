package com.pareidolia.projecttracker.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Collections;

@Configuration
public class GoogleSheetsConfig {

    @Value("${google.credentials.path:credentials/google-service-account.json}")
    private String credentialsPath;

    @Value("${GOOGLE_SERVICE_ACCOUNT_JSON:}")
    private String googleServiceAccountJson;

    @Bean
    public Sheets sheetsService() throws Exception {

        InputStream inputStream;

        // Railway / production
        if (googleServiceAccountJson != null
                && !googleServiceAccountJson.isBlank()) {

            inputStream = new ByteArrayInputStream(
                    googleServiceAccountJson.getBytes(StandardCharsets.UTF_8)
            );

        } else {

            // Local development fallback
            inputStream =
                    new ClassPathResource(credentialsPath).getInputStream();
        }

        GoogleCredentials credentials =
                GoogleCredentials.fromStream(inputStream)
                        .createScoped(
                                Collections.singleton(
                                        SheetsScopes.SPREADSHEETS_READONLY
                                )
                        );

        HttpCredentialsAdapter credentialsAdapter =
                new HttpCredentialsAdapter(credentials);

        HttpRequestInitializer requestInitializer = request -> {

            credentialsAdapter.initialize(request);

            // Maximum time allowed to establish the connection
            request.setConnectTimeout(30_000);

            // Maximum time waiting for Google Sheets to return data
            request.setReadTimeout(120_000);
        };

        return new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                GsonFactory.getDefaultInstance(),
                requestInitializer
        )
                .setApplicationName("Project Tracker Dashboard")
                .build();
    }
}