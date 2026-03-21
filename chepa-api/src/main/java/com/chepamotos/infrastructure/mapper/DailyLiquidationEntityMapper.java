package com.chepamotos.infrastructure.mapper;

public final class DailyLiquidationEntityMapper {

    private DailyLiquidationEntityMapper() {
    }

    public static com.chepamotos.domain.model.DailyLiquidation toDomain(
            com.chepamotos.infrastructure.entity.DailyLiquidation entity
    ) {
        if (entity == null) {
            return null;
        }

        return com.chepamotos.domain.model.DailyLiquidation.restore(
                entity.getId(),
                MechanicEntityMapper.toDomain(entity.getMechanic()),
                entity.getDate(),
                entity.getTotalRevenue(),
                entity.getInvoiceCount()
        );
    }

    public static com.chepamotos.infrastructure.entity.DailyLiquidation toEntity(
            com.chepamotos.domain.model.DailyLiquidation domain
    ) {
        if (domain == null) {
            return null;
        }

        com.chepamotos.infrastructure.entity.DailyLiquidation entity = new com.chepamotos.infrastructure.entity.DailyLiquidation();
        entity.setId(domain.id());
        entity.setMechanic(MechanicEntityMapper.toEntity(domain.mechanic()));
        entity.setDate(domain.date());
        entity.setTotalRevenue(domain.totalRevenue());
        entity.setMechanicShare(domain.mechanicShare());
        entity.setShopShare(domain.shopShare());
        entity.setInvoiceCount(domain.invoiceCount());
        return entity;
    }
}
