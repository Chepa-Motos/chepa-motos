package com.chepamotos.domain.port;

import com.chepamotos.domain.model.Invoice;
import com.chepamotos.domain.model.InvoiceItem;
import com.chepamotos.domain.model.InvoiceType;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface InvoiceRepository {

    List<Invoice> findAll();

    List<Invoice> findAllByFilters(LocalDate date, InvoiceType type, Long mechanicId, boolean cancelled);

    Optional<Invoice> findById(Long id);

    BigDecimal sumActiveServiceLaborByMechanicAndDate(Long mechanicId, LocalDate date);

    int countActiveServiceInvoicesByMechanicAndDate(Long mechanicId, LocalDate date);

    List<Long> findActiveMechanicIdsWithActiveServiceInvoicesByDate(LocalDate date);

    List<InvoiceItem> findSuggestionsByModelAndDescription(String model, String descriptionPrefix);

    Invoice save(Invoice invoice);
}
