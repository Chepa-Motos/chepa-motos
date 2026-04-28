package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.model.AuthTokens;
import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceItemInput;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.UserRole;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.usecase.auth.LoginUseCase;
import com.chepamotos.domain.usecase.auth.LogoutUseCase;
import com.chepamotos.domain.usecase.auth.RefreshSessionUseCase;
import com.chepamotos.domain.usecase.invoice.CancelInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateDeliveryInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateServiceInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.FindInvoiceItemSuggestionsUseCase;
import com.chepamotos.domain.usecase.invoice.GetInvoiceByIdUseCase;
import com.chepamotos.domain.usecase.invoice.ListInvoicesUseCase;
import com.chepamotos.domain.usecase.liquidation.CreateLiquidationUseCase;
import com.chepamotos.domain.usecase.liquidation.ListLiquidationsUseCase;
import com.chepamotos.domain.usecase.mechanic.ChangeMechanicStatusUseCase;
import com.chepamotos.domain.usecase.mechanic.CreateMechanicUseCase;
import com.chepamotos.domain.usecase.mechanic.GetMechanicByIdUseCase;
import com.chepamotos.domain.usecase.mechanic.ListMechanicsUseCase;
import com.chepamotos.domain.usecase.vehicle.GetVehicleByPlateUseCase;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;
import com.chepamotos.infrastructure.config.AuthUseCaseConfig;
import com.chepamotos.infrastructure.config.SecurityConfig;
import com.chepamotos.infrastructure.security.ApiAccessDeniedHandler;
import com.chepamotos.infrastructure.security.ApiAuthenticationEntryPoint;
import com.chepamotos.infrastructure.security.JwtAuthenticationFilter;
import com.chepamotos.infrastructure.security.JwtTokenDecoder;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.AccessTokenService;
import com.chepamotos.domain.port.PasswordHasher;
import com.chepamotos.domain.port.RefreshTokenRepository;
import com.chepamotos.domain.port.TokenHashService;
import com.chepamotos.domain.port.VehicleRepository;
import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.DailyLiquidationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InfrastructureWiringTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);

    @Mock private AppUserRepository appUserRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordHasher passwordHasher;
    @Mock private TokenHashService tokenHashService;
    @Mock private AccessTokenService accessTokenService;

    @Mock private ListInvoicesUseCase listInvoicesUseCase;
    @Mock private GetInvoiceByIdUseCase getInvoiceByIdUseCase;
    @Mock private CancelInvoiceUseCase cancelInvoiceUseCase;
    @Mock private CreateServiceInvoiceUseCase createServiceInvoiceUseCase;
    @Mock private CreateDeliveryInvoiceUseCase createDeliveryInvoiceUseCase;
    @Mock private FindInvoiceItemSuggestionsUseCase findInvoiceItemSuggestionsUseCase;

    @Mock private ListMechanicsUseCase listMechanicsUseCase;
    @Mock private GetMechanicByIdUseCase getMechanicByIdUseCase;
    @Mock private CreateMechanicUseCase createMechanicUseCase;
    @Mock private ChangeMechanicStatusUseCase changeMechanicStatusUseCase;

    @Mock private GetVehicleByPlateUseCase getVehicleByPlateUseCase;
    @Mock private ResolveVehicleForServiceInvoiceUseCase resolveVehicleForServiceInvoiceUseCase;

    @Mock private ListLiquidationsUseCase listLiquidationsUseCase;
    @Mock private CreateLiquidationUseCase createLiquidationUseCase;

    @Test
    void authApplicationService_delegatesToUseCases() {
        AuthTokens tokens = AuthTokens.of(new AccessToken("access.token", 900), "refresh.token");
        LoginUseCase loginUseCase = org.mockito.Mockito.mock(LoginUseCase.class);
        RefreshSessionUseCase refreshSessionUseCase = org.mockito.Mockito.mock(RefreshSessionUseCase.class);
        LogoutUseCase logoutUseCase = org.mockito.Mockito.mock(LogoutUseCase.class);
        when(loginUseCase.execute("admin", "secret")).thenReturn(tokens);
        when(refreshSessionUseCase.execute("refresh-token")).thenReturn(tokens);

        AuthApplicationService service = new AuthApplicationService(loginUseCase, refreshSessionUseCase, logoutUseCase);

        assertEquals(tokens, service.login("admin", "secret"));
        assertEquals(tokens, service.refresh("refresh-token"));
        service.logout("refresh-token");

        verify(loginUseCase).execute("admin", "secret");
        verify(refreshSessionUseCase).execute("refresh-token");
        verify(logoutUseCase).execute("refresh-token");
    }

    @Test
    void invoiceApplicationService_delegatesToUseCases() {
        Invoice invoice = sampleServiceInvoice();
        List<InvoiceItem> suggestions = List.of(sampleInvoiceItem());
        when(listInvoicesUseCase.execute(LocalDate.of(2026, 1, 28), InvoiceType.SERVICE, 1L, false)).thenReturn(List.of(invoice));
        when(getInvoiceByIdUseCase.execute(99L)).thenReturn(invoice);
        when(cancelInvoiceUseCase.execute(99L)).thenReturn(invoice.cancel());
        when(createServiceInvoiceUseCase.execute(any(), any(), any(), any(), any())).thenReturn(invoice);
        when(createDeliveryInvoiceUseCase.execute(any(), any())).thenReturn(sampleDeliveryInvoice());
        when(findInvoiceItemSuggestionsUseCase.execute("Boxer", "Fr")).thenReturn(suggestions);

        InvoiceApplicationService service = new InvoiceApplicationService(
                listInvoicesUseCase,
                getInvoiceByIdUseCase,
                cancelInvoiceUseCase,
                createServiceInvoiceUseCase,
                createDeliveryInvoiceUseCase,
                findInvoiceItemSuggestionsUseCase);

        assertEquals(List.of(invoice), service.list(LocalDate.of(2026, 1, 28), InvoiceType.SERVICE, 1L, false));
        assertEquals(invoice, service.getById(99L));
        Invoice cancelled = service.cancel(99L);
        assertEquals(99L, cancelled.id());
        assertEquals(InvoiceType.SERVICE, cancelled.type());
        assertTrue(cancelled.cancelled());

        Invoice createdService = service.createService(1L, "BXR74F", "Boxer", new BigDecimal("65000"), List.of(sampleInvoiceItemInput()));
        Invoice createdDelivery = service.createDelivery("Buyer", List.of(sampleInvoiceItemInput()));

        assertEquals(InvoiceType.SERVICE, createdService.type());
        assertEquals(1L, createdService.mechanic().id());
        assertEquals("BXR74F", createdService.vehicle().plate());
        assertEquals(InvoiceType.DELIVERY, createdDelivery.type());
        assertEquals("Buyer", createdDelivery.buyerName());
        assertEquals(suggestions, service.findSuggestions("Boxer", "Fr"));
    }

    @Test
    void mechanicApplicationService_delegatesToUseCases() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        when(listMechanicsUseCase.execute(true)).thenReturn(List.of(mechanic));
        when(getMechanicByIdUseCase.execute(1L)).thenReturn(mechanic);
        when(createMechanicUseCase.execute("Carlos")).thenReturn(mechanic);
        when(changeMechanicStatusUseCase.execute(1L, false)).thenReturn(mechanic.withStatus(false));

        MechanicApplicationService service = new MechanicApplicationService(
                listMechanicsUseCase,
                getMechanicByIdUseCase,
                createMechanicUseCase,
                changeMechanicStatusUseCase);

        assertEquals(List.of(mechanic), service.listByActive(true));
        assertEquals(mechanic, service.getById(1L));

        Mechanic created = service.create("Carlos");
        Mechanic updated = service.changeStatus(1L, false);

        assertEquals(1L, created.id());
        assertEquals("Jose", created.name());
        assertEquals(1L, updated.id());
        assertFalse(updated.active());
    }

    @Test
    void vehicleApplicationService_delegatesToUseCases() {
        Vehicle vehicle = Vehicle.restore(1L, "BXR74F", "Boxer");
        when(getVehicleByPlateUseCase.execute("BXR74F")).thenReturn(vehicle);
        when(resolveVehicleForServiceInvoiceUseCase.execute("BXR74F", "Boxer 2021")).thenReturn(vehicle);

        VehicleApplicationService service = new VehicleApplicationService(
                getVehicleByPlateUseCase,
                resolveVehicleForServiceInvoiceUseCase);

        assertEquals(vehicle, service.getByPlate("BXR74F"));
        assertEquals(vehicle, service.resolveForServiceInvoice("BXR74F", "Boxer 2021"));
    }

    @Test
    void liquidationApplicationService_delegatesToUseCases() {
        DailyLiquidation liquidation = sampleLiquidation();
        when(listLiquidationsUseCase.execute(1L, LocalDate.of(2026, 1, 28))).thenReturn(List.of(liquidation));
        when(createLiquidationUseCase.execute(LocalDate.of(2026, 1, 28), 1L)).thenReturn(List.of(liquidation));

        LiquidationApplicationService service = new LiquidationApplicationService(listLiquidationsUseCase, createLiquidationUseCase);

        assertEquals(List.of(liquidation), service.list(1L, LocalDate.of(2026, 1, 28)));
        assertEquals(List.of(liquidation), service.create(LocalDate.of(2026, 1, 28), 1L));
    }

    @Test
    void authUseCaseConfig_createsUseCases() {
        AuthUseCaseConfig config = new AuthUseCaseConfig();

        assertNotNull(config.loginUseCase(appUserRepository, refreshTokenRepository, passwordHasher, tokenHashService, accessTokenService, FIXED_CLOCK, 7L));
        assertNotNull(config.refreshSessionUseCase(appUserRepository, refreshTokenRepository, tokenHashService, accessTokenService, FIXED_CLOCK, 7L));
        assertNotNull(config.logoutUseCase(refreshTokenRepository, tokenHashService, FIXED_CLOCK));
    }

    @Test
    void securityConfig_createsSecurityBeans() throws Exception {
        SecurityConfig config = new SecurityConfig();
        JwtTokenDecoder jwtTokenDecoder = org.mockito.Mockito.mock(JwtTokenDecoder.class);

        JwtAuthenticationFilter filter = config.jwtAuthenticationFilter(jwtTokenDecoder, appUserRepository);
        ApiAuthenticationEntryPoint entryPoint = config.apiAuthenticationEntryPoint(FIXED_CLOCK);
        ApiAccessDeniedHandler accessDeniedHandler = config.apiAccessDeniedHandler(FIXED_CLOCK);

        assertNotNull(filter);
        assertNotNull(entryPoint);
        assertNotNull(accessDeniedHandler);
    }

    private static Invoice sampleServiceInvoice() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        Vehicle vehicle = Vehicle.restore(2L, "BXR74F", "Boxer");
        List<InvoiceItem> items = List.of(sampleInvoiceItem());
        return Invoice.restore(99L, InvoiceType.SERVICE, mechanic, vehicle, null, LocalDateTime.of(2026, 1, 28, 10, 0), new BigDecimal("65000"), false, items);
    }

    private static Invoice sampleDeliveryInvoice() {
        return Invoice.restore(100L, InvoiceType.DELIVERY, null, null, "Buyer", LocalDateTime.of(2026, 1, 28, 11, 0), BigDecimal.ZERO, false, List.of(sampleInvoiceItem()));
    }

    private static InvoiceItem sampleInvoiceItem() {
        return InvoiceItem.restore(51L, "Freno", BigDecimal.ONE, new BigDecimal("3900"));
    }

    private static InvoiceItemInput sampleInvoiceItemInput() {
        return new InvoiceItemInput("Freno", BigDecimal.ONE, new BigDecimal("3900"));
    }

    private static DailyLiquidation sampleLiquidation() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        return DailyLiquidation.create(mechanic, LocalDate.of(2026, 1, 28), new BigDecimal("100000"), 2);
    }
}