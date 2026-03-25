package com.chepamotos.chepa_api.servicio;

import com.chepamotos.chepa_api.dto.DailyLiquidationDTO;
import com.chepamotos.chepa_api.modelo.DailyLiquidation;
import com.chepamotos.chepa_api.modelo.Invoice;
import com.chepamotos.chepa_api.modelo.Mechanic;
import com.chepamotos.chepa_api.repositorio.DailyLiquidationRepository;
import com.chepamotos.chepa_api.repositorio.InvoiceRepository;
import com.chepamotos.chepa_api.repositorio.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DailyLiquidationService {
    
    private final DailyLiquidationRepository liquidationRepository;
    private final InvoiceRepository invoiceRepository;
    private final MechanicRepository mechanicRepository;
    
    // Listar todas las liquidaciones
    public List<DailyLiquidationDTO> getAllLiquidations() {
        return liquidationRepository.findAll()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Obtener liquidación por ID
    public DailyLiquidationDTO getLiquidationById(Long id) {
        DailyLiquidation liquidation = liquidationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Liquidación no encontrada con ID: " + id));
        return convertToDTO(liquidation);
    }
    
    // Obtener liquidaciones por mecánico
    public List<DailyLiquidationDTO> getLiquidationsByMechanicId(Long mechanicId) {
        return liquidationRepository.findByMechanic_MechanicId(mechanicId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Ejecutar liquidación del día para un mecánico
    @Transactional
    public DailyLiquidationDTO executeLiquidation(Long mechanicId, LocalDate date) {
        // Validar que el mecánico existe
        Mechanic mechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + mechanicId));
        
        // Validar que no exista ya una liquidación para ese día
        if (liquidationRepository.existsByMechanic_MechanicIdAndLiquidationDate(mechanicId, date)) {
            throw new RuntimeException("Ya existe una liquidación para el mecánico " + mechanic.getName() + " en la fecha " + date);
        }
        
        // Calcular el total de facturas del mecánico para ese día
        List<Invoice> invoices = invoiceRepository.findByMechanic_MechanicId(mechanicId)
                .stream()
                .filter(invoice -> invoice.getInvoiceDate().equals(date))
                .filter(invoice -> !invoice.getIsCancelled())
                .toList();
        
        BigDecimal total = invoices.stream()
                .map(Invoice::getTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        // Crear la liquidación
        DailyLiquidation liquidation = DailyLiquidation.builder()
                .mechanic(mechanic)
                .liquidationDate(date)
                .total(total)
                .build();
        
        DailyLiquidation savedLiquidation = liquidationRepository.save(liquidation);
        return convertToDTO(savedLiquidation);
    }
    
    // Ejecutar liquidación del día para todos los mecánicos activos
    @Transactional
    public List<DailyLiquidationDTO> executeDailyLiquidationForAll(LocalDate date) {
        List<Mechanic> activeMechanics = mechanicRepository.findByIsActiveTrue();
        
        return activeMechanics.stream()
                .filter(mechanic -> !liquidationRepository.existsByMechanic_MechanicIdAndLiquidationDate(
                        mechanic.getMechanicId(), date))
                .map(mechanic -> executeLiquidation(mechanic.getMechanicId(), date))
                .collect(Collectors.toList());
    }
    
    // Convertir entidad a DTO
    private DailyLiquidationDTO convertToDTO(DailyLiquidation liquidation) {
        return DailyLiquidationDTO.builder()
                .liquidationId(liquidation.getLiquidationId())
                .mechanicId(liquidation.getMechanic().getMechanicId())
                .mechanicName(liquidation.getMechanic().getName())
                .liquidationDate(liquidation.getLiquidationDate())
                .total(liquidation.getTotal())
                .build();
    }
}