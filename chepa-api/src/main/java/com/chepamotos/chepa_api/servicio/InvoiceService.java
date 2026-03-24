package com.chepamotos.chepa_api.servicio;

import com.chepamotos.chepa_api.dto.InvoiceDTO;
import com.chepamotos.chepa_api.enums.InvoiceType;
import com.chepamotos.chepa_api.modelo.Invoice;
import com.chepamotos.chepa_api.modelo.Mechanic;
import com.chepamotos.chepa_api.modelo.Vehicle;
import com.chepamotos.chepa_api.repositorio.InvoiceRepository;
import com.chepamotos.chepa_api.repositorio.MechanicRepository;
import com.chepamotos.chepa_api.repositorio.VehicleRepository;
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
public class InvoiceService {
    
    private final InvoiceRepository invoiceRepository;
    private final MechanicRepository mechanicRepository;
    private final VehicleRepository vehicleRepository;
    
    // Listar todas las facturas activas (no canceladas)
    public List<InvoiceDTO> getAllActiveInvoices() {
        return invoiceRepository.findByIsCancelledFalse()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Obtener una factura por ID
    public InvoiceDTO getInvoiceById(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + id));
        return convertToDTO(invoice);
    }
    
    // Crear una nueva factura
    @Transactional
    public InvoiceDTO createInvoice(InvoiceDTO invoiceDTO) {
        // Validar que el mecánico existe
        Mechanic mechanic = mechanicRepository.findById(invoiceDTO.getMechanicId())
                .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + invoiceDTO.getMechanicId()));
        
        // Validar que el vehículo existe
        Vehicle vehicle = vehicleRepository.findById(invoiceDTO.getVehiclePlate())
                .orElseThrow(() -> new RuntimeException("Vehículo no encontrado con placa: " + invoiceDTO.getVehiclePlate()));
        
        Invoice invoice = Invoice.builder()
                .invoiceDate(LocalDate.now())
                .invoiceType(invoiceDTO.getInvoiceType())
                .mechanic(mechanic)
                .vehicle(vehicle)
                .total(BigDecimal.ZERO)
                .isCancelled(false)
                .build();
        
        Invoice savedInvoice = invoiceRepository.save(invoice);
        return convertToDTO(savedInvoice);
    }
    
    // Anular una factura
    @Transactional
    public void cancelInvoice(Long id) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + id));
        
        if (invoice.getIsCancelled()) {
            throw new RuntimeException("La factura ya está cancelada");
        }
        
        invoice.setIsCancelled(true);
        invoiceRepository.save(invoice);
    }
    
    // Actualizar el total de una factura
    @Transactional
    public InvoiceDTO updateTotal(Long id, BigDecimal total) {
        Invoice invoice = invoiceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + id));
        
        if (invoice.getIsCancelled()) {
            throw new RuntimeException("No se puede actualizar una factura cancelada");
        }
        
        invoice.setTotal(total);
        Invoice updatedInvoice = invoiceRepository.save(invoice);
        return convertToDTO(updatedInvoice);
    }
    
    // Buscar facturas por tipo
    public List<InvoiceDTO> getInvoicesByType(InvoiceType type) {
        return invoiceRepository.findByInvoiceType(type)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Convertir entidad a DTO
    private InvoiceDTO convertToDTO(Invoice invoice) {
        return InvoiceDTO.builder()
                .invoiceId(invoice.getInvoiceId())
                .invoiceDate(invoice.getInvoiceDate())
                .invoiceType(invoice.getInvoiceType())
                .mechanicId(invoice.getMechanic().getMechanicId())
                .mechanicName(invoice.getMechanic().getName())
                .vehiclePlate(invoice.getVehicle().getPlate())
                .vehicleBrand(invoice.getVehicle().getBrand())
                .vehicleModel(invoice.getVehicle().getModel())
                .total(invoice.getTotal())
                .isCancelled(invoice.getIsCancelled())
                .build();
    }
}