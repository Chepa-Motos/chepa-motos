package com.chepamotos.infrastructure.mapper;

public final class AppUserEntityMapper {

    private AppUserEntityMapper() {
    }

    public static com.chepamotos.domain.model.AppUser toDomain(com.chepamotos.infrastructure.entity.AppUser entity) {
        if (entity == null) {
            return null;
        }

        return com.chepamotos.domain.model.AppUser.restore(
                entity.getId(),
                entity.getUsername(),
                entity.getPasswordHash(),
                entity.getRole(),
                entity.isActive(),
                entity.getCreatedAt()
        );
    }

    public static com.chepamotos.infrastructure.entity.AppUser toEntity(com.chepamotos.domain.model.AppUser domain) {
        if (domain == null) {
            return null;
        }

        com.chepamotos.infrastructure.entity.AppUser entity = new com.chepamotos.infrastructure.entity.AppUser();
        entity.setId(domain.id());
        entity.setUsername(domain.username());
        entity.setPasswordHash(domain.passwordHash());
        entity.setRole(domain.role());
        entity.setActive(domain.active());
        entity.setCreatedAt(domain.createdAt());
        return entity;
    }
}
