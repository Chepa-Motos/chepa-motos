package com.chepamotos.infrastructure.mapper;

public final class InvoiceItemEntityMapper {

    private InvoiceItemEntityMapper() {
    }

    public static com.chepamotos.domain.model.InvoiceItem toDomain(com.chepamotos.infrastructure.entity.InvoiceItem entity) {
        if (entity == null) {
            return null;
        }
        return com.chepamotos.domain.model.InvoiceItem.restore(
                entity.getId(),
                entity.getDescription(),
                entity.getQuantity(),
                entity.getUnitPrice()
        );
    }

    public static com.chepamotos.infrastructure.entity.InvoiceItem toEntity(
            com.chepamotos.domain.model.InvoiceItem domain,
            com.chepamotos.infrastructure.entity.Invoice invoiceEntity
    ) {
        if (domain == null) {
            return null;
        }
        com.chepamotos.infrastructure.entity.InvoiceItem entity = new com.chepamotos.infrastructure.entity.InvoiceItem();
        entity.setId(domain.id());
        entity.setInvoice(invoiceEntity);
        entity.setDescription(domain.description());
        entity.setQuantity(domain.quantity());
        entity.setUnitPrice(domain.unitPrice());
        entity.setSubtotal(domain.subtotal());
        return entity;
    }
}
