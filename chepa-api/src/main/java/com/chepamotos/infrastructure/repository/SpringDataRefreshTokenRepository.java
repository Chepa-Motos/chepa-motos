package com.chepamotos.infrastructure.repository;

import com.chepamotos.infrastructure.entity.RefreshToken;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SpringDataRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Query("""
            SELECT rt
            FROM RefreshToken rt
            WHERE rt.user.id = :userId
              AND rt.revokedAt IS NULL
              AND rt.expiresAt > :now
            ORDER BY rt.issuedAt DESC
            """)
    List<RefreshToken> findActiveByUserId(@Param("userId") Long userId, @Param("now") LocalDateTime now);
}
