package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateLiquidationRequest;
import com.chepamotos.adapter.dto.LiquidationResponse;
import com.chepamotos.infrastructure.application.LiquidationApplicationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/liquidations")
public class LiquidationController {

    private final LiquidationApplicationService liquidationApplicationService;

    public LiquidationController(LiquidationApplicationService liquidationApplicationService) {
        this.liquidationApplicationService = liquidationApplicationService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<LiquidationResponse>>> list(
            @RequestParam(name = "mechanic_id", required = false) Long mechanicId,
            @RequestParam(name = "date", required = false) LocalDate date) {
        List<LiquidationResponse> data = liquidationApplicationService.list(mechanicId, date)
                .stream()
                .map(LiquidationResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<List<LiquidationResponse>>> create(
            @Valid @RequestBody CreateLiquidationRequest request) {
        List<LiquidationResponse> data = liquidationApplicationService.create(request.date(), request.mechanicId())
                .stream()
                .map(LiquidationResponse::fromDomain)
                .toList();
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }
}