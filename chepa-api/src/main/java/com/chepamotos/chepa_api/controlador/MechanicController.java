package com.chepamotos.chepa_api.controlador;

import com.chepamotos.chepa_api.dto.MechanicDTO;
import com.chepamotos.chepa_api.servicio.MechanicService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/mecanicos")
@RequiredArgsConstructor
public class MechanicController {
    
    private final MechanicService mechanicService;
    
    @GetMapping
    public ResponseEntity<List<MechanicDTO>> getAllActiveMechanics() {
        List<MechanicDTO> mechanics = mechanicService.getAllActiveMechanics();
        return ResponseEntity.ok(mechanics);
    }
    
    @GetMapping("/{id}")
    public ResponseEntity<MechanicDTO> getMechanicById(@PathVariable Long id) {
        MechanicDTO mechanic = mechanicService.getMechanicById(id);
        return ResponseEntity.ok(mechanic);
    }
    
    @PostMapping
    public ResponseEntity<MechanicDTO> createMechanic(@RequestBody MechanicDTO mechanicDTO) {
        MechanicDTO createdMechanic = mechanicService.createMechanic(mechanicDTO);
        return ResponseEntity.ok(createdMechanic);
    }
    
    @PutMapping("/{id}")
    public ResponseEntity<MechanicDTO> updateMechanic(
            @PathVariable Long id, 
            @RequestBody MechanicDTO mechanicDTO) {
        MechanicDTO updatedMechanic = mechanicService.updateMechanic(id, mechanicDTO);
        return ResponseEntity.ok(updatedMechanic);
    }
    
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deactivateMechanic(@PathVariable Long id) {
        mechanicService.deactivateMechanic(id);
        return ResponseEntity.noContent().build();
    }
}