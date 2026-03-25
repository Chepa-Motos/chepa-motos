package com.chepamotos.chepa_api.repositorio;

import com.chepamotos.chepa_api.modelo.InvoiceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface InvoiceItemRepository extends JpaRepository<InvoiceItem, Long> {
    
    // Buscar ítems por factura
    List<InvoiceItem> findByInvoice_InvoiceId(Long invoiceId);
    
    // Buscar ítems por descripción
    List<InvoiceItem> findByDescriptionContainingIgnoreCase(String description);
}