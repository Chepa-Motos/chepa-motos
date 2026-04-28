package com.chepamotos.infrastructure.mapper;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class InfrastructureMapperTest {

    @Test
    void appUserMapper_roundTripAndNulls() {
        var domain = com.chepamotos.domain.model.AppUser.restore(7L, "admin", "hash", com.chepamotos.domain.model.UserRole.GERENTE, true, LocalDateTime.of(2026, 1, 28, 10, 0));

        assertNull(AppUserEntityMapper.toDomain(null));
        assertNull(AppUserEntityMapper.toEntity(null));

        var entity = AppUserEntityMapper.toEntity(domain);
        var roundTrip = AppUserEntityMapper.toDomain(entity);

        assertEquals(domain.id(), roundTrip.id());
        assertEquals(domain.username(), roundTrip.username());
        assertEquals(domain.passwordHash(), roundTrip.passwordHash());
        assertEquals(domain.role(), roundTrip.role());
        assertEquals(domain.active(), roundTrip.active());
    }

    @Test
    void mechanicMapper_roundTripAndNulls() {
        var domain = com.chepamotos.domain.model.Mechanic.restore(1L, "Jose", true);

        assertNull(MechanicEntityMapper.toDomain(null));
        assertNull(MechanicEntityMapper.toEntity(null));

        var entity = MechanicEntityMapper.toEntity(domain);
        var roundTrip = MechanicEntityMapper.toDomain(entity);

        assertEquals(domain.id(), roundTrip.id());
        assertEquals(domain.name(), roundTrip.name());
        assertEquals(domain.active(), roundTrip.active());
    }

    @Test
    void vehicleMapper_roundTripAndNulls() {
        var domain = com.chepamotos.domain.model.Vehicle.restore(1L, "BXR74F", "Boxer 150");

        assertNull(VehicleEntityMapper.toDomain(null));
        assertNull(VehicleEntityMapper.toEntity(null));

        var entity = VehicleEntityMapper.toEntity(domain);
        var roundTrip = VehicleEntityMapper.toDomain(entity);

        assertEquals(domain.id(), roundTrip.id());
        assertEquals(domain.plate(), roundTrip.plate());
        assertEquals(domain.model(), roundTrip.model());
    }

    @Test
    void invoiceItemMapper_roundTripAndNulls() {
        var invoiceEntity = new com.chepamotos.infrastructure.entity.Invoice();
        var domain = com.chepamotos.domain.model.InvoiceItem.restore(51L, "Freno", new BigDecimal("2"), new BigDecimal("3900"));

        assertNull(InvoiceItemEntityMapper.toDomain(null));
        assertNull(InvoiceItemEntityMapper.toEntity(null, invoiceEntity));

        var entity = InvoiceItemEntityMapper.toEntity(domain, invoiceEntity);
        var roundTrip = InvoiceItemEntityMapper.toDomain(entity);

        assertEquals(domain.id(), roundTrip.id());
        assertEquals(domain.description(), roundTrip.description());
        assertEquals(domain.quantity(), roundTrip.quantity());
        assertEquals(domain.unitPrice(), roundTrip.unitPrice());
    }

    @Test
    void invoiceMapper_roundTripAndNulls() {
        var mechanic = com.chepamotos.domain.model.Mechanic.restore(1L, "Jose", true);
        var vehicle = com.chepamotos.domain.model.Vehicle.restore(2L, "BXR74F", "Boxer 150");
        var items = List.of(com.chepamotos.domain.model.InvoiceItem.restore(51L, "Freno", new BigDecimal("1"), new BigDecimal("3900")));
        var domain = com.chepamotos.domain.model.Invoice.restore(99L, com.chepamotos.domain.model.InvoiceType.SERVICE, mechanic, vehicle, null, LocalDateTime.of(2026, 1, 28, 10, 0), new BigDecimal("65000"), false, items);

        assertNull(InvoiceEntityMapper.toDomain(null));
        assertNull(InvoiceEntityMapper.toEntity(null));

        var entity = InvoiceEntityMapper.toEntity(domain);
        var roundTrip = InvoiceEntityMapper.toDomain(entity);

        assertEquals(domain.id(), roundTrip.id());
        assertEquals(domain.type(), roundTrip.type());
        assertEquals(domain.mechanic().name(), roundTrip.mechanic().name());
        assertEquals(domain.vehicle().plate(), roundTrip.vehicle().plate());
        assertEquals(domain.items().size(), roundTrip.items().size());
    }

    @Test
    void dailyLiquidationMapper_roundTripAndNulls() {
        var mechanic = com.chepamotos.domain.model.Mechanic.restore(1L, "Jose", true);
        var domain = com.chepamotos.domain.model.DailyLiquidation.create(mechanic, LocalDate.of(2026, 1, 28), new BigDecimal("100000"), 2);

        assertNull(DailyLiquidationEntityMapper.toDomain(null));
        assertNull(DailyLiquidationEntityMapper.toEntity(null));

        var entity = DailyLiquidationEntityMapper.toEntity(domain);
        var roundTrip = DailyLiquidationEntityMapper.toDomain(entity);

        assertEquals(domain.id(), roundTrip.id());
        assertEquals(domain.mechanic().name(), roundTrip.mechanic().name());
        assertEquals(domain.date(), roundTrip.date());
        assertEquals(domain.totalRevenue(), roundTrip.totalRevenue());
        assertEquals(domain.invoiceCount(), roundTrip.invoiceCount());
    }

    @Test
    void refreshTokenMapper_toDomainAndNulls() {
        var userEntity = new com.chepamotos.infrastructure.entity.AppUser();
        userEntity.setId(7L);
        userEntity.setUsername("admin");
        userEntity.setPasswordHash("hash");
        userEntity.setRole(com.chepamotos.domain.model.UserRole.GERENTE);
        userEntity.setActive(true);
        userEntity.setCreatedAt(LocalDateTime.of(2026, 1, 28, 10, 0));

        var entity = new com.chepamotos.infrastructure.entity.RefreshToken();
        entity.setId(11L);
        entity.setUser(userEntity);
        entity.setTokenHash("token-hash");
        entity.setIssuedAt(LocalDateTime.of(2026, 1, 28, 10, 0));
        entity.setExpiresAt(LocalDateTime.of(2026, 2, 4, 10, 0));

        assertNull(RefreshTokenEntityMapper.toDomain(null));

        var domain = RefreshTokenEntityMapper.toDomain(entity);

        assertEquals(entity.getId(), domain.id());
        assertEquals(entity.getUser().getId(), domain.userId());
        assertEquals(entity.getTokenHash(), domain.tokenHash());
        assertEquals(entity.getIssuedAt(), domain.issuedAt());
        assertEquals(entity.getExpiresAt(), domain.expiresAt());
    }
}