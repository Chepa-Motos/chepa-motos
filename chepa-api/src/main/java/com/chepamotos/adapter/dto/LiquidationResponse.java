package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.DailyLiquidation;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LiquidationResponse(
        Long id,
        MechanicResponse mechanic,
        LocalDate date,
        @JsonProperty("invoice_count") int invoiceCount,
        @JsonProperty("total_revenue") BigDecimal totalRevenue,
        @JsonProperty("mechanic_share") BigDecimal mechanicShare,
        @JsonProperty("shop_share") BigDecimal shopShare
) {
    public static LiquidationResponse fromDomain(DailyLiquidation liquidation) {
        return new LiquidationResponse(
                liquidation.id(),
                MechanicResponse.fromDomain(liquidation.mechanic()),
                liquidation.date(),
                liquidation.invoiceCount(),
                liquidation.totalRevenue(),
                liquidation.mechanicShare(),
                liquidation.shopShare()
        );
    }
}
