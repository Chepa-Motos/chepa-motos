package com.chepamotos.infrastructure.security;

import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.port.AccessTokenService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtAccessTokenService implements AccessTokenService {

    private final SecretKey signingKey;
    private final Clock clock;
    private final long accessTokenTtlMinutes;

    public JwtAccessTokenService(
            @Value("${auth.jwt.secret}") String jwtSecret,
            @Value("${auth.jwt.access-token-ttl-minutes:15}") long accessTokenTtlMinutes,
            Clock clock
    ) {
        byte[] secretBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("JWT secret must be at least 32 bytes");
        }
        this.signingKey = Keys.hmacShaKeyFor(secretBytes);
        this.accessTokenTtlMinutes = accessTokenTtlMinutes;
        this.clock = clock;
    }

    @Override
    public AccessToken generate(AppUser user) {
        Instant issuedAt = Instant.now(clock);
        Instant expiresAt = issuedAt.plus(accessTokenTtlMinutes, ChronoUnit.MINUTES);

        String token = Jwts.builder()
                .subject(user.username())
                .claim("uid", user.id())
                .claim("role", user.role().name())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .signWith(signingKey)
                .compact();

        return new AccessToken(token, ChronoUnit.SECONDS.between(issuedAt, expiresAt));
    }
}
