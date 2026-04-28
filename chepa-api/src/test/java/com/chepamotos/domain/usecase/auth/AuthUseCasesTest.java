package com.chepamotos.domain.usecase.auth;

import com.chepamotos.domain.exception.InvalidCredentialsException;
import com.chepamotos.domain.exception.InvalidRefreshTokenException;
import com.chepamotos.domain.exception.RefreshTokenExpiredException;
import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.model.AuthTokens;
import com.chepamotos.domain.model.RefreshToken;
import com.chepamotos.domain.model.UserRole;
import com.chepamotos.domain.port.AccessTokenService;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.PasswordHasher;
import com.chepamotos.domain.port.RefreshTokenRepository;
import com.chepamotos.domain.port.TokenHashService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthUseCasesTest {

    @Mock
    private AppUserRepository appUserRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordHasher passwordHasher;

    @Mock
    private TokenHashService tokenHashService;

    @Mock
    private AccessTokenService accessTokenService;

    @Test
    void loginUseCase_successfulLogin_persistsRefreshTokenAndReturnsTokens() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
        AppUser user = AppUser.restore(7L, "admin", "stored-hash", UserRole.GERENTE, true,
                LocalDateTime.of(2026, 1, 10, 8, 30));
        AccessToken accessToken = new AccessToken("access.jwt.token", 900);

        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.of(user));
        when(passwordHasher.matches("secret", "stored-hash")).thenReturn(true);
        when(tokenHashService.hash(any(String.class))).thenReturn("hashed-refresh-token");
        when(accessTokenService.generate(user)).thenReturn(accessToken);

        LoginUseCase useCase = new LoginUseCase(
                appUserRepository,
                refreshTokenRepository,
                passwordHasher,
                tokenHashService,
                accessTokenService,
                clock,
                Duration.ofDays(7)
        );

        AuthTokens result = useCase.execute("admin", "secret");

        assertEquals("Bearer", result.tokenType());
        assertEquals("access.jwt.token", result.accessToken());
        assertEquals(900, result.accessTokenExpiresInSeconds());
        assertNotNull(result.refreshToken());
        assertTrue(!result.refreshToken().isBlank());

        ArgumentCaptor<RefreshToken> refreshTokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(refreshTokenCaptor.capture());
        RefreshToken savedToken = refreshTokenCaptor.getValue();
        assertEquals(7L, savedToken.userId());
        assertEquals("hashed-refresh-token", savedToken.tokenHash());
        assertEquals(LocalDateTime.of(2026, 1, 28, 10, 0), savedToken.issuedAt());
        assertEquals(LocalDateTime.of(2026, 2, 4, 10, 0), savedToken.expiresAt());
        verify(accessTokenService).generate(user);
    }

    @Test
    void loginUseCase_whenUserMissingOrInactive_throwsInvalidCredentials() {
        when(appUserRepository.findByUsername("admin")).thenReturn(Optional.empty());

        LoginUseCase useCase = new LoginUseCase(
                appUserRepository,
                refreshTokenRepository,
                passwordHasher,
                tokenHashService,
                accessTokenService,
                Clock.systemUTC(),
                Duration.ofDays(7)
        );

        assertThrows(InvalidCredentialsException.class, () -> useCase.execute("admin", "secret"));

        verify(passwordHasher, never()).matches(any(String.class), any(String.class));
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
        verify(accessTokenService, never()).generate(any(AppUser.class));
    }

    @Test
    void refreshSessionUseCase_successfulRefresh_revokesOldTokenAndIssuesNewOne() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
        AppUser user = AppUser.restore(7L, "admin", "stored-hash", UserRole.GERENTE, true,
                LocalDateTime.of(2026, 1, 10, 8, 30));
        RefreshToken storedToken = RefreshToken.restore(
                11L,
                7L,
                "hashed-refresh-token",
                LocalDateTime.of(2026, 1, 20, 10, 0),
                LocalDateTime.of(2026, 2, 4, 10, 0),
                null,
                null
        );
        AccessToken accessToken = new AccessToken("new.access.token", 1200);

        when(tokenHashService.hash(any(String.class))).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(storedToken));
        when(appUserRepository.findById(7L)).thenReturn(Optional.of(user));
        when(accessTokenService.generate(user)).thenReturn(accessToken);

        RefreshSessionUseCase useCase = new RefreshSessionUseCase(
                appUserRepository,
                refreshTokenRepository,
                tokenHashService,
                accessTokenService,
                clock,
                Duration.ofDays(7)
        );

        AuthTokens result = useCase.execute("raw-refresh-token");

        assertEquals("Bearer", result.tokenType());
        assertEquals("new.access.token", result.accessToken());
        assertEquals(1200, result.accessTokenExpiresInSeconds());
        assertNotNull(result.refreshToken());

                ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
                verify(refreshTokenRepository, times(2)).save(tokenCaptor.capture());
                RefreshToken revokedToken = tokenCaptor.getAllValues().get(0);
                RefreshToken issuedToken = tokenCaptor.getAllValues().get(1);
        assertEquals(11L, revokedToken.id());
        assertNotNull(revokedToken.revokedAt());
                assertEquals(null, issuedToken.id());
                assertEquals(null, issuedToken.revokedAt());
        verify(accessTokenService).generate(user);
    }

    @Test
    void refreshSessionUseCase_whenTokenMissing_throwsInvalidRefreshToken() {
        when(tokenHashService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.empty());

        RefreshSessionUseCase useCase = new RefreshSessionUseCase(
                appUserRepository,
                refreshTokenRepository,
                tokenHashService,
                accessTokenService,
                Clock.systemUTC(),
                Duration.ofDays(7)
        );

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.execute("raw-refresh-token"));

        verify(appUserRepository, never()).findById(any());
        verify(accessTokenService, never()).generate(any(AppUser.class));
    }

    @Test
    void refreshSessionUseCase_whenExpired_throwsRefreshTokenExpired() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
        RefreshToken storedToken = RefreshToken.restore(
                11L,
                7L,
                "hashed-refresh-token",
                LocalDateTime.of(2026, 1, 20, 10, 0),
                LocalDateTime.of(2026, 1, 28, 9, 59),
                null,
                null
        );

        when(tokenHashService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(storedToken));

        RefreshSessionUseCase useCase = new RefreshSessionUseCase(
                appUserRepository,
                refreshTokenRepository,
                tokenHashService,
                accessTokenService,
                clock,
                Duration.ofDays(7)
        );

        assertThrows(RefreshTokenExpiredException.class, () -> useCase.execute("raw-refresh-token"));

        verify(appUserRepository, never()).findById(any());
        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logoutUseCase_whenTokenIsActive_revokesToken() {
        Clock clock = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
        RefreshToken storedToken = RefreshToken.restore(
                11L,
                7L,
                "hashed-refresh-token",
                LocalDateTime.of(2026, 1, 20, 10, 0),
                LocalDateTime.of(2026, 2, 4, 10, 0),
                null,
                null
        );

        when(tokenHashService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(storedToken));

        LogoutUseCase useCase = new LogoutUseCase(refreshTokenRepository, tokenHashService, clock);

        useCase.execute("raw-refresh-token");

        ArgumentCaptor<RefreshToken> tokenCaptor = ArgumentCaptor.forClass(RefreshToken.class);
        verify(refreshTokenRepository).save(tokenCaptor.capture());
        assertEquals(11L, tokenCaptor.getValue().id());
        assertNotNull(tokenCaptor.getValue().revokedAt());
    }

    @Test
    void logoutUseCase_whenTokenMissing_throwsInvalidRefreshToken() {
        when(tokenHashService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.empty());

        LogoutUseCase useCase = new LogoutUseCase(refreshTokenRepository, tokenHashService, Clock.systemUTC());

        assertThrows(InvalidRefreshTokenException.class, () -> useCase.execute("raw-refresh-token"));

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }

    @Test
    void logoutUseCase_whenTokenAlreadyRevoked_returnsWithoutSaving() {
        RefreshToken storedToken = RefreshToken.restore(
                11L,
                7L,
                "hashed-refresh-token",
                LocalDateTime.of(2026, 1, 20, 10, 0),
                LocalDateTime.of(2026, 2, 4, 10, 0),
                LocalDateTime.of(2026, 1, 21, 10, 0),
                null
        );

        when(tokenHashService.hash("raw-refresh-token")).thenReturn("hashed-refresh-token");
        when(refreshTokenRepository.findByTokenHash("hashed-refresh-token")).thenReturn(Optional.of(storedToken));

        LogoutUseCase useCase = new LogoutUseCase(refreshTokenRepository, tokenHashService, Clock.systemUTC());

        useCase.execute("raw-refresh-token");

        verify(refreshTokenRepository, never()).save(any(RefreshToken.class));
    }
}