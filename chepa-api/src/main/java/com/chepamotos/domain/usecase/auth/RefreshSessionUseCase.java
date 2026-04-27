package com.chepamotos.domain.usecase.auth;

import com.chepamotos.domain.exception.InvalidRefreshTokenException;
import com.chepamotos.domain.exception.RefreshTokenExpiredException;
import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.model.AuthTokens;
import com.chepamotos.domain.model.RefreshToken;
import com.chepamotos.domain.port.AccessTokenService;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.RefreshTokenRepository;
import com.chepamotos.domain.port.TokenHashService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public final class RefreshSessionUseCase {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final AccessTokenService accessTokenService;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public RefreshSessionUseCase(AppUserRepository appUserRepository,
                                 RefreshTokenRepository refreshTokenRepository,
                                 TokenHashService tokenHashService,
                                 AccessTokenService accessTokenService,
                                 Clock clock,
                                 Duration refreshTokenTtl) {
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.accessTokenService = accessTokenService;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public AuthTokens execute(String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        String tokenHash = tokenHashService.hash(rawRefreshToken);

        RefreshToken storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (storedToken.revokedAt() != null) {
            throw new InvalidRefreshTokenException();
        }

        if (!storedToken.expiresAt().isAfter(now)) {
            throw new RefreshTokenExpiredException();
        }

        AppUser user = appUserRepository.findById(storedToken.userId())
                .filter(AppUser::active)
                .orElseThrow(InvalidRefreshTokenException::new);

        refreshTokenRepository.save(storedToken.revoke(now, null));

        String newRawRefreshToken = UUID.randomUUID().toString();
        String newRefreshTokenHash = tokenHashService.hash(newRawRefreshToken);
        LocalDateTime issuedAt = now;
        LocalDateTime expiresAt = issuedAt.plus(refreshTokenTtl);
        refreshTokenRepository.save(RefreshToken.createNew(user.id(), newRefreshTokenHash, issuedAt, expiresAt));

        AccessToken accessToken = accessTokenService.generate(user);
        return AuthTokens.of(accessToken, newRawRefreshToken);
    }
}
