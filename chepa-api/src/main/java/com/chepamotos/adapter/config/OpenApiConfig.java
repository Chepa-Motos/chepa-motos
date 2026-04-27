package com.chepamotos.adapter.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.tags.Tag;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chepaApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chepa Motos API")
                        .description("""
                                REST API para facturación y gestión operativa de Chepa Motos, \
                                taller de motocicletas en Medellín, Colombia.

                                ## Autenticación

                                La API usa **JWT Bearer** con refresh tokens.

                                **Flujo:**
                                1. `POST /api/auth/login` → recibe `accessToken` (JWT, expira en ~15 min) \
                                y `refreshToken` (UUID, larga duración).
                                2. Incluir el access token en cada petición protegida: \
                                `Authorization: Bearer <accessToken>`
                                3. Cuando el access token expira → `POST /api/auth/refresh` con el refresh token.
                                4. Para cerrar sesión → `POST /api/auth/logout` con el refresh token.

                                **Endpoints protegidos** (requieren rol `GERENTE`):
                                - `POST /api/mechanics`
                                - `PATCH /api/mechanics/{id}/status`
                                - `POST /api/liquidations`

                                El resto de endpoints son **públicos** (no requieren token).
                                """)
                        .version("v1")
                        .contact(new Contact().name("Chepa Motos Team")))
                .components(new Components()
                        .addSecuritySchemes("bearerAuth", new SecurityScheme()
                                .name("bearerAuth")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")
                                .description("JWT obtenido en POST /api/auth/login. " +
                                             "Encabezado: Authorization: Bearer <token>")))
                .tags(List.of(
                        new Tag().name("Autenticación")
                                .description("Login, refresh de sesión y logout. " +
                                             "Todos los endpoints de este grupo son públicos."),
                        new Tag().name("Mecánicos")
                                .description("Gestión de mecánicos. " +
                                             "Crear y cambiar estado requieren rol GERENTE."),
                        new Tag().name("Vehículos")
                                .description("Consulta de vehículos por placa. " +
                                             "La creación ocurre automáticamente al crear una factura de servicio."),
                        new Tag().name("Facturas")
                                .description("Creación, consulta y cancelación de facturas de servicio y entrega. " +
                                             "Todos los endpoints son públicos."),
                        new Tag().name("Ítems de factura")
                                .description("Autocompletado de ítems a partir del historial de facturas."),
                        new Tag().name("Liquidaciones")
                                .description("Liquidaciones diarias por mecánico (70% mecánico / 30% taller). " +
                                             "Crear liquidación requiere rol GERENTE.")
                ));
    }
}
