package com.chepamotos.infrastructure.mapper;

public final class RefreshTokenEntityMapper {

    private RefreshTokenEntityMapper() {
    }

    public static com.chepamotos.domain.model.RefreshToken toDomain(com.chepamotos.infrastructure.entity.RefreshToken entity) {
        if (entity == null) {
            return null;
        }

        return com.chepamotos.domain.model.RefreshToken.restore(
                entity.getId(),
                entity.getUser().getId(),
                entity.getTokenHash(),
                entity.getIssuedAt(),
                entity.getExpiresAt(),
                entity.getRevokedAt(),
                entity.getReplacedByTokenId()
        );
    }
}
