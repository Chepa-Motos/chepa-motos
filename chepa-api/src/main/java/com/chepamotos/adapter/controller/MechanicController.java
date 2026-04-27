package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateMechanicRequest;
import com.chepamotos.adapter.dto.MechanicResponse;
import com.chepamotos.adapter.dto.UpdateMechanicStatusRequest;
import com.chepamotos.domain.port.in.MechanicApplicationUseCase;
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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.List;

@Tag(name = "Mecánicos")
@RestController
@RequestMapping("/api/mechanics")
public class MechanicController {

    private final MechanicApplicationUseCase mechanicApplicationService;
    private final Clock clock;

    public MechanicController(MechanicApplicationUseCase mechanicApplicationService, Clock clock) {
        this.mechanicApplicationService = mechanicApplicationService;
        this.clock = clock;
    }

    @Operation(
            summary = "Listar mecánicos",
            description = "Retorna mecánicos filtrados por estado activo/inactivo. Endpoint público."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Lista de mecánicos (puede ser vacía)",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"data":[{"id":1,"name":"Jose","is_active":true}],"timestamp":"2026-01-28T10:00:00"}
                                    """)))
    })
    @GetMapping
    public ResponseEntity<ApiResponse<List<MechanicResponse>>> list(
            @Parameter(description = "Filtrar por estado. true = activos, false = inactivos.", example = "true")
            @RequestParam(name = "active", defaultValue = "true") boolean active
    ) {
        List<MechanicResponse> data = mechanicApplicationService.listByActive(active)
                .stream()
                .map(MechanicResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(data, clock));
    }

    @Operation(summary = "Obtener mecánico por ID", description = "Endpoint público.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Mecánico encontrado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"data":{"id":1,"name":"Jose","is_active":true},"timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Mecánico no encontrado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"MECHANIC_NOT_FOUND","message":"Mechanic with ID 99 does not exist","timestamp":"2026-01-28T10:00:00"}
                                    """)))
    })
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MechanicResponse>> getById(
            @Parameter(description = "ID del mecánico.", example = "1")
            @PathVariable("id") Long mechanicId) {
        MechanicResponse data = MechanicResponse.fromDomain(mechanicApplicationService.getById(mechanicId));
        return ResponseEntity.ok(ApiResponse.of(data, clock));
    }

    @Operation(
            summary = "Crear mecánico",
            description = "Registra un nuevo mecánico activo. **Requiere rol GERENTE.**"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Mecánico creado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"data":{"id":6,"name":"Carlos Rueda","is_active":true},"timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error de validación",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"VALIDATION_ERROR","message":"name: name is required","timestamp":"2026-01-28T10:00:00"}
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
                                    """)))
    })
    @PostMapping
    public ResponseEntity<ApiResponse<MechanicResponse>> create(
            @Valid @RequestBody CreateMechanicRequest request
    ) {
        MechanicResponse data = MechanicResponse.fromDomain(mechanicApplicationService.create(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data, clock));
    }

    @Operation(
            summary = "Cambiar estado del mecánico",
            description = "Activa o desactiva un mecánico. Un mecánico inactivo no puede ser asignado a nuevas facturas. " +
                          "**Requiere rol GERENTE.**"
    )
    @SecurityRequirement(name = "bearerAuth")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Estado actualizado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"data":{"id":1,"name":"Jose","is_active":false},"timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Error de validación",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"VALIDATION_ERROR","message":"is_active: is_active is required","timestamp":"2026-01-28T10:00:00"}
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
                                    """)))
    })
    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MechanicResponse>> changeStatus(
            @Parameter(description = "ID del mecánico.", example = "1")
            @PathVariable("id") Long mechanicId,
            @Valid @RequestBody UpdateMechanicStatusRequest request
    ) {
        MechanicResponse data = MechanicResponse.fromDomain(
            mechanicApplicationService.changeStatus(mechanicId, request.isActive())
        );
        return ResponseEntity.ok(ApiResponse.of(data, clock));
    }
}
