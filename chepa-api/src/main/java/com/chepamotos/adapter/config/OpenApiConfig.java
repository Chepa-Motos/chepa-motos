package com.chepamotos.adapter.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI chepaApiOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Chepa Motos API")
                        .description("REST API for billing and operations at Chepa Motos")
                        .version("v1")
                        .contact(new Contact().name("Chepa Motos Team")));
    }
}
