package com.chepamotos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Objects;

public final class InvoiceItem {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final Long id;
    private final String description;
    private final BigDecimal quantity;
    private final BigDecimal unitPrice;
    private final BigDecimal subtotal;

    private InvoiceItem(Long id, String description, BigDecimal quantity, BigDecimal unitPrice) {
        this.id = id;
        this.description = normalizeDescription(description);
        this.quantity = normalizeQuantity(quantity);
        this.unitPrice = normalizeMoney(unitPrice, "Unit price");
        this.subtotal = this.quantity.multiply(this.unitPrice).setScale(2, RoundingMode.HALF_UP);
    }

    public static InvoiceItem createNew(String description, BigDecimal quantity, BigDecimal unitPrice) {
        return new InvoiceItem(null, description, quantity, unitPrice);
    }

    public static InvoiceItem restore(Long id, String description, BigDecimal quantity, BigDecimal unitPrice) {
        return new InvoiceItem(id, description, quantity, unitPrice);
    }

    public Long id() {
        return id;
    }

    public String description() {
        return description;
    }

    public BigDecimal quantity() {
        return quantity;
    }

    public BigDecimal unitPrice() {
        return unitPrice;
    }

    public BigDecimal subtotal() {
        return subtotal;
    }

    private static String normalizeDescription(String raw) {
        String normalized = Objects.requireNonNull(raw, "Item description is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Item description cannot be blank");
        }
        if (normalized.length() > 255) {
            throw new IllegalArgumentException("Item description cannot exceed 255 characters");
        }
        return normalized;
    }

    private static BigDecimal normalizeQuantity(BigDecimal raw) {
        BigDecimal normalized = normalizeMoney(raw, "Quantity");
        if (normalized.compareTo(ZERO) <= 0) {
            throw new IllegalArgumentException("Quantity must be greater than zero");
        }
        return normalized;
    }

    private static BigDecimal normalizeMoney(BigDecimal raw, String fieldName) {
        BigDecimal normalized = Objects.requireNonNull(raw, fieldName + " is required").setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return normalized;
    }
}
