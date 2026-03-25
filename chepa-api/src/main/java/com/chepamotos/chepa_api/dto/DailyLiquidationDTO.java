package com.chepamotos.chepa_api.dto;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DailyLiquidationDTO {
    private Long liquidationId;
    private Long mechanicId;
    private String mechanicName;
    private LocalDate liquidationDate;
    private BigDecimal total;
}