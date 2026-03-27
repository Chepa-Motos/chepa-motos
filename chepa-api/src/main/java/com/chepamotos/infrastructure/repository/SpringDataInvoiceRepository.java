package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.infrastructure.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface SpringDataInvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.mechanic LEFT JOIN FETCH i.vehicle LEFT JOIN FETCH i.items")
    List<Invoice> findAllWithDetails();

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.mechanic LEFT JOIN FETCH i.vehicle LEFT JOIN FETCH i.items WHERE i.id = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(i.laborAmount), 0) FROM Invoice i WHERE i.invoiceType = :invoiceType AND i.isCancelled = false AND i.mechanic.id = :mechanicId AND i.createdAt = :date")
    BigDecimal sumActiveServiceLaborByMechanicAndDate(@Param("invoiceType") InvoiceType invoiceType, @Param("mechanicId") Long mechanicId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceType = :invoiceType AND i.isCancelled = false AND i.mechanic.id = :mechanicId AND i.createdAt = :date")
    long countActiveServiceInvoicesByMechanicAndDate(@Param("invoiceType") InvoiceType invoiceType, @Param("mechanicId") Long mechanicId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT i.mechanic.id FROM Invoice i JOIN i.mechanic m WHERE i.invoiceType = :invoiceType AND i.isCancelled = false AND m.isActive = true AND i.createdAt = :date")
    List<Long> findActiveMechanicIdsWithActiveServiceInvoicesByDate(@Param("invoiceType") InvoiceType invoiceType, @Param("date") LocalDate date);
}
