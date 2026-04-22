package com.chepamotos.domain.port.in;

import com.chepamotos.domain.model.Vehicle;

public interface VehicleApplicationUseCase {

    Vehicle getByPlate(String rawPlate);

    Vehicle resolveForServiceInvoice(String rawPlate, String rawModel);
}