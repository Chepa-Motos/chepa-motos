package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateDeliveryInvoiceRequest;
import com.chepamotos.adapter.dto.CreateServiceInvoiceRequest;
import com.chepamotos.adapter.dto.InvoiceCancelResponse;
import com.chepamotos.adapter.dto.InvoiceResponse;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.port.in.InvoiceApplicationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Facturas")
@RestController
@RequestMapping("/api/invoices")
public class InvoiceController {

        private final InvoiceApplicationUseCase invoiceApplicationService;
        private final Clock clock;

        public InvoiceController(InvoiceApplicationUseCase invoiceApplicationService, Clock clock) {
                this.invoiceApplicationService = invoiceApplicationService;
                this.clock = clock;
        }

        @Operation(
                summary = "Listar facturas",
                description = "Retorna facturas filtradas por fecha, tipo, mecánico y estado de cancelación. " +
                              "Por defecto retorna las facturas activas del día de hoy. Endpoint público."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de facturas (puede ser vacía)")
        })
        @GetMapping
        public ResponseEntity<ApiResponse<List<InvoiceResponse>>> list(
                        @Parameter(description = "Filtrar por fecha (YYYY-MM-DD). Por defecto: hoy.", example = "2026-01-28")
                        @RequestParam(name = "date", required = false) LocalDate date,
                        @Parameter(description = "Filtrar por tipo: SERVICE o DELIVERY.", example = "SERVICE")
                        @RequestParam(name = "type", required = false) InvoiceType type,
                        @Parameter(description = "Filtrar por ID de mecánico.", example = "1")
                        @RequestParam(name = "mechanic_id", required = false) Long mechanicId,
                        @Parameter(description = "Incluir canceladas. Por defecto false (solo activas).", example = "false")
                        @RequestParam(name = "cancelled", defaultValue = "false") boolean cancelled) {
                List<InvoiceResponse> data = invoiceApplicationService.list(date, type, mechanicId, cancelled)
                                .stream()
                                .map(InvoiceResponse::fromDomain)
                                .toList();
                return ResponseEntity.ok(ApiResponse.of(data, clock));
        }

        @Operation(summary = "Obtener factura por ID", description = "Retorna la factura completa con todos sus ítems. Endpoint público.")
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Factura encontrada"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Factura no encontrada",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"code":"INVOICE_NOT_FOUND","message":"Invoice with ID 99 does not exist","timestamp":"2026-01-28T10:00:00"}
                                        """)))
        })
        @GetMapping("/{id}")
        public ResponseEntity<ApiResponse<InvoiceResponse>> getById(
                        @Parameter(description = "ID de la factura.", example = "14")
                        @PathVariable("id") Long invoiceId) {
                return ResponseEntity.ok(ApiResponse.of(
                                InvoiceResponse.fromDomain(invoiceApplicationService.getById(invoiceId)), clock));
        }

        @Operation(
                summary = "Crear factura de servicio",
                description = "Crea una factura SERVICE con mecánico y vehículo. " +
                              "Si la placa no existe se crea el vehículo en la misma transacción; si existe, se actualiza el modelo. " +
                              "El backend calcula subtotales y total. Endpoint público."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Factura creada"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error de validación",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"code":"VALIDATION_ERROR","message":"mechanic_id: mechanic_id is required","timestamp":"2026-01-28T10:00:00"}
                                        """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mecánico no encontrado",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"code":"MECHANIC_NOT_FOUND","message":"Mechanic with ID 99 does not exist","timestamp":"2026-01-28T10:00:00"}
                                        """)))
        })
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
                                                itemInputs)), clock));
        }

        @Operation(
                summary = "Crear factura de entrega",
                description = "Crea una factura DELIVERY para venta de repuestos sin servicio de taller. " +
                              "Sin mecánico ni vehículo. labor_amount siempre 0. Total = suma de subtotales. Endpoint público."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Factura creada"),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error de validación",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"code":"VALIDATION_ERROR","message":"buyer_name: buyer_name is required","timestamp":"2026-01-28T10:00:00"}
                                        """)))
        })
        @PostMapping("/delivery")
        public ResponseEntity<ApiResponse<InvoiceResponse>> createDelivery(
                        @Valid @RequestBody CreateDeliveryInvoiceRequest request) {
                List<InvoiceItemInput> itemInputs = request.items().stream()
                                .map(i -> new InvoiceItemInput(i.description(), i.quantity(), i.unitPrice()))
                                .toList();

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(
                                InvoiceResponse.fromDomain(invoiceApplicationService.createDelivery(
                                                request.buyerName(),
                                                itemInputs)), clock));
        }

        @Operation(
                summary = "Cancelar factura",
                description = "Marca la factura como cancelada (irreversible). El registro se conserva para auditoría. Endpoint público."
        )
        @ApiResponses({
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Factura cancelada",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"data":{"id":14,"is_cancelled":true},"timestamp":"2026-01-28T10:00:00"}
                                        """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Factura no encontrada",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"code":"INVOICE_NOT_FOUND","message":"Invoice with ID 99 does not exist","timestamp":"2026-01-28T10:00:00"}
                                        """))),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Factura ya estaba cancelada",
                        content = @Content(examples = @ExampleObject(
                                value = """
                                        {"code":"INVOICE_ALREADY_CANCELLED","message":"Invoice 14 is already cancelled","timestamp":"2026-01-28T10:00:00"}
                                        """)))
        })
        @PatchMapping("/{id}/cancel")
        public ResponseEntity<ApiResponse<InvoiceCancelResponse>> cancel(
                        @Parameter(description = "ID de la factura.", example = "14")
                        @PathVariable("id") Long invoiceId) {
                return ResponseEntity.ok(ApiResponse.of(
                                InvoiceCancelResponse.fromDomain(invoiceApplicationService.cancel(invoiceId)), clock));
        }
}