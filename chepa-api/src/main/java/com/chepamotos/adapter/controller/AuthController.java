package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.AuthTokensResponse;
import com.chepamotos.adapter.dto.LoginRequest;
import com.chepamotos.adapter.dto.RefreshTokenRequest;
import com.chepamotos.domain.port.in.AuthApplicationUseCase;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationUseCase authApplicationService;
    private final Clock clock;

    public AuthController(AuthApplicationUseCase authApplicationService, Clock clock) {
        this.authApplicationService = authApplicationService;
        this.clock = clock;
    }

    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authApplicationService.login(request.username(), request.password());
        return ResponseEntity.ok(ApiResponse.of(AuthTokensResponse.fromDomain(tokens), clock));
    }

    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens = authApplicationService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.of(AuthTokensResponse.fromDomain(tokens), clock));
    }

    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authApplicationService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Session closed"), clock));
    }
}
