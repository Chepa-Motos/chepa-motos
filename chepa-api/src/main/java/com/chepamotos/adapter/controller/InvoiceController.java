package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateDeliveryInvoiceRequest;
import com.chepamotos.adapter.dto.CreateServiceInvoiceRequest;
import com.chepamotos.adapter.dto.InvoiceCancelResponse;
import com.chepamotos.adapter.dto.InvoiceResponse;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.infrastructure.application.InvoiceApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

        private final InvoiceApplicationService invoiceApplicationService;

        public InvoiceController(InvoiceApplicationService invoiceApplicationService) {
                this.invoiceApplicationService = invoiceApplicationService;
        }

        @GetMapping
        public ResponseEntity<ApiResponse<List<InvoiceResponse>>> list() {
                List<InvoiceResponse> data = invoiceApplicationService.listAll()
                                .stream()
                                .map(InvoiceResponse::fromDomain)
                                .toList();
                return ResponseEntity.ok(ApiResponse.of(data));
        }

        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<InvoiceResponse>> getById(@PathVariable("id") Long invoiceId) {
                return ResponseEntity.ok(ApiResponse.of(
                                InvoiceResponse.fromDomain(invoiceApplicationService.getById(invoiceId))));
        }

        @PostMapping("/service")
        public ResponseEntity<ApiResponse<InvoiceResponse>> createService(
                        @Valid @RequestBody CreateServiceInvoiceRequest request) {
                List<InvoiceItemInput> itemInputs = request.items().stream()
                                .map(i -> new InvoiceItemInput(i.description(), i.quantity(), i.unitPrice()))
                                .toList();

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                                InvoiceResponse.fromDomain(invoiceApplicationService.createService(
                                                request.mechanicId(),
                                                request.vehiclePlate(),
                                                request.model(),
                                                request.laborAmount(),
                                                itemInputs))));
        }

        @PostMapping("/delivery")
        public ResponseEntity<ApiResponse<InvoiceResponse>> createDelivery(
                        @Valid @RequestBody CreateDeliveryInvoiceRequest request) {
                List<InvoiceItemInput> itemInputs = request.items().stream()
                                .map(i -> new InvoiceItemInput(i.description(), i.quantity(), i.unitPrice()))
                                .toList();

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                                InvoiceResponse.fromDomain(invoiceApplicationService.createDelivery(
                                                request.buyerName(),
                                                itemInputs))));
        }

        @PatchMapping("/{id}/cancel")
        public ResponseEntity<ApiResponse<InvoiceCancelResponse>> cancel(@PathVariable("id") Long invoiceId) {
                return ResponseEntity.ok(ApiResponse.of(
                                InvoiceCancelResponse.fromDomain(invoiceApplicationService.cancel(invoiceId))));
        }
}