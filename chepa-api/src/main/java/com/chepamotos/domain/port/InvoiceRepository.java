package com.chepamotos.domain.port;

import com.chepamotos.domain.model.Invoice;

import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    List<Invoice> findAll();

    Optional<Invoice> findById(Long id);

    Invoice save(Invoice invoice);
}
