package com.chepamotos.adapter.controller;

import com.chepamotos.domain.exception.VehicleNotFoundException;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.in.VehicleApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VehicleController.class)
@Import({GlobalExceptionHandler.class, VehicleControllerTest.MockConfig.class})
class VehicleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private VehicleApplicationUseCase vehicleApplicationService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(vehicleApplicationService);
    }

    @Test
    void getByPlate_whenExists_returnsVehicleEnvelope() throws Exception {
        when(vehicleApplicationService.getByPlate("bxr42h"))
                .thenReturn(Vehicle.restore(1L, "BXR42H", "Boxer 150 2021"));

        mockMvc.perform(get("/api/vehicles/plate/bxr42h"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.plate").value("BXR42H"))
                .andExpect(jsonPath("$.data.model").value("Boxer 150 2021"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getByPlate_whenMissing_returns404WithStandardError() throws Exception {
        when(vehicleApplicationService.getByPlate(anyString()))
                .thenThrow(new VehicleNotFoundException("NOEXISTE1"));

        mockMvc.perform(get("/api/vehicles/plate/noexiste1"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("VEHICLE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    static class MockConfig {

        @Bean
        @Primary
        VehicleApplicationUseCase vehicleApplicationService() {
            return Mockito.mock(VehicleApplicationUseCase.class);
        }

        @Bean
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
