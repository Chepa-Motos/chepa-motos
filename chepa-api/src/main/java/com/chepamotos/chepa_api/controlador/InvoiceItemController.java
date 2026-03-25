package com.chepamotos.chepa_api.controlador;

import com.chepamotos.chepa_api.dto.InvoiceItemDTO;
import com.chepamotos.chepa_api.servicio.InvoiceItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/items")
@RequiredArgsConstructor
public class InvoiceItemController {
    
    private final InvoiceItemService invoiceItemService;
    
    // GET /api/items/factura/{invoiceId} - Listar ítems de una factura
    @GetMapping("/factura/{invoiceId}")
    public ResponseEntity<List<InvoiceItemDTO>> getItemsByInvoiceId(@PathVariable Long invoiceId) {
        List<InvoiceItemDTO> items = invoiceItemService.getItemsByInvoiceId(invoiceId);
        return ResponseEntity.ok(items);
    }
    
    // GET /api/items/{id} - Obtener un ítem por ID
    @GetMapping("/{id}")
    public ResponseEntity<InvoiceItemDTO> getItemById(@PathVariable Long id) {
        InvoiceItemDTO item = invoiceItemService.getItemById(id);
        return ResponseEntity.ok(item);
    }
    
    // POST /api/items - Crear un nuevo ítem
    @PostMapping
    public ResponseEntity<InvoiceItemDTO> createItem(@RequestBody InvoiceItemDTO itemDTO) {
        InvoiceItemDTO createdItem = invoiceItemService.createItem(itemDTO);
        return ResponseEntity.ok(createdItem);
    }
    
    // PUT /api/items/{id} - Actualizar un ítem
    @PutMapping("/{id}")
    public ResponseEntity<InvoiceItemDTO> updateItem(
            @PathVariable Long id, 
            @RequestBody InvoiceItemDTO itemDTO) {
        InvoiceItemDTO updatedItem = invoiceItemService.updateItem(id, itemDTO);
        return ResponseEntity.ok(updatedItem);
    }
    
    // DELETE /api/items/{id} - Eliminar un ítem
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteItem(@PathVariable Long id) {
        invoiceItemService.deleteItem(id);
        return ResponseEntity.noContent().build();
    }
}