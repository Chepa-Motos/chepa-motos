package com.chepamotos.chepa_api.servicio;

import com.chepamotos.chepa_api.dto.VehicleDTO;
import com.chepamotos.chepa_api.modelo.Vehicle;
import com.chepamotos.chepa_api.repositorio.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VehicleService {
    
    private final VehicleRepository vehicleRepository;
    
    public List<VehicleDTO> getAllVehicles() {
        return vehicleRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public VehicleDTO getVehicleByPlate(String plate) {
        Vehicle vehicle = vehicleRepository.findById(plate)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con placa: " + plate));
        return convertToDTO(vehicle);
    }
    
    @Transactional
    public VehicleDTO createVehicle(VehicleDTO vehicleDTO) {
        if (vehicleRepository.existsById(vehicleDTO.getPlate())) {
            throw new RuntimeException("Ya existe un vehículo con la placa: " + vehicleDTO.getPlate());
        }
        
        Vehicle vehicle = Vehicle.builder()
                .plate(vehicleDTO.getPlate().toUpperCase())
                .brand(vehicleDTO.getBrand())
                .model(vehicleDTO.getModel())
                .build();
        
        Vehicle savedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(savedVehicle);
    }
    
    @Transactional
    public VehicleDTO updateVehicle(String plate, VehicleDTO vehicleDTO) {
        Vehicle vehicle = vehicleRepository.findById(plate)
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con placa: " + plate));
        
        vehicle.setBrand(vehicleDTO.getBrand());
        vehicle.setModel(vehicleDTO.getModel());
        
        Vehicle updatedVehicle = vehicleRepository.save(vehicle);
        return convertToDTO(updatedVehicle);
    }
    
    public List<VehicleDTO> searchByBrand(String brand) {
        return vehicleRepository.findByBrandContainingIgnoreCase(brand)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private VehicleDTO convertToDTO(Vehicle vehicle) {
        return VehicleDTO.builder()
                .plate(vehicle.getPlate())
                .brand(vehicle.getBrand())
                .model(vehicle.getModel())
                .build();
    }
}