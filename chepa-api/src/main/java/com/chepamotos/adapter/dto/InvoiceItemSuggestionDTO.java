package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.InvoiceItem;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;

@Schema(description = "Invoice item suggestion for autocomplete")
public record InvoiceItemSuggestionDTO(
        @Schema(description = "Item description", example = "Freno Delantero")
        String description,
        @JsonProperty("unit_price")
        @Schema(description = "Unit price in COP", example = "45000.00")
        BigDecimal unitPrice
) {

    public static InvoiceItemSuggestionDTO fromDomain(InvoiceItem item) {
        return new InvoiceItemSuggestionDTO(item.description(), item.unitPrice());
    }
}
