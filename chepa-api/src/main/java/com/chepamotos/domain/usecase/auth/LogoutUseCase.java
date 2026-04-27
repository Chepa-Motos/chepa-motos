package com.chepamotos.domain.usecase.auth;

import com.chepamotos.domain.exception.InvalidRefreshTokenException;
import com.chepamotos.domain.exception.RefreshTokenExpiredException;
import com.chepamotos.domain.port.RefreshTokenRepository;
import com.chepamotos.domain.port.TokenHashService;

import java.time.Clock;
import java.time.LocalDateTime;

public final class LogoutUseCase {

    private final RefreshTokenRepository refreshTokenRepository;
    private final TokenHashService tokenHashService;
    private final Clock clock;

    public LogoutUseCase(RefreshTokenRepository refreshTokenRepository,
                         TokenHashService tokenHashService,
                         Clock clock) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.tokenHashService = tokenHashService;
        this.clock = clock;
    }

    public void execute(String rawRefreshToken) {
        LocalDateTime now = LocalDateTime.now(clock);
        String tokenHash = tokenHashService.hash(rawRefreshToken);

        var storedToken = refreshTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(InvalidRefreshTokenException::new);

        if (storedToken.revokedAt() != null) {
            return;
        }

        if (!storedToken.expiresAt().isAfter(now)) {
            throw new RefreshTokenExpiredException();
        }

        refreshTokenRepository.save(storedToken.revoke(now, null));
    }
}
