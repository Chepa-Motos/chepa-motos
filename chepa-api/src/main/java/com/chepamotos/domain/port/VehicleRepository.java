package com.chepamotos.domain.port;

import com.chepamotos.domain.model.Vehicle;

import java.util.Optional;

public interface VehicleRepository {

    Optional<Vehicle> findById(Long id);

    Optional<Vehicle> findByPlate(String plate);

    Vehicle save(Vehicle vehicle);
}
