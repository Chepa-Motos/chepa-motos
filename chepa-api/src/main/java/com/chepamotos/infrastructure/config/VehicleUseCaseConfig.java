package com.chepamotos.infrastructure.config;

import com.chepamotos.domain.port.VehicleRepository;
import com.chepamotos.domain.usecase.vehicle.GetVehicleByPlateUseCase;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VehicleUseCaseConfig {

    // Query use case beans
    @Bean
    public GetVehicleByPlateUseCase getVehicleByPlateUseCase(VehicleRepository vehicleRepository) {
        return new GetVehicleByPlateUseCase(vehicleRepository);
    }

    // Command use case beans
    @Bean
    public ResolveVehicleForServiceInvoiceUseCase resolveVehicleForServiceInvoiceUseCase(VehicleRepository vehicleRepository) {
        return new ResolveVehicleForServiceInvoiceUseCase(vehicleRepository);
    }
}
