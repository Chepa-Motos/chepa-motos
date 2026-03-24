package com.chepamotos.chepa_api.repositorio;

import com.chepamotos.chepa_api.modelo.Vehicle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VehicleRepository extends JpaRepository<Vehicle, String> {
    
    // Buscar vehículos por marca
    List<Vehicle> findByBrandContainingIgnoreCase(String brand);
    
    // Buscar vehículos por modelo
    List<Vehicle> findByModelContainingIgnoreCase(String model);
}