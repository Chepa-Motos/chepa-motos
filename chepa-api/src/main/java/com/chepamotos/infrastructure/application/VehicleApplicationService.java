package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.usecase.vehicle.GetVehicleByPlateUseCase;
import com.chepamotos.domain.usecase.vehicle.ResolveVehicleForServiceInvoiceUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class VehicleApplicationService {

    // This class is an outer-layer facade: Spring + transactions live here.
    // Domain use cases remain framework-free under domain/usecase.

    private final GetVehicleByPlateUseCase getVehicleByPlateUseCase;
    private final ResolveVehicleForServiceInvoiceUseCase resolveVehicleForServiceInvoiceUseCase;

    public VehicleApplicationService(
            GetVehicleByPlateUseCase getVehicleByPlateUseCase,
            ResolveVehicleForServiceInvoiceUseCase resolveVehicleForServiceInvoiceUseCase
    ) {
        this.getVehicleByPlateUseCase = getVehicleByPlateUseCase;
        this.resolveVehicleForServiceInvoiceUseCase = resolveVehicleForServiceInvoiceUseCase;
    }

    @Transactional(readOnly = true)
    public Vehicle getByPlate(String rawPlate) {
        return getVehicleByPlateUseCase.execute(rawPlate);
    }

    @Transactional
    public Vehicle resolveForServiceInvoice(String rawPlate, String rawModel) {
        return resolveVehicleForServiceInvoiceUseCase.execute(rawPlate, rawModel);
    }
}
