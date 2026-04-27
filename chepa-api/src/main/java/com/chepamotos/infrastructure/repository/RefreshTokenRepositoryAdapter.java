package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.RefreshToken;
import com.chepamotos.infrastructure.mapper.RefreshTokenEntityMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public class RefreshTokenRepositoryAdapter implements com.chepamotos.domain.port.RefreshTokenRepository {

    private final SpringDataRefreshTokenRepository springDataRefreshTokenRepository;
    private final SpringDataAppUserRepository springDataAppUserRepository;

    public RefreshTokenRepositoryAdapter(SpringDataRefreshTokenRepository springDataRefreshTokenRepository,
                                         SpringDataAppUserRepository springDataAppUserRepository) {
        this.springDataRefreshTokenRepository = springDataRefreshTokenRepository;
        this.springDataAppUserRepository = springDataAppUserRepository;
    }

    @Override
    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return springDataRefreshTokenRepository.findByTokenHash(tokenHash)
                .map(RefreshTokenEntityMapper::toDomain);
    }

    @Override
    public List<RefreshToken> findActiveByUserId(Long userId, LocalDateTime now) {
        return springDataRefreshTokenRepository.findActiveByUserId(userId, now)
                .stream()
                .map(RefreshTokenEntityMapper::toDomain)
                .toList();
    }

    @Override
    public RefreshToken save(RefreshToken refreshToken) {
        var userEntity = springDataAppUserRepository.findById(refreshToken.userId())
                .orElseThrow(() -> new IllegalArgumentException("User not found for refresh token persistence"));

        var entityToSave = new com.chepamotos.infrastructure.entity.RefreshToken();
        entityToSave.setId(refreshToken.id());
        entityToSave.setUser(userEntity);
        entityToSave.setTokenHash(refreshToken.tokenHash());
        entityToSave.setIssuedAt(refreshToken.issuedAt());
        entityToSave.setExpiresAt(refreshToken.expiresAt());
        entityToSave.setRevokedAt(refreshToken.revokedAt());
        entityToSave.setReplacedByTokenId(refreshToken.replacedByTokenId());

        var savedEntity = springDataRefreshTokenRepository.save(entityToSave);
        return RefreshTokenEntityMapper.toDomain(savedEntity);
    }
}
