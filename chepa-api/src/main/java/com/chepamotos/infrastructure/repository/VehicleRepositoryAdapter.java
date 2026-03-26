package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;
import com.chepamotos.infrastructure.mapper.VehicleEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class VehicleRepositoryAdapter implements VehicleRepository {

    private final SpringDataVehicleRepository springDataVehicleRepository;

    public VehicleRepositoryAdapter(SpringDataVehicleRepository springDataVehicleRepository) {
        this.springDataVehicleRepository = springDataVehicleRepository;
    }

    @Override
    public Optional<Vehicle> findById(Long id) {
        return springDataVehicleRepository.findById(id)
                .map(VehicleEntityMapper::toDomain);
    }

    @Override
    public Optional<Vehicle> findByPlate(String plate) {
        return springDataVehicleRepository.findByPlate(plate)
                .map(VehicleEntityMapper::toDomain);
    }

    @Override
    public Vehicle save(Vehicle vehicle) {
        var entityToSave = VehicleEntityMapper.toEntity(vehicle);
        var savedEntity = springDataVehicleRepository.save(entityToSave);
        return VehicleEntityMapper.toDomain(savedEntity);
    }
}
