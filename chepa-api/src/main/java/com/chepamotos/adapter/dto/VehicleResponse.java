package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.Vehicle;

public record VehicleResponse(
        Long id,
        String plate,
        String model
) {

    public static VehicleResponse fromDomain(Vehicle vehicle) {
        return new VehicleResponse(vehicle.id(), vehicle.plate(), vehicle.model());
    }
}
