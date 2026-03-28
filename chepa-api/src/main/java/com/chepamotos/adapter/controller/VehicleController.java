package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.VehicleResponse;
import com.chepamotos.infrastructure.application.VehicleApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/vehicles")
public class VehicleController {

    private final VehicleApplicationService vehicleApplicationService;

    public VehicleController(VehicleApplicationService vehicleApplicationService) {
        this.vehicleApplicationService = vehicleApplicationService;
    }

        @Operation(
            summary = "Lookup vehicle by plate",
            description = "Returns a vehicle by plate. Plate lookup is normalized (trim + uppercase) before querying."
        )
        @ApiResponses(value = {
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Vehicle found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "data": {
                            "id": 1,
                            "plate": "BXR42H",
                            "model": "Boxer 150 2021"
                              },
                              "timestamp": "2026-03-21T15:00:00"
                            }
                            """
                    )
                )
            ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "404",
                description = "Plate not found",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "code": "VEHICLE_NOT_FOUND",
                              "message": "Vehicle not found with plate: ABC123",
                              "timestamp": "2026-03-21T15:00:00"
                            }
                            """
                    )
                )
            ),
                @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                description = "Validation error",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "code": "VALIDATION_ERROR",
                              "message": "Vehicle plate cannot be blank",
                              "timestamp": "2026-03-21T15:00:00"
                            }
                            """
                    )
                )
            )
        })
    @GetMapping("/plate/{plate}")
        public ResponseEntity<ApiResponse<VehicleResponse>> getByPlate(
            @Parameter(description = "Vehicle plate", example = "bxr42h")
            @PathVariable("plate") String plate
        ) {
        VehicleResponse data = VehicleResponse.fromDomain(vehicleApplicationService.getByPlate(plate));
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
