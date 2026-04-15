package com.chepamotos.domain.service;

import com.chepamotos.domain.model.Vehicle;

public interface VehicleApplicationUseCase {

    Vehicle getByPlate(String rawPlate);

    Vehicle resolveForServiceInvoice(String rawPlate, String rawModel);
}
