package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.AuthTokens;

public record AuthTokensResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken
) {
    public static AuthTokensResponse fromDomain(AuthTokens authTokens) {
        return new AuthTokensResponse(
                authTokens.tokenType(),
                authTokens.accessToken(),
                authTokens.accessTokenExpiresInSeconds(),
                authTokens.refreshToken()
        );
    }
}
