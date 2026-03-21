package com.chepamotos.adapter.controller;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.service.MechanicService;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MechanicController.class)
@Import({GlobalExceptionHandler.class, MechanicControllerTest.MockConfig.class})
class MechanicControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MechanicService mechanicService;

    @Test
    void list_returnsOkEnvelope() throws Exception {
        when(mechanicService.listByActive(anyBoolean()))
                .thenReturn(List.of(Mechanic.restore(1L, "Jose", true)));

        mockMvc.perform(get("/api/mechanics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].name").value("Jose"))
                .andExpect(jsonPath("$.data[0].is_active").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getById_whenNotFound_returns404WithStandardError() throws Exception {
        when(mechanicService.getById(anyLong()))
                .thenThrow(new MechanicNotFoundException(99L));

        mockMvc.perform(get("/api/mechanics/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("MECHANIC_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenValid_returnsCreatedEnvelope() throws Exception {
        when(mechanicService.create(anyString()))
                .thenReturn(Mechanic.restore(5L, "Carlos", true));

        mockMvc.perform(post("/api/mechanics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Carlos"
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(5))
                .andExpect(jsonPath("$.data.name").value("Carlos"))
                .andExpect(jsonPath("$.data.is_active").value(true))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenBlankName_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/mechanics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "   "
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void changeStatus_returnsUpdatedMechanic() throws Exception {
        when(mechanicService.changeStatus(1L, false))
                .thenReturn(Mechanic.restore(1L, "Jose", false));

        mockMvc.perform(patch("/api/mechanics/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "is_active": false
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.name").value("Jose"))
                .andExpect(jsonPath("$.data.is_active").value(false))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    static class MockConfig {

        @Bean
        @Primary
        MechanicService mechanicService() {
            return Mockito.mock(MechanicService.class);
        }
    }
}
