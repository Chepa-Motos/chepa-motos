package com.chepamotos.infrastructure.config;

import com.chepamotos.domain.port.DailyLiquidationRepository;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.domain.usecase.liquidation.CreateLiquidationUseCase;
import com.chepamotos.domain.usecase.liquidation.ListLiquidationsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LiquidationUseCaseConfig {

    @Bean
    public ListLiquidationsUseCase listLiquidationsUseCase(DailyLiquidationRepository dailyLiquidationRepository) {
        return new ListLiquidationsUseCase(dailyLiquidationRepository);
    }

    @Bean
    public CreateLiquidationUseCase createLiquidationUseCase(
            DailyLiquidationRepository dailyLiquidationRepository,
            InvoiceRepository invoiceRepository,
            MechanicRepository mechanicRepository) {
        return new CreateLiquidationUseCase(dailyLiquidationRepository, invoiceRepository, mechanicRepository);
    }
}