package com.chepamotos.chepa_api.controlador;

import com.chepamotos.chepa_api.dto.DailyLiquidationDTO;
import com.chepamotos.chepa_api.servicio.DailyLiquidationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/liquidaciones")
@RequiredArgsConstructor
public class DailyLiquidationController {
    
    private final DailyLiquidationService liquidationService;
    
    // GET /api/liquidaciones - Listar todas las liquidaciones
    @GetMapping
    public ResponseEntity<List<DailyLiquidationDTO>> getAllLiquidations() {
        List<DailyLiquidationDTO> liquidations = liquidationService.getAllLiquidations();
        return ResponseEntity.ok(liquidations);
    }
    
    // GET /api/liquidaciones/{id} - Obtener liquidación por ID
    @GetMapping("/{id}")
    public ResponseEntity<DailyLiquidationDTO> getLiquidationById(@PathVariable Long id) {
        DailyLiquidationDTO liquidation = liquidationService.getLiquidationById(id);
        return ResponseEntity.ok(liquidation);
    }
    
    // GET /api/liquidaciones/mecanico/{mechanicId} - Liquidaciones de un mecánico
    @GetMapping("/mecanico/{mechanicId}")
    public ResponseEntity<List<DailyLiquidationDTO>> getLiquidationsByMechanicId(@PathVariable Long mechanicId) {
        List<DailyLiquidationDTO> liquidations = liquidationService.getLiquidationsByMechanicId(mechanicId);
        return ResponseEntity.ok(liquidations);
    }
    
    // POST /api/liquidaciones/ejecutar - Ejecutar liquidación para un mecánico
    @PostMapping("/ejecutar")
    public ResponseEntity<DailyLiquidationDTO> executeLiquidation(
            @RequestParam Long mechanicId,
            @RequestParam LocalDate fecha) {
        DailyLiquidationDTO liquidation = liquidationService.executeLiquidation(mechanicId, fecha);
        return ResponseEntity.ok(liquidation);
    }
    
    // POST /api/liquidaciones/ejecutar/todos - Ejecutar liquidación del día para todos
    @PostMapping("/ejecutar/todos")
    public ResponseEntity<List<DailyLiquidationDTO>> executeDailyLiquidationForAll(
            @RequestParam LocalDate fecha) {
        List<DailyLiquidationDTO> liquidations = liquidationService.executeDailyLiquidationForAll(fecha);
        return ResponseEntity.ok(liquidations);
    }
}