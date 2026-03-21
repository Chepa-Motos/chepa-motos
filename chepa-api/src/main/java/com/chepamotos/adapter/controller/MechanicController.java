package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.CreateMechanicRequest;
import com.chepamotos.adapter.dto.MechanicResponse;
import com.chepamotos.adapter.dto.UpdateMechanicStatusRequest;
import com.chepamotos.domain.service.MechanicService;
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

import java.util.List;

@RestController
@RequestMapping("/api/mechanics")
public class MechanicController {

    private final MechanicService mechanicService;

    public MechanicController(MechanicService mechanicService) {
        this.mechanicService = mechanicService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<MechanicResponse>>> list(
            @RequestParam(name = "active", defaultValue = "true") boolean active
    ) {
        List<MechanicResponse> data = mechanicService.listByActive(active)
                .stream()
                .map(MechanicResponse::fromDomain)
                .toList();
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<MechanicResponse>> getById(@PathVariable("id") Long mechanicId) {
        MechanicResponse data = MechanicResponse.fromDomain(mechanicService.getById(mechanicId));
        return ResponseEntity.ok(ApiResponse.of(data));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MechanicResponse>> create(
            @Valid @RequestBody CreateMechanicRequest request
    ) {
        MechanicResponse data = MechanicResponse.fromDomain(mechanicService.create(request.name()));
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.of(data));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<MechanicResponse>> changeStatus(
            @PathVariable("id") Long mechanicId,
            @Valid @RequestBody UpdateMechanicStatusRequest request
    ) {
        MechanicResponse data = MechanicResponse.fromDomain(
                mechanicService.changeStatus(mechanicId, request.isActive())
        );
        return ResponseEntity.ok(ApiResponse.of(data));
    }
}
