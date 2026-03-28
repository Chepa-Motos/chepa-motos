package com.chepamotos.infrastructure.config;

import com.chepamotos.domain.port.VehicleRepository;
import com.chepamotos.domain.usecase.vehicle.GetVehicleByPlateUseCase;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class VehicleUseCaseConfig {

    @Bean
    public GetVehicleByPlateUseCase getVehicleByPlateUseCase(VehicleRepository vehicleRepository) {
        return new GetVehicleByPlateUseCase(vehicleRepository);
    }

    @Bean
    public ResolveVehicleForServiceInvoiceUseCase resolveVehicleForServiceInvoiceUseCase(VehicleRepository vehicleRepository) {
        return new ResolveVehicleForServiceInvoiceUseCase(vehicleRepository);
    }
}
