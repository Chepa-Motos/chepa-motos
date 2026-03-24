package com.chepamotos.chepa_api.controlador;

import com.chepamotos.chepa_api.dto.VehicleDTO;
import com.chepamotos.chepa_api.servicio.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/vehiculos")
@RequiredArgsConstructor
public class VehicleController {
    
    private final VehicleService vehicleService;
    
    // GET /api/vehiculos - Listar todos los vehículos
    @GetMapping
    public ResponseEntity<List<VehicleDTO>> getAllVehicles() {
        List<VehicleDTO> vehicles = vehicleService.getAllVehicles();
        return ResponseEntity.ok(vehicles);
    }
    
    // GET /api/vehiculos/{plate} - Obtener un vehículo por placa
    @GetMapping("/{plate}")
    public ResponseEntity<VehicleDTO> getVehicleByPlate(@PathVariable String plate) {
        VehicleDTO vehicle = vehicleService.getVehicleByPlate(plate.toUpperCase());
        return ResponseEntity.ok(vehicle);
    }
    
    // POST /api/vehiculos - Registrar un nuevo vehículo
    @PostMapping
    public ResponseEntity<VehicleDTO> createVehicle(@RequestBody VehicleDTO vehicleDTO) {
        VehicleDTO createdVehicle = vehicleService.createVehicle(vehicleDTO);
        return ResponseEntity.ok(createdVehicle);
    }
    
    // PUT /api/vehiculos/{plate} - Actualizar un vehículo
    @PutMapping("/{plate}")
    public ResponseEntity<VehicleDTO> updateVehicle(
            @PathVariable String plate, 
            @RequestBody VehicleDTO vehicleDTO) {
        VehicleDTO updatedVehicle = vehicleService.updateVehicle(plate.toUpperCase(), vehicleDTO);
        return ResponseEntity.ok(updatedVehicle);
    }
    
    // GET /api/vehiculos/buscar?marca=honda - Buscar vehículos por marca
    @GetMapping("/buscar")
    public ResponseEntity<List<VehicleDTO>> searchVehicles(@RequestParam String marca) {
        List<VehicleDTO> vehicles = vehicleService.searchByBrand(marca);
        return ResponseEntity.ok(vehicles);
    }
}