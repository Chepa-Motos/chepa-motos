package com.chepamotos.infrastructure.mapper;

import java.util.ArrayList;
import java.util.List;

public final class InvoiceEntityMapper {

    private InvoiceEntityMapper() {
    }

    public static com.chepamotos.domain.model.Invoice toDomain(com.chepamotos.infrastructure.entity.Invoice entity) {
        if (entity == null) {
            return null;
        }

        List<com.chepamotos.domain.model.InvoiceItem> items = new ArrayList<>();
        if (entity.getItems() != null) {
            for (com.chepamotos.infrastructure.entity.InvoiceItem itemEntity : entity.getItems()) {
                items.add(InvoiceItemEntityMapper.toDomain(itemEntity));
            }
        }

        return com.chepamotos.domain.model.Invoice.restore(
                entity.getId(),
                entity.getInvoiceType(),
                MechanicEntityMapper.toDomain(entity.getMechanic()),
                VehicleEntityMapper.toDomain(entity.getVehicle()),
                entity.getBuyerName(),
                entity.getCreatedAt(),
                entity.getLaborAmount(),
                entity.isCancelled(),
                items
        );
    }

    public static com.chepamotos.infrastructure.entity.Invoice toEntity(com.chepamotos.domain.model.Invoice domain) {
        if (domain == null) {
            return null;
        }

        com.chepamotos.infrastructure.entity.Invoice entity = new com.chepamotos.infrastructure.entity.Invoice();
        entity.setId(domain.id());
        entity.setInvoiceType(domain.type());
        entity.setMechanic(MechanicEntityMapper.toEntity(domain.mechanic()));
        entity.setVehicle(VehicleEntityMapper.toEntity(domain.vehicle()));
        entity.setBuyerName(domain.buyerName());
        entity.setCreatedAt(domain.createdAt());
        entity.setLaborAmount(domain.laborAmount());
        entity.setTotalAmount(domain.totalAmount());
        entity.setCancelled(domain.cancelled());

        List<com.chepamotos.infrastructure.entity.InvoiceItem> itemEntities = new ArrayList<>();
        for (com.chepamotos.domain.model.InvoiceItem item : domain.items()) {
            itemEntities.add(InvoiceItemEntityMapper.toEntity(item, entity));
        }
        entity.setItems(itemEntities);

        return entity;
    }
}
