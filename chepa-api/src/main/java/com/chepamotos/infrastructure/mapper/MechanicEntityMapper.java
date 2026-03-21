package com.chepamotos.infrastructure.mapper;

public final class MechanicEntityMapper {

    private MechanicEntityMapper() {
    }

    public static com.chepamotos.domain.model.Mechanic toDomain(com.chepamotos.infrastructure.entity.Mechanic entity) {
        if (entity == null) {
            return null;
        }
        return com.chepamotos.domain.model.Mechanic.restore(entity.getId(), entity.getName(), entity.isActive());
    }

    public static com.chepamotos.infrastructure.entity.Mechanic toEntity(com.chepamotos.domain.model.Mechanic domain) {
        if (domain == null) {
            return null;
        }
        com.chepamotos.infrastructure.entity.Mechanic entity = new com.chepamotos.infrastructure.entity.Mechanic();
        entity.setId(domain.id());
        entity.setName(domain.name());
        entity.setActive(domain.active());
        return entity;
    }
}
