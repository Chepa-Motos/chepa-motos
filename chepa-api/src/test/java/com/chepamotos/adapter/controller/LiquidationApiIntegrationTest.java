package com.chepamotos.adapter.controller;

import com.jayway.jsonpath.JsonPath;
import com.chepamotos.domain.port.PasswordHasher;
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

    @Autowired
    private PasswordHasher passwordHasher;

    @BeforeEach
    void cleanDatabase() {
      jdbcTemplate.execute("TRUNCATE TABLE refresh_token, app_user, daily_liquidation, invoice_item, invoice, vehicle, mechanic RESTART IDENTITY CASCADE");
      jdbcTemplate.update(
          "INSERT INTO app_user (username, password_hash, role, is_active, created_at) VALUES (?, ?, ?, ?, now())",
          "gerente",
        passwordHasher.hash("password"),
          "GERENTE",
          true
      );
    }

    @Test
    void liquidationFlow_createThenListByDateAndMechanic_returnsComputedValues() throws Exception {
        LocalDate today = LocalDate.now();
      String managerToken = loginAsManager();
      long mechanicId = createMechanic("Jose Integration", managerToken);

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
            .header("Authorization", "Bearer " + managerToken)
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

    @Test
    void protectedManagerEndpoints_withoutToken_return401() throws Exception {
        mockMvc.perform(post("/api/mechanics")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "name": "Unauthorized"
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));

        mockMvc.perform(post("/api/liquidations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "date": "2026-01-28",
                                  "mechanic_id": 1
                                }
                                """))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("AUTH_REQUIRED"));
    }

    private long createMechanic(String name, String accessToken) throws Exception {
        MvcResult result = mockMvc.perform(post("/api/mechanics")
              .header("Authorization", "Bearer " + accessToken)
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

      private String loginAsManager() throws Exception {
        MvcResult result = mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {
                      "username": "gerente",
                      "password": "password"
                    }
                    """))
            .andExpect(status().isOk())
            .andReturn();

        return JsonPath.read(result.getResponse().getContentAsString(), "$.data.accessToken");
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
