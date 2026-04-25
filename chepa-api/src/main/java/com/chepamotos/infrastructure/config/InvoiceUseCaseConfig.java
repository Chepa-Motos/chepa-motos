package com.chepamotos.infrastructure.config;

import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.domain.usecase.invoice.CancelInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateDeliveryInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.CreateServiceInvoiceUseCase;
import com.chepamotos.domain.usecase.invoice.FindInvoiceItemSuggestionsUseCase;
import com.chepamotos.domain.usecase.invoice.GetInvoiceByIdUseCase;
import com.chepamotos.domain.usecase.invoice.ListInvoicesUseCase;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class InvoiceUseCaseConfig {

    @Bean
    public Clock systemClock() {
        return Clock.system(ZoneId.of("America/Bogota"));
    }

    @Bean
    public ListInvoicesUseCase listInvoicesUseCase(InvoiceRepository invoiceRepository, Clock systemClock) {
        return new ListInvoicesUseCase(invoiceRepository, systemClock);
    }

    @Bean
    public GetInvoiceByIdUseCase getInvoiceByIdUseCase(InvoiceRepository invoiceRepository) {
        return new GetInvoiceByIdUseCase(invoiceRepository);
    }

    @Bean
    public CancelInvoiceUseCase cancelInvoiceUseCase(InvoiceRepository invoiceRepository) {
        return new CancelInvoiceUseCase(invoiceRepository);
    }

    @Bean
    public CreateDeliveryInvoiceUseCase createDeliveryInvoiceUseCase(InvoiceRepository invoiceRepository) {
        return new CreateDeliveryInvoiceUseCase(invoiceRepository);
    }

    @Bean
    public CreateServiceInvoiceUseCase createServiceInvoiceUseCase(
            InvoiceRepository invoiceRepository,
            MechanicRepository mechanicRepository,
            ResolveVehicleForServiceInvoiceUseCase resolveVehicleForServiceInvoiceUseCase) {
        return new CreateServiceInvoiceUseCase(
                invoiceRepository,
                mechanicRepository,
                resolveVehicleForServiceInvoiceUseCase);
    }

    @Bean
    public FindInvoiceItemSuggestionsUseCase findInvoiceItemSuggestionsUseCase(InvoiceRepository invoiceRepository) {
        return new FindInvoiceItemSuggestionsUseCase(invoiceRepository);
    }
}