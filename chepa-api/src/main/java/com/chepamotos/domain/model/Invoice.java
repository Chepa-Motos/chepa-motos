package com.chepamotos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class Invoice {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);

    private final Long id;
    private final InvoiceType type;
    private final Mechanic mechanic;
    private final Vehicle vehicle;
    private final String buyerName;
    private final LocalDateTime createdAt;
    private final BigDecimal laborAmount;
    private final BigDecimal totalAmount;
    private final boolean cancelled;
    private final List<InvoiceItem> items;

    private Invoice(
            Long id,
            InvoiceType type,
            Mechanic mechanic,
            Vehicle vehicle,
            String buyerName,
            LocalDateTime createdAt,
            BigDecimal laborAmount,
            boolean cancelled,
            List<InvoiceItem> items
    ) {
        this.id = id;
        this.type = Objects.requireNonNull(type, "Invoice type is required");
        this.createdAt = Objects.requireNonNullElse(createdAt, LocalDateTime.now());
        this.cancelled = cancelled;
        this.items = normalizeItems(items);

        if (this.type == InvoiceType.SERVICE) {
            this.mechanic = Objects.requireNonNull(mechanic, "Service invoice requires mechanic");
            this.vehicle = Objects.requireNonNull(vehicle, "Service invoice requires vehicle");
            this.buyerName = null;
            this.laborAmount = normalizeMoney(laborAmount, "Labor amount");
        } else {
            this.mechanic = null;
            this.vehicle = null;
            this.buyerName = normalizeBuyerName(buyerName);
            this.laborAmount = ZERO;
        }

        this.totalAmount = computeItemsSubtotal(this.items).add(this.laborAmount).setScale(2, RoundingMode.HALF_UP);
    }

    public static Invoice createService(Mechanic mechanic, Vehicle vehicle, BigDecimal laborAmount, List<InvoiceItem> items) {
        return new Invoice(null, InvoiceType.SERVICE, mechanic, vehicle, null, null, laborAmount, false, items);
    }

    public static Invoice createDelivery(String buyerName, List<InvoiceItem> items) {
        return new Invoice(null, InvoiceType.DELIVERY, null, null, buyerName, null, ZERO, false, items);
    }

    public static Invoice restore(
            Long id,
            InvoiceType type,
            Mechanic mechanic,
            Vehicle vehicle,
            String buyerName,
            LocalDateTime createdAt,
            BigDecimal laborAmount,
            boolean cancelled,
            List<InvoiceItem> items
    ) {
        return new Invoice(id, type, mechanic, vehicle, buyerName, createdAt, laborAmount, cancelled, items);
    }

    public Invoice cancel() {
        if (this.cancelled) {
            throw new IllegalStateException("Invoice is already cancelled");
        }
        return new Invoice(id, type, mechanic, vehicle, buyerName, createdAt, laborAmount, true, items);
    }

    public Long id() {
        return id;
    }

    public InvoiceType type() {
        return type;
    }

    public Mechanic mechanic() {
        return mechanic;
    }

    public Vehicle vehicle() {
        return vehicle;
    }

    public String buyerName() {
        return buyerName;
    }

    public LocalDateTime createdAt() {
        return createdAt;
    }

    public BigDecimal laborAmount() {
        return laborAmount;
    }

    public BigDecimal totalAmount() {
        return totalAmount;
    }

    public boolean cancelled() {
        return cancelled;
    }

    public List<InvoiceItem> items() {
        return items;
    }

    private static List<InvoiceItem> normalizeItems(List<InvoiceItem> rawItems) {
        List<InvoiceItem> normalized = new ArrayList<>(Objects.requireNonNull(rawItems, "Invoice items are required"));
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Invoice requires at least one item");
        }
        return List.copyOf(normalized);
    }

    private static String normalizeBuyerName(String raw) {
        String normalized = Objects.requireNonNull(raw, "Buyer name is required").trim();
        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Buyer name cannot be blank");
        }
        if (normalized.length() > 150) {
            throw new IllegalArgumentException("Buyer name cannot exceed 150 characters");
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

    private static BigDecimal computeItemsSubtotal(List<InvoiceItem> items) {
        BigDecimal subtotal = ZERO;
        for (InvoiceItem item : items) {
            subtotal = subtotal.add(item.subtotal());
        }
        return subtotal.setScale(2, RoundingMode.HALF_UP);
    }
}
