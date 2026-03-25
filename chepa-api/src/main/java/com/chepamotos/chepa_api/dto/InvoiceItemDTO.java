package com.chepamotos.chepa_api.dto;

import lombok.*;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceItemDTO {
    private Long itemId;
    private Long invoiceId;
    private String description;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;
}