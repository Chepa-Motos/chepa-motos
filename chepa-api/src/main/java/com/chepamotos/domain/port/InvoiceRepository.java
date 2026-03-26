package com.chepamotos.domain.port;

import com.chepamotos.domain.model.Invoice;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    List<Invoice> findAll();

    Optional<Invoice> findById(Long id);

    BigDecimal sumActiveServiceLaborByMechanicAndDate(Long mechanicId, LocalDate date);

    int countActiveServiceInvoicesByMechanicAndDate(Long mechanicId, LocalDate date);

    List<Long> findActiveMechanicIdsWithActiveServiceInvoicesByDate(LocalDate date);

    Invoice save(Invoice invoice);
}
