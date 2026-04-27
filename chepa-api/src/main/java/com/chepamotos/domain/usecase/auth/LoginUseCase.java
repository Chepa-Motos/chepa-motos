package com.chepamotos.domain.usecase.auth;

import com.chepamotos.domain.exception.InvalidCredentialsException;
import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.model.AuthTokens;
import com.chepamotos.domain.model.RefreshToken;
import com.chepamotos.domain.port.AccessTokenService;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.PasswordHasher;
import com.chepamotos.domain.port.RefreshTokenRepository;
import com.chepamotos.domain.port.TokenHashService;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

public final class LoginUseCase {

    private final AppUserRepository appUserRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final TokenHashService tokenHashService;
    private final AccessTokenService accessTokenService;
    private final Clock clock;
    private final Duration refreshTokenTtl;

    public LoginUseCase(AppUserRepository appUserRepository,
                        RefreshTokenRepository refreshTokenRepository,
                        PasswordHasher passwordHasher,
                        TokenHashService tokenHashService,
                        AccessTokenService accessTokenService,
                        Clock clock,
                        Duration refreshTokenTtl) {
        this.appUserRepository = appUserRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.tokenHashService = tokenHashService;
        this.accessTokenService = accessTokenService;
        this.clock = clock;
        this.refreshTokenTtl = refreshTokenTtl;
    }

    public AuthTokens execute(String username, String password) {
        AppUser user = appUserRepository.findByUsername(username)
                .filter(AppUser::active)
                .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(password, user.passwordHash())) {
            throw new InvalidCredentialsException();
        }

        String rawRefreshToken = UUID.randomUUID().toString();
        String refreshTokenHash = tokenHashService.hash(rawRefreshToken);
        LocalDateTime issuedAt = LocalDateTime.now(clock);
        LocalDateTime expiresAt = issuedAt.plus(refreshTokenTtl);

        refreshTokenRepository.save(RefreshToken.createNew(user.id(), refreshTokenHash, issuedAt, expiresAt));

        AccessToken accessToken = accessTokenService.generate(user);
        return AuthTokens.of(accessToken, rawRefreshToken);
    }
}
