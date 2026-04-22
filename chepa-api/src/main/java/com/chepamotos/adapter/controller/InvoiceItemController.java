package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.InvoiceItemSuggestionDTO;
import com.chepamotos.domain.port.in.InvoiceApplicationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@Validated
@RequestMapping("/api/invoice-items")
public class InvoiceItemController {

    private final InvoiceApplicationUseCase invoiceApplicationService;

    public InvoiceItemController(InvoiceApplicationUseCase invoiceApplicationService) {
        this.invoiceApplicationService = invoiceApplicationService;
    }

    @Operation(
        summary = "Get autocomplete suggestions for invoice items",
        description = "Returns invoice item suggestions (description + price) filtered by vehicle model and item description prefix. " +
                      "Only searches active (non-cancelled) SERVICE invoices. " +
                      "Results are ordered by frequency (how often used) and limited to 10 items."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "200",
                description = "Suggestions found or empty array if no matches",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "data": [
                                {
                                  "description": "Freno Delantero",
                                  "unit_price": 45000.00
                                },
                                {
                                  "description": "Freno Trasero",
                                  "unit_price": 38000.00
                                }
                              ],
                              "timestamp": "2026-04-21T14:30:00"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "400",
                                description = "Missing or invalid query parameters",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "code": "VALIDATION_ERROR",
                                                            "message": "suggestions.q: Search query must be at least 2 characters",
                              "timestamp": "2026-04-21T14:30:00"
                            }
                            """
                    )
                )
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                responseCode = "500",
                description = "Unexpected server error",
                content = @Content(
                    mediaType = "application/json",
                    examples = @ExampleObject(
                        value = """
                            {
                              "code": "INTERNAL_ERROR",
                              "message": "An unexpected error occurred",
                              "timestamp": "2026-04-21T14:30:00"
                            }
                            """
                    )
                )
            )
    })
    @GetMapping("/suggestions")
    public ResponseEntity<ApiResponse<List<InvoiceItemSuggestionDTO>>> suggestions(
            @Parameter(
                description = "Vehicle model prefix to filter suggestions. " +
                              "Matched case-insensitively against vehicle models in past invoices.",
                example = "Boxer 150",
                required = true
            )
            @RequestParam("model") @NotBlank(message = "Model is required") String model,
            @Parameter(
                description = "Description prefix to search for. Minimum 2 characters. " +
                              "Matched case-insensitively using prefix search (ILIKE).",
                example = "Fre",
                required = true
            )
            @RequestParam("q")
            @NotBlank(message = "Search query is required")
            @Size(min = 2, message = "Search query must be at least 2 characters")
            String q) {
        List<InvoiceItemSuggestionDTO> data = invoiceApplicationService.findSuggestions(model, q)
                .stream()
                .map(InvoiceItemSuggestionDTO::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
