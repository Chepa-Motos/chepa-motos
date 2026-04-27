package com.chepamotos.infrastructure.security;

import com.chepamotos.domain.port.TokenHashService;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Component
public class Sha256TokenHashService implements TokenHashService {

    @Override
    public String hash(String token) {
        if (token == null || token.trim().isEmpty()) {
            throw new IllegalArgumentException("Refresh token is required");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(token.trim().getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte b : hashed) {
                result.append(String.format("%02x", b));
            }
            return result.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm is not available", e);
        }
    }
}
