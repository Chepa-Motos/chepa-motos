package com.chepamotos.domain.model;

import java.math.BigDecimal;

public record InvoiceItemInput(
        String description,
        BigDecimal quantity,
        BigDecimal unitPrice) {
}