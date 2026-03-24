package com.chepamotos.chepa_api.repositorio;

import com.chepamotos.chepa_api.enums.InvoiceType;
import com.chepamotos.chepa_api.modelo.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface InvoiceRepository extends JpaRepository<Invoice, Long> {
    
    // Buscar facturas por tipo
    List<Invoice> findByInvoiceType(InvoiceType invoiceType);
    
    // Buscar facturas no canceladas
    List<Invoice> findByIsCancelledFalse();
    
    // Buscar facturas por fecha
    List<Invoice> findByInvoiceDate(LocalDate date);
    
    // Buscar facturas por rango de fechas
    List<Invoice> findByInvoiceDateBetween(LocalDate startDate, LocalDate endDate);
    
    // Buscar facturas por mecánico
    List<Invoice> findByMechanic_MechanicId(Long mechanicId);
    
    // Buscar facturas por vehículo
    List<Invoice> findByVehicle_Plate(String plate);
}