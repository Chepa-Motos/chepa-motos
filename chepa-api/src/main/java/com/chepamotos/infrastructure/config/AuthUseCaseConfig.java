package com.chepamotos.infrastructure.config;

import com.chepamotos.domain.port.AccessTokenService;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.PasswordHasher;
import com.chepamotos.domain.port.RefreshTokenRepository;
import com.chepamotos.domain.port.TokenHashService;
import com.chepamotos.domain.usecase.auth.LoginUseCase;
import com.chepamotos.domain.usecase.auth.LogoutUseCase;
import com.chepamotos.domain.usecase.auth.RefreshSessionUseCase;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.Duration;

@Configuration
public class AuthUseCaseConfig {

    @Bean
    public LoginUseCase loginUseCase(AppUserRepository appUserRepository,
                                     RefreshTokenRepository refreshTokenRepository,
                                     PasswordHasher passwordHasher,
                                     TokenHashService tokenHashService,
                                     AccessTokenService accessTokenService,
                                     Clock systemClock,
                                     @Value("${auth.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays) {
        return new LoginUseCase(
                appUserRepository,
                refreshTokenRepository,
                passwordHasher,
                tokenHashService,
                accessTokenService,
                systemClock,
                Duration.ofDays(refreshTokenTtlDays)
        );
    }

    @Bean
    public RefreshSessionUseCase refreshSessionUseCase(AppUserRepository appUserRepository,
                                                       RefreshTokenRepository refreshTokenRepository,
                                                       TokenHashService tokenHashService,
                                                       AccessTokenService accessTokenService,
                                                       Clock systemClock,
                                                       @Value("${auth.jwt.refresh-token-ttl-days:7}") long refreshTokenTtlDays) {
        return new RefreshSessionUseCase(
                appUserRepository,
                refreshTokenRepository,
                tokenHashService,
                accessTokenService,
                systemClock,
                Duration.ofDays(refreshTokenTtlDays)
        );
    }

    @Bean
    public LogoutUseCase logoutUseCase(RefreshTokenRepository refreshTokenRepository,
                                       TokenHashService tokenHashService,
                                       Clock systemClock) {
        return new LogoutUseCase(refreshTokenRepository, tokenHashService, systemClock);
    }
}
