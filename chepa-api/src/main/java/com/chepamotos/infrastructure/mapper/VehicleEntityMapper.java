package com.chepamotos.infrastructure.mapper;

public final class VehicleEntityMapper {

    private VehicleEntityMapper() {
    }

    public static com.chepamotos.domain.model.Vehicle toDomain(com.chepamotos.infrastructure.entity.Vehicle entity) {
        if (entity == null) {
            return null;
        }
        return com.chepamotos.domain.model.Vehicle.restore(entity.getId(), entity.getPlate(), entity.getModel());
    }

    public static com.chepamotos.infrastructure.entity.Vehicle toEntity(com.chepamotos.domain.model.Vehicle domain) {
        if (domain == null) {
            return null;
        }
        com.chepamotos.infrastructure.entity.Vehicle entity = new com.chepamotos.infrastructure.entity.Vehicle();
        entity.setId(domain.id());
        entity.setPlate(domain.plate());
        entity.setModel(domain.model());
        return entity;
    }
}
