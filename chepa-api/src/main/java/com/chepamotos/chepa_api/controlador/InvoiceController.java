package com.chepamotos.chepa_api.controlador;

import com.chepamotos.chepa_api.dto.InvoiceDTO;
import com.chepamotos.chepa_api.enums.InvoiceType;
import com.chepamotos.chepa_api.servicio.InvoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/facturas")
@RequiredArgsConstructor
public class InvoiceController {
    
    private final InvoiceService invoiceService;
    
    // GET /api/facturas - Listar todas las facturas activas
    @GetMapping
    public ResponseEntity<List<InvoiceDTO>> getAllActiveInvoices() {
        List<InvoiceDTO> invoices = invoiceService.getAllActiveInvoices();
        return ResponseEntity.ok(invoices);
    }
    
    // GET /api/facturas/{id} - Obtener una factura por ID
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceDTO> getInvoiceById(@PathVariable Long id) {
        InvoiceDTO invoice = invoiceService.getInvoiceById(id);
        return ResponseEntity.ok(invoice);
    }
    
    // POST /api/facturas - Crear una nueva factura
    @PostMapping
    public ResponseEntity<InvoiceDTO> createInvoice(@RequestBody InvoiceDTO invoiceDTO) {
        InvoiceDTO createdInvoice = invoiceService.createInvoice(invoiceDTO);
        return ResponseEntity.ok(createdInvoice);
    }
    
    // PUT /api/facturas/{id}/anular - Anular una factura
    @PutMapping("/{id}/anular")
    public ResponseEntity<Void> cancelInvoice(@PathVariable Long id) {
        invoiceService.cancelInvoice(id);
        return ResponseEntity.noContent().build();
    }
    
    // GET /api/facturas/tipo/{tipo} - Buscar facturas por tipo
    @GetMapping("/tipo/{tipo}")
    public ResponseEntity<List<InvoiceDTO>> getInvoicesByType(@PathVariable InvoiceType tipo) {
        List<InvoiceDTO> invoices = invoiceService.getInvoicesByType(tipo);
        return ResponseEntity.ok(invoices);
    }
}