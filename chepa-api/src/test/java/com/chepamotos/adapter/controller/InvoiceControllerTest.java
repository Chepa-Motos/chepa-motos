package com.chepamotos.adapter.controller;

import com.chepamotos.domain.exception.InvoiceNotFoundException;
import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.service.InvoiceService;
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
import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InvoiceController.class)
@Import({GlobalExceptionHandler.class, InvoiceControllerTest.MockConfig.class})
class InvoiceControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private InvoiceService invoiceService;

    @BeforeEach
    void resetMocks() {
        Mockito.reset(invoiceService);
    }

    @Test
    void list_returnsOkEnvelope() throws Exception {
        when(invoiceService.listAll()).thenReturn(List.of(sampleServiceInvoice()));

        mockMvc.perform(get("/api/invoices"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(1))
                .andExpect(jsonPath("$.data[0].invoice_type").value("SERVICE"))
                .andExpect(jsonPath("$.data[0].mechanic.id").value(1))
                .andExpect(jsonPath("$.data[0].items[0].description").value("Freno delantero"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getById_whenExists_returnsInvoiceEnvelope() throws Exception {
        when(invoiceService.getById(1L)).thenReturn(sampleServiceInvoice());

        mockMvc.perform(get("/api/invoices/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.invoice_type").value("SERVICE"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void getById_whenNotFound_returns404WithStandardError() throws Exception {
        when(invoiceService.getById(anyLong())).thenThrow(new InvoiceNotFoundException(99L));

        mockMvc.perform(get("/api/invoices/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INVOICE_NOT_FOUND"))
                .andExpect(jsonPath("$.message").exists())
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_serviceInvoice_whenValid_returnsCreatedEnvelope() throws Exception {
        when(invoiceService.create(any(), anyLong(), anyLong(), any(), any(), any()))
                .thenReturn(sampleServiceInvoice());

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoice_type": "SERVICE",
                                  "mechanic_id": 1,
                                  "vehicle_plate": "BXR42H",
                                  "labor_amount": 45000.00,
                                  "items": [
                                    {"description": "Freno delantero", "quantity": 1, "unit_price": 36900.00}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.invoice_type").value("SERVICE"))
                .andExpect(jsonPath("$.data.mechanic.name").value("Jose"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_deliveryInvoice_whenValid_returnsCreatedEnvelope() throws Exception {
        when(invoiceService.create(any(), any(), any(), any(), any(), any()))
                .thenReturn(sampleDeliveryInvoice());

        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoice_type": "DELIVERY",
                                  "buyer_name": "Talleres La 80",
                                  "items": [
                                    {"description": "Boxer 150 Palanca de freno", "quantity": 2, "unit_price": 18500.00}
                                  ]
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.invoice_type").value("DELIVERY"))
                .andExpect(jsonPath("$.data.buyer_name").value("Talleres La 80"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenEmptyItems_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "invoice_type": "SERVICE",
                                  "mechanic_id": 1,
                                  "vehicle_plate": "BXR42H",
                                  "labor_amount": 45000.00,
                                  "items": []
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void create_whenMissingInvoiceType_returns400ValidationError() throws Exception {
        mockMvc.perform(post("/api/invoices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "items": [
                                    {"description": "Freno", "quantity": 1, "unit_price": 36900.00}
                                  ]
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    private Invoice sampleServiceInvoice() {
        Mechanic mechanic = Mechanic.restore(1L, "Jose", true);
        Vehicle vehicle = Vehicle.restore(1L, "BXR42H", "Boxer 150 2021");
        List<InvoiceItem> items = List.of(InvoiceItem.restore(1L, "Freno delantero", BigDecimal.ONE, new BigDecimal("36900.00")));
        return Invoice.restore(1L, InvoiceType.SERVICE, mechanic, vehicle, null, LocalDateTime.now(), new BigDecimal("45000.00"), false, items);
    }

    private Invoice sampleDeliveryInvoice() {
        List<InvoiceItem> items = List.of(InvoiceItem.restore(1L, "Boxer 150 Palanca de freno", new BigDecimal("2"), new BigDecimal("18500.00")));
        return Invoice.restore(2L, InvoiceType.DELIVERY, null, null, "Talleres La 80", LocalDateTime.now(), BigDecimal.ZERO, false, items);
    }

    static class MockConfig {

        @Bean
        @Primary
        InvoiceService invoiceService() {
            return Mockito.mock(InvoiceService.class);
        }
    }
}
