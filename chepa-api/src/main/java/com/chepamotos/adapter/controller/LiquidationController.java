package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateLiquidationRequest;
import com.chepamotos.adapter.dto.LiquidationResponse;
import com.chepamotos.domain.port.in.LiquidationApplicationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;

@Tag(name = "Liquidaciones")
@RestController
@RequestMapping("/api/liquidations")
public class LiquidationController {

    private final LiquidationApplicationUseCase liquidationApplicationService;
    private final Clock clock;

    public LiquidationController(LiquidationApplicationUseCase liquidationApplicationService, Clock clock) {
        this.liquidationApplicationService = liquidationApplicationService;
        this.clock = clock;
    }

    @Operation(
            summary = "Listar liquidaciones",
            description = "Retorna liquidaciones filtradas por mecánico y/o fecha. Endpoint público."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de liquidaciones (puede ser vacía)",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {
                                      "data": [{
                                        "id": 8,
                                        "mechanic": {"id":1,"name":"Jose","is_active":true},
                                        "date": "2026-01-28",
                                        "invoice_count": 4,
                                        "total_revenue": 304100.00,
                                        "mechanic_share": 212870.00,
                                        "shop_share": 91230.00
                                      }],
                                      "timestamp": "2026-01-28T10:00:00"
                                    }
                                    """)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<LiquidationResponse>>> list(
            @Parameter(description = "Filtrar por ID de mecánico.", example = "1")
            @RequestParam(name = "mechanic_id", required = false) Long mechanicId,
            @Parameter(description = "Filtrar por fecha (YYYY-MM-DD).", example = "2026-01-28")
            @RequestParam(name = "date", required = false) LocalDate date) {
        List<LiquidationResponse> data = liquidationApplicationService.list(mechanicId, date)
                .stream()
                .map(LiquidationResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(data, clock));
    }

    @Operation(
            summary = "Ejecutar liquidación diaria",
            description = """
                    Calcula y registra la liquidación del día para uno o todos los mecánicos. **Requiere rol GERENTE.**

                    - `mechanic_id` presente → liquida solo ese mecánico.
                    - `mechanic_id` null → liquida todos los mecánicos con facturas SERVICE activas en esa fecha.

                    **Cálculos:** `total_revenue` = suma de `labor_amount` de facturas SERVICE activas.
                    `mechanic_share` = 70%, `shop_share` = 30%. Facturas DELIVERY excluidas.
                    """
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Liquidaciones creadas",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {
                                      "data": [{
                                        "id": 8,
                                        "mechanic": {"id":1,"name":"Jose","is_active":true},
                                        "date": "2026-01-28",
                                        "invoice_count": 4,
                                        "total_revenue": 304100.00,
                                        "mechanic_share": 212870.00,
                                        "shop_share": 91230.00
                                      }],
                                      "timestamp": "2026-01-28T10:00:00"
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error de validación (falta la fecha)",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"VALIDATION_ERROR","message":"date: date is required","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Token ausente o inválido",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"AUTH_REQUIRED","message":"Authentication is required","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "El usuario autenticado no tiene rol GERENTE",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"FORBIDDEN","message":"You do not have permission to perform this action","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mecánico no encontrado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"MECHANIC_NOT_FOUND","message":"Mechanic with ID 99 does not exist","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Ya existe liquidación para ese mecánico y fecha",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"LIQUIDATION_ALREADY_EXISTS","message":"Liquidation already exists for mechanic 1 on 2026-01-28","timestamp":"2026-01-28T10:00:00"}
                                    """)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<List<LiquidationResponse>>> create(
            @Valid @RequestBody CreateLiquidationRequest request) {
        List<LiquidationResponse> data = liquidationApplicationService.create(request.date(), request.mechanicId())
                .stream()
                .map(LiquidationResponse::fromDomain)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data, clock));
    }
}