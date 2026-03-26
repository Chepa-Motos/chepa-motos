package com.chepamotos.adapter.controller;

import com.chepamotos.domain.exception.LiquidationAlreadyExistsException;
import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.service.LiquidationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LiquidationController.class)
@Import({GlobalExceptionHandler.class, LiquidationControllerTest.MockConfig.class})
class LiquidationControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LiquidationService liquidationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(liquidationService);
    }

    @Test
    void list_returnsOkEnvelope() throws Exception {
        when(liquidationService.list(any(), any())).thenReturn(List.of(sampleLiquidation(1L, 1L, "Jose")));

        mockMvc.perform(get("/api/liquidations"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].mechanic.id").value(1))
                .andExpect(jsonPath("$.data[0].invoice_count").value(2))
                .andExpect(jsonPath("$.data[0].total_revenue").value(100000.00))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_singleMechanic_whenValid_returnsCreatedEnvelope() throws Exception {
        when(liquidationService.create(any(), anyLong())).thenReturn(List.of(sampleLiquidation(10L, 1L, "Jose")));

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-01-28",
                                  "mechanic_id": 1
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[0].mechanic.id").value(1))
                .andExpect(jsonPath("$.data[0].mechanic.name").value("Jose"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_allMechanics_whenValid_returnsCreatedEnvelope() throws Exception {
        when(liquidationService.create(any(), any())).thenReturn(List.of(
                sampleLiquidation(10L, 1L, "Jose"),
                sampleLiquidation(11L, 2L, "Andres")
        ));

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-01-28",
                                  "mechanic_id": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].id").value(10))
                .andExpect(jsonPath("$.data[1].id").value(11))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_allMechanics_whenNoEligibleMechanics_returnsCreatedWithEmptyData() throws Exception {
        when(liquidationService.create(any(), any())).thenReturn(List.of());

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-01-28",
                                  "mechanic_id": null
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data").isArray())
                .andExpect(jsonPath("$.data").isEmpty())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenMissingDate_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mechanic_id": 1
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenMechanicNotFound_returns404WithStandardError() throws Exception {
        when(liquidationService.create(any(), anyLong())).thenThrow(new MechanicNotFoundException(99L));

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-01-28",
                                  "mechanic_id": 99
                                }
                                """))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MECHANIC_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenLiquidationAlreadyExists_returns409WithStandardError() throws Exception {
        when(liquidationService.create(any(), anyLong())).thenThrow(new LiquidationAlreadyExistsException(1L, LocalDate.of(2026, 1, 28)));

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-01-28",
                                  "mechanic_id": 1
                                }
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("LIQUIDATION_ALREADY_EXISTS"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private DailyLiquidation sampleLiquidation(Long id, Long mechanicId, String mechanicName) {
        return DailyLiquidation.restore(
                id,
                Mechanic.restore(mechanicId, mechanicName, true),
                LocalDate.of(2026, 1, 28),
                new BigDecimal("100000.00"),
                2
        );
    }

    static class MockConfig {

        @Bean
        @Primary
        LiquidationService liquidationService() {
            return Mockito.mock(LiquidationService.class);
        }
    }
}
