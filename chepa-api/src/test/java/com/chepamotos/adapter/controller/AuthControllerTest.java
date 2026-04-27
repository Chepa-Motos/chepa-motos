package com.chepamotos.adapter.controller;

import com.chepamotos.domain.model.AccessToken;
import com.chepamotos.domain.model.AuthTokens;
import com.chepamotos.domain.port.in.AuthApplicationUseCase;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({GlobalExceptionHandler.class, AuthControllerTest.MockConfig.class})
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AuthApplicationUseCase authApplicationService;

    @Test
    void login_whenValid_returnsTokenEnvelope() throws Exception {
        when(authApplicationService.login(anyString(), anyString()))
                .thenReturn(AuthTokens.of(new AccessToken("test-access-token", 900), "test-refresh-token"));

        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "username": "gerente",
                                  "password": "password"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.accessToken").value("test-access-token"))
                .andExpect(jsonPath("$.data.expiresIn").value(900))
                .andExpect(jsonPath("$.data.refreshToken").value("test-refresh-token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void refresh_whenValid_returnsTokenEnvelope() throws Exception {
        when(authApplicationService.refresh(anyString()))
                .thenReturn(AuthTokens.of(new AccessToken("new-access-token", 900), "new-refresh-token"));

        mockMvc.perform(post("/api/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "old-refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").value("new-access-token"))
                .andExpect(jsonPath("$.data.refreshToken").value("new-refresh-token"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void logout_whenValid_returnsOkEnvelope() throws Exception {
        mockMvc.perform(post("/api/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "refreshToken": "refresh-token"
                                }
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.message").value("Session closed"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    static class MockConfig {

        @Bean
        @Primary
        AuthApplicationUseCase authApplicationService() {
            return Mockito.mock(AuthApplicationUseCase.class);
        }

        @Bean
        Clock testClock() {
            return Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
        }
    }
}
