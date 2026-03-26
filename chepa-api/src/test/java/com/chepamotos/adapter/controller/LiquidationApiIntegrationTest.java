package com.chepamotos.adapter.controller;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class LiquidationApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE daily_liquidation, invoice_item, invoice, vehicle, mechanic RESTART IDENTITY CASCADE");
    }

    @Test
    void liquidationFlow_createThenListByDateAndMechanic_returnsComputedValues() throws Exception {
        LocalDate today = LocalDate.now();
        long mechanicId = createMechanic("Jose Integration");

        createServiceInvoice(mechanicId, "BXR74F", "Boxer 150 2021", "65000.00", "Tornillo", "1", "3900.00");
        createServiceInvoice(mechanicId, "BXR74F", "Boxer 150 2021", "35000.00", "Filtro", "1", "12000.00");

        long cancelledInvoiceId = createServiceInvoice(
                mechanicId,
                "BXR74F",
                "Boxer 150 2021",
                "50000.00",
                "Aceite",
                "1",
                "15000.00"
        );

        mockMvc.perform(patch("/api/invoices/{id}/cancel", cancelledInvoiceId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.is_cancelled").value(true));

        createDeliveryInvoice("Cliente Mostrador", "Cadena", "1", "25000.00");

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "%s",
                                  "mechanic_id": %d
                                }
                                """.formatted(today, mechanicId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].mechanic.id").value(mechanicId))
                .andExpect(jsonPath("$.data[0].date").value(today.toString()))
                .andExpect(jsonPath("$.data[0].invoice_count").value(2))
                .andExpect(jsonPath("$.data[0].total_revenue").value(100000.00))
                .andExpect(jsonPath("$.data[0].mechanic_share").value(70000.00))
                .andExpect(jsonPath("$.data[0].shop_share").value(30000.00));

        mockMvc.perform(get("/api/liquidations")
                        .param("date", today.toString())
                        .param("mechanic_id", String.valueOf(mechanicId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].mechanic.id").value(mechanicId))
                .andExpect(jsonPath("$.data[0].invoice_count").value(2))
                .andExpect(jsonPath("$.data[0].total_revenue").value(100000.00))
                .andExpect(jsonPath("$.data[0].mechanic_share").value(70000.00))
                .andExpect(jsonPath("$.data[0].shop_share").value(30000.00));
    }

    private long createMechanic(String name) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/mechanics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "%s"
                                }
                                """.formatted(name)))
                .andExpect(status().isCreated())
                .andReturn();

        Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
        return id.longValue();
    }

    private long createServiceInvoice(
            long mechanicId,
            String plate,
            String model,
            String laborAmount,
            String itemDescription,
            String quantity,
            String unitPrice
    ) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/invoices/service")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "mechanic_id": %d,
                                  "vehicle_plate": "%s",
                                  "model": "%s",
                                  "labor_amount": %s,
                                  "items": [
                                    {
                                      "description": "%s",
                                      "quantity": %s,
                                      "unit_price": %s
                                    }
                                  ]
                                }
                                """.formatted(mechanicId, plate, model, laborAmount, itemDescription, quantity, unitPrice)))
                .andExpect(status().isCreated())
                .andReturn();

                          Number id = JsonPath.read(result.getResponse().getContentAsString(), "$.data.id");
                          return id.longValue();
    }

    private void createDeliveryInvoice(
            String buyerName,
            String itemDescription,
            String quantity,
            String unitPrice
    ) throws Exception {
        mockMvc.perform(post("/api/invoices/delivery")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "buyer_name": "%s",
                                  "items": [
                                    {
                                      "description": "%s",
                                      "quantity": %s,
                                      "unit_price": %s
                                    }
                                  ]
                                }
                                """.formatted(buyerName, itemDescription, quantity, unitPrice)))
                .andExpect(status().isCreated());
    }
}
