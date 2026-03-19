package com.chepamotos.domain.model;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.Objects;

public final class DailyLiquidation {

    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal MECHANIC_RATE = new BigDecimal("0.70");
    private static final BigDecimal SHOP_RATE = new BigDecimal("0.30");

    private final Long id;
    private final Mechanic mechanic;
    private final LocalDate date;
    private final BigDecimal totalRevenue;
    private final BigDecimal mechanicShare;
    private final BigDecimal shopShare;
    private final int invoiceCount;

    private DailyLiquidation(Long id, Mechanic mechanic, LocalDate date, BigDecimal totalRevenue, int invoiceCount) {
        this.id = id;
        this.mechanic = Objects.requireNonNull(mechanic, "Mechanic is required");
        this.date = Objects.requireNonNull(date, "Date is required");
        this.totalRevenue = normalizeMoney(totalRevenue, "Total revenue");
        if (invoiceCount < 0) {
            throw new IllegalArgumentException("Invoice count cannot be negative");
        }
        this.invoiceCount = invoiceCount;

        this.mechanicShare = this.totalRevenue.multiply(MECHANIC_RATE).setScale(2, RoundingMode.HALF_UP);
        this.shopShare = this.totalRevenue.multiply(SHOP_RATE).setScale(2, RoundingMode.HALF_UP);
    }

    public static DailyLiquidation create(Mechanic mechanic, LocalDate date, BigDecimal totalRevenue, int invoiceCount) {
        return new DailyLiquidation(null, mechanic, date, totalRevenue, invoiceCount);
    }

    public static DailyLiquidation restore(Long id, Mechanic mechanic, LocalDate date, BigDecimal totalRevenue, int invoiceCount) {
        return new DailyLiquidation(id, mechanic, date, totalRevenue, invoiceCount);
    }

    public Long id() {
        return id;
    }

    public Mechanic mechanic() {
        return mechanic;
    }

    public LocalDate date() {
        return date;
    }

    public BigDecimal totalRevenue() {
        return totalRevenue;
    }

    public BigDecimal mechanicShare() {
        return mechanicShare;
    }

    public BigDecimal shopShare() {
        return shopShare;
    }

    public int invoiceCount() {
        return invoiceCount;
    }

    private static BigDecimal normalizeMoney(BigDecimal raw, String fieldName) {
        BigDecimal normalized = Objects.requireNonNull(raw, fieldName + " is required").setScale(2, RoundingMode.HALF_UP);
        if (normalized.compareTo(ZERO) < 0) {
            throw new IllegalArgumentException(fieldName + " cannot be negative");
        }
        return normalized;
    }
}
