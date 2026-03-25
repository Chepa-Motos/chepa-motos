package com.chepamotos.chepa_api.servicio;

import com.chepamotos.chepa_api.dto.InvoiceItemDTO;
import com.chepamotos.chepa_api.modelo.Invoice;
import com.chepamotos.chepa_api.modelo.InvoiceItem;
import com.chepamotos.chepa_api.repositorio.InvoiceItemRepository;
import com.chepamotos.chepa_api.repositorio.InvoiceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InvoiceItemService {
    
    private final InvoiceItemRepository invoiceItemRepository;
    private final InvoiceRepository invoiceRepository;
    
    // Listar ítems por factura
    public List<InvoiceItemDTO> getItemsByInvoiceId(Long invoiceId) {
        return invoiceItemRepository.findByInvoice_InvoiceId(invoiceId)
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    // Obtener un ítem por ID
    public InvoiceItemDTO getItemById(Long id) {
        InvoiceItem item = invoiceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado con ID: " + id));
        return convertToDTO(item);
    }
    
    // Crear un nuevo ítem
    @Transactional
    public InvoiceItemDTO createItem(InvoiceItemDTO itemDTO) {
        Invoice invoice = invoiceRepository.findById(itemDTO.getInvoiceId())
                .orElseThrow(() -> new RuntimeException("Factura no encontrada con ID: " + itemDTO.getInvoiceId()));
        
        if (invoice.getIsCancelled()) {
            throw new RuntimeException("No se pueden agregar ítems a una factura cancelada");
        }
        
        InvoiceItem item = InvoiceItem.builder()
                .invoice(invoice)
                .description(itemDTO.getDescription())
                .quantity(itemDTO.getQuantity())
                .unitPrice(itemDTO.getUnitPrice())
                .build();
        
        InvoiceItem savedItem = invoiceItemRepository.save(item);
        
        // Actualizar el total de la factura
        updateInvoiceTotal(invoice);
        
        return convertToDTO(savedItem);
    }
    
    // Actualizar un ítem
    @Transactional
    public InvoiceItemDTO updateItem(Long id, InvoiceItemDTO itemDTO) {
        InvoiceItem item = invoiceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado con ID: " + id));
        
        if (item.getInvoice().getIsCancelled()) {
            throw new RuntimeException("No se pueden modificar ítems de una factura cancelada");
        }
        
        item.setDescription(itemDTO.getDescription());
        item.setQuantity(itemDTO.getQuantity());
        item.setUnitPrice(itemDTO.getUnitPrice());
        
        InvoiceItem updatedItem = invoiceItemRepository.save(item);
        
        // Actualizar el total de la factura
        updateInvoiceTotal(item.getInvoice());
        
        return convertToDTO(updatedItem);
    }
    
    // Eliminar un ítem
    @Transactional
    public void deleteItem(Long id) {
        InvoiceItem item = invoiceItemRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Ítem no encontrado con ID: " + id));
        
        if (item.getInvoice().getIsCancelled()) {
            throw new RuntimeException("No se pueden eliminar ítems de una factura cancelada");
        }
        
        Invoice invoice = item.getInvoice();
        invoiceItemRepository.delete(item);
        
        // Actualizar el total de la factura
        updateInvoiceTotal(invoice);
    }
    
    // Actualizar el total de la factura sumando todos sus ítems
    private void updateInvoiceTotal(Invoice invoice) {
        List<InvoiceItem> items = invoiceItemRepository.findByInvoice_InvoiceId(invoice.getInvoiceId());
        
        BigDecimal total = items.stream()
                .map(item -> item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        invoice.setTotal(total);
        invoiceRepository.save(invoice);
    }
    
    // Convertir entidad a DTO
    private InvoiceItemDTO convertToDTO(InvoiceItem item) {
        BigDecimal subtotal = item.getUnitPrice().multiply(BigDecimal.valueOf(item.getQuantity()));
        
        return InvoiceItemDTO.builder()
                .itemId(item.getItemId())
                .invoiceId(item.getInvoice().getInvoiceId())
                .description(item.getDescription())
                .quantity(item.getQuantity())
                .unitPrice(item.getUnitPrice())
                .subtotal(subtotal)
                .build();
    }
}