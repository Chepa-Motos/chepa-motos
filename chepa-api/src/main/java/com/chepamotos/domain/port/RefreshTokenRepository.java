package com.chepamotos.domain.port;

import com.chepamotos.domain.model.RefreshToken;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface RefreshTokenRepository {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findActiveByUserId(Long userId, LocalDateTime now);

    RefreshToken save(RefreshToken refreshToken);
}
