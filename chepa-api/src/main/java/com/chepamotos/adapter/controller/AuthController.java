package com.chepamotos.adapter.controller;

import com.chepamotos.adapter.dto.ApiResponse;
import com.chepamotos.adapter.dto.AuthTokensResponse;
import com.chepamotos.adapter.dto.LoginRequest;
import com.chepamotos.adapter.dto.RefreshTokenRequest;
import com.chepamotos.domain.port.in.AuthApplicationUseCase;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.util.Map;

@Tag(name = "Autenticación")
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthApplicationUseCase authApplicationService;
    private final Clock clock;

    public AuthController(AuthApplicationUseCase authApplicationService, Clock clock) {
        this.authApplicationService = authApplicationService;
        this.clock = clock;
    }

    @Operation(
            summary = "Iniciar sesión",
            description = "Autentica con usuario y contraseña (BCrypt). Retorna un access token JWT " +
                          "(expira en ~15 min) y un refresh token de larga duración. " +
                          "Incluir el access token en endpoints protegidos: `Authorization: Bearer <accessToken>`."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Login exitoso",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {
                                      "data": {
                                        "tokenType": "Bearer",
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "expiresIn": 900,
                                        "refreshToken": "3f2a1b4c-8d9e-4f5a-b6c7-d8e9f0a1b2c3"
                                      },
                                      "timestamp": "2026-01-28T10:00:00"
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Campos faltantes o inválidos",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"VALIDATION_ERROR","message":"username: username is required","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Credenciales incorrectas",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"INVALID_CREDENTIALS","message":"Invalid username or password","timestamp":"2026-01-28T10:00:00"}
                                    """)))
    })
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> login(@Valid @RequestBody LoginRequest request) {
        var tokens = authApplicationService.login(request.username(), request.password());
        return ResponseEntity.ok(ApiResponse.of(AuthTokensResponse.fromDomain(tokens), clock));
    }

    @Operation(
            summary = "Renovar sesión",
            description = "Intercambia un refresh token válido por un nuevo par access + refresh. " +
                          "El refresh token anterior queda revocado (rotación de token)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Tokens renovados exitosamente",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {
                                      "data": {
                                        "tokenType": "Bearer",
                                        "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
                                        "expiresIn": 900,
                                        "refreshToken": "9a8b7c6d-1e2f-3a4b-5c6d-7e8f9a0b1c2d"
                                      },
                                      "timestamp": "2026-01-28T10:15:00"
                                    }
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Campos faltantes o inválidos",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"VALIDATION_ERROR","message":"refreshToken: refresh_token is required","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token inválido o expirado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"SESSION_EXPIRED","message":"Refresh token has expired","timestamp":"2026-01-28T10:00:00"}
                                    """)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<AuthTokensResponse>> refresh(@Valid @RequestBody RefreshTokenRequest request) {
        var tokens = authApplicationService.refresh(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.of(AuthTokensResponse.fromDomain(tokens), clock));
    }

    @Operation(
            summary = "Cerrar sesión",
            description = "Revoca el refresh token. El access token en circulación sigue válido " +
                          "hasta su expiración natural (~15 min)."
    )
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Sesión cerrada",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"data":{"message":"Session closed"},"timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Campos faltantes o inválidos",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"VALIDATION_ERROR","message":"refreshToken: refresh_token is required","timestamp":"2026-01-28T10:00:00"}
                                    """))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Refresh token inválido o ya revocado",
                    content = @Content(examples = @ExampleObject(
                            value = """
                                    {"code":"AUTH_REQUIRED","message":"Invalid refresh token","timestamp":"2026-01-28T10:00:00"}
                                    """)))
    })
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<Map<String, String>>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authApplicationService.logout(request.refreshToken());
        return ResponseEntity.ok(ApiResponse.of(Map.of("message", "Session closed"), clock));
    }
}
