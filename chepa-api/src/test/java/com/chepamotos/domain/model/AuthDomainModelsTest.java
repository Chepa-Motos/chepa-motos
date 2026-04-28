package com.chepamotos.domain.model;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthDomainModelsTest {

    @Test
    void appUserCreateNew_normalizesUsernameAndStoresValues() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 28, 10, 0);

        AppUser user = AppUser.createNew("  Admin  ", "  hash-value  ", UserRole.GERENTE, createdAt);

        assertEquals(null, user.id());
        assertEquals("admin", user.username());
        assertEquals("hash-value", user.passwordHash());
        assertEquals(UserRole.GERENTE, user.role());
        assertTrue(user.active());
        assertEquals(createdAt, user.createdAt());
    }

    @Test
    void appUserWithPasswordHashAndStatus_createUpdatedCopies() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 28, 10, 0);
        AppUser user = AppUser.restore(7L, "admin", "hash-value", UserRole.GERENTE, true, createdAt);

        AppUser updated = user.withPasswordHash("  new-hash  ").withStatus(false);

        assertEquals(7L, updated.id());
        assertEquals("admin", updated.username());
        assertEquals("new-hash", updated.passwordHash());
        assertEquals(UserRole.GERENTE, updated.role());
        assertTrue(!updated.active());
        assertEquals(createdAt, updated.createdAt());
    }

    @Test
    void appUserValidationRejectsBlankUsernameAndPasswordHash() {
        LocalDateTime createdAt = LocalDateTime.of(2026, 1, 28, 10, 0);

        assertThrows(IllegalArgumentException.class,
                () -> AppUser.createNew("   ", "hash-value", UserRole.GERENTE, createdAt));
        assertThrows(IllegalArgumentException.class,
                () -> AppUser.createNew("admin", "   ", UserRole.GERENTE, createdAt));
    }

    @Test
    void accessTokenTrimsTokenAndValidatesExpiration() {
        AccessToken token = new AccessToken("  jwt-token  ", 1200);

        assertEquals("jwt-token", token.token());
        assertEquals(1200, token.expiresInSeconds());
        assertThrows(IllegalArgumentException.class, () -> new AccessToken("   ", 10));
        assertThrows(IllegalArgumentException.class, () -> new AccessToken("jwt-token", 0));
    }

    @Test
    void authTokensWrapAccessTokenAndValidateRefreshToken() {
        AuthTokens tokens = AuthTokens.of(new AccessToken("jwt-token", 900), "  refresh-token  ");

        assertEquals("Bearer", tokens.tokenType());
        assertEquals("jwt-token", tokens.accessToken());
        assertEquals(900, tokens.accessTokenExpiresInSeconds());
        assertEquals("refresh-token", tokens.refreshToken());
    }

    @Test
    void refreshTokenCreateRestoreAndRevoke_behavesAsExpected() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 1, 28, 10, 0);
        LocalDateTime expiresAt = issuedAt.plusDays(7);

        RefreshToken token = RefreshToken.createNew(7L, "  hash-value  ", issuedAt, expiresAt);
        RefreshToken revoked = token.revoke(issuedAt.plusHours(2), 99L);

        assertEquals(null, token.id());
        assertEquals(7L, token.userId());
        assertEquals("hash-value", token.tokenHash());
        assertEquals(issuedAt, token.issuedAt());
        assertEquals(expiresAt, token.expiresAt());
        assertEquals(null, token.revokedAt());
        assertEquals(null, token.replacedByTokenId());

        assertEquals(issuedAt.plusHours(2), revoked.revokedAt());
        assertEquals(99L, revoked.replacedByTokenId());
    }

    @Test
    void refreshTokenValidationRejectsInvalidValues() {
        LocalDateTime issuedAt = LocalDateTime.of(2026, 1, 28, 10, 0);

        assertThrows(IllegalArgumentException.class,
                () -> RefreshToken.createNew(7L, "   ", issuedAt, issuedAt.plusDays(7)));
        assertThrows(IllegalArgumentException.class,
                () -> RefreshToken.createNew(7L, "hash", issuedAt, issuedAt));
        assertThrows(IllegalArgumentException.class,
                () -> RefreshToken.createNew(7L, "hash", issuedAt, issuedAt.minusDays(1)));
    }

    @Test
    void dailyLiquidationCalculatesShares() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        DailyLiquidation liquidation = DailyLiquidation.create(mechanic, LocalDate.of(2026, 1, 28), new BigDecimal("304100"), 4);

        assertEquals(null, liquidation.id());
        assertEquals(new BigDecimal("304100.00"), liquidation.totalRevenue());
        assertEquals(new BigDecimal("212870.00"), liquidation.mechanicShare());
        assertEquals(new BigDecimal("91230.00"), liquidation.shopShare());
        assertEquals(4, liquidation.invoiceCount());
    }
}