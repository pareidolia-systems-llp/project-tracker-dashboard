package com.pareidolia.projecttracker.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@Component
public class RefreshApiKeyValidator {

    @Value("${refresh.api.key}")
    private String expectedKey;

    public void validate(String suppliedKey) {

        if (suppliedKey == null
                || suppliedKey.isBlank()
                || expectedKey == null
                || expectedKey.isBlank()) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh authorization failed."
            );
        }

        boolean matches =
                MessageDigest.isEqual(
                        expectedKey.getBytes(StandardCharsets.UTF_8),
                        suppliedKey.getBytes(StandardCharsets.UTF_8)
                );

        if (!matches) {

            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "Refresh authorization failed."
            );
        }
    }
}