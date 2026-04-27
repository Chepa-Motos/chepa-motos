package com.chepamotos.adapter.controller;

import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.port.in.InvoiceApplicationUseCase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceItemController.class)
@Import({ GlobalExceptionHandler.class, InvoiceItemControllerTest.MockConfig.class })
class InvoiceItemControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private InvoiceApplicationUseCase invoiceApplicationService;

  @BeforeEach
  void resetMocks() {
    Mockito.reset(invoiceApplicationService);
  }

  @Test
  void suggestions_withModelAndQuery_returnsSuggestionsEnvelope() throws Exception {
    List<InvoiceItem> suggestions = List.of(
        InvoiceItem.createNew("Freno Delantero", BigDecimal.ONE, new BigDecimal("45000.00")),
        InvoiceItem.createNew("Freno Trasero", BigDecimal.ONE, new BigDecimal("38000.00")));

    when(invoiceApplicationService.findSuggestions("Boxer 150", "Fre"))
        .thenReturn(suggestions);

    mockMvc.perform(get("/api/invoice-items/suggestions")
        .param("model", "Boxer 150")
        .param("q", "Fre"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].description").value("Freno Delantero"))
      .andExpect(jsonPath("$.data[0].unit_price").value(45000.0))
        .andExpect(jsonPath("$.data[1].description").value("Freno Trasero"))
      .andExpect(jsonPath("$.data[1].unit_price").value(38000.0))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void suggestions_withEmptyResults_returnsEmptyArray() throws Exception {
    when(invoiceApplicationService.findSuggestions(anyString(), anyString()))
        .thenReturn(List.of());

    mockMvc.perform(get("/api/invoice-items/suggestions")
        .param("model", "Honda CB")
        .param("q", "xyz"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data").isArray())
        .andExpect(jsonPath("$.data.length()").value(0))
        .andExpect(jsonPath("$.timestamp").exists());
  }

  @Test
  void suggestions_withSingleResult_returnsSingleSuggestion() throws Exception {
    List<InvoiceItem> suggestions = List.of(
        InvoiceItem.createNew("Aceite Sintetico 20W50", BigDecimal.ONE, new BigDecimal("25000.00")));

    when(invoiceApplicationService.findSuggestions("Boxer 150", "Ace"))
        .thenReturn(suggestions);

    mockMvc.perform(get("/api/invoice-items/suggestions")
        .param("model", "Boxer 150")
        .param("q", "Ace"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].description").value("Aceite Sintetico 20W50"))
        .andExpect(jsonPath("$.data.length()").value(1))
        .andExpect(jsonPath("$.timestamp").exists());
  }

        @Test
        void suggestions_withShortQuery_returns400ValidationError() throws Exception {
          mockMvc.perform(get("/api/invoice-items/suggestions")
          .param("model", "Boxer 150")
          .param("q", "F"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
          .andExpect(jsonPath("$.message").exists())
          .andExpect(jsonPath("$.timestamp").exists());
        }

  static class MockConfig {

    @Bean
    @Primary
    InvoiceApplicationUseCase invoiceApplicationService() {
      return Mockito.mock(InvoiceApplicationUseCase.class);
    }

    @Bean
    Clock testClock() {
      return Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);
    }
  }
}
