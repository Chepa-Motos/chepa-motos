package com.chepamotos.infrastructure.repository;

import com.chepamotos.infrastructure.entity.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SpringDataVehicleRepository extends JpaRepository<Vehicle, Long> {

    Optional<Vehicle> findByPlate(String plate);
}
