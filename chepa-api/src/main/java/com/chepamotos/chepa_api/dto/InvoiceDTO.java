package com.chepamotos.chepa_api.dto;

import com.chepamotos.chepa_api.enums.InvoiceType;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InvoiceDTO {
    private Long invoiceId;
    private LocalDate invoiceDate;
    private InvoiceType invoiceType;
    private Long mechanicId;
    private String mechanicName;
    private String vehiclePlate;
    private String vehicleBrand;
    private String vehicleModel;
    private BigDecimal total;
    private Boolean isCancelled;
}