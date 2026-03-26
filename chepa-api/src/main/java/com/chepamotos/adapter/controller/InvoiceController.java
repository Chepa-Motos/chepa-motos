package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateServiceInvoiceRequest;
import com.chepamotos.adapter.dto.CreateDeliveryInvoiceRequest;
import com.chepamotos.adapter.dto.InvoiceResponse;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.service.InvoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

    private final InvoiceService invoiceService;

    public InvoiceController(InvoiceService invoiceService) {
        this.invoiceService = invoiceService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<InvoiceResponse>>> list() {
        List<InvoiceResponse> data = invoiceService.listAll()
                .stream()
                .map(InvoiceResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable("id") Long invoiceId) {
        InvoiceResponse data = InvoiceResponse.fromDomain(invoiceService.getById(invoiceId));
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping("/service")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createService(
            @Valid @RequestBody CreateServiceInvoiceRequest request) {
        List<InvoiceService.InvoiceItemData> itemData = request.items().stream()
                .map(i -> new InvoiceService.InvoiceItemData(i.description(), i.quantity(), i.unitPrice()))
                .toList();

        InvoiceResponse data = InvoiceResponse.fromDomain(
                invoiceService.create(
                        InvoiceType.SERVICE,
                        request.mechanicId(),
                        request.vehiclePlate(),
                        request.model(),
                        null,  // buyerName is null for SERVICE
                        request.laborAmount(),
                        itemData
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PostMapping("/delivery")
    public ResponseEntity<ApiResponse<InvoiceResponse>> createDelivery(
            @Valid @RequestBody CreateDeliveryInvoiceRequest request) {
        List<InvoiceService.InvoiceItemData> itemData = request.items().stream()
                .map(i -> new InvoiceService.InvoiceItemData(i.description(), i.quantity(), i.unitPrice()))
                .toList();

        InvoiceResponse data = InvoiceResponse.fromDomain(
                invoiceService.create(
                        InvoiceType.DELIVERY,
                        null,  // mechanicId is null for DELIVERY
                        null,  // vehiclePlate is null for DELIVERY
                        null,  // vehicleModel is null for DELIVERY
                        request.buyerName(),
                        null,  // laborAmount is null (will default to 0)
                        itemData
                )
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }
}
