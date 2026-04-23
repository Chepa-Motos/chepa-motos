package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.InvoiceType;
import com.chepamotos.infrastructure.entity.Invoice;
import com.chepamotos.infrastructure.entity.InvoiceItem;
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

            @Query("""
                SELECT DISTINCT i
                FROM Invoice i
                LEFT JOIN FETCH i.mechanic
                LEFT JOIN FETCH i.vehicle
                LEFT JOIN FETCH i.items
                WHERE FUNCTION('DATE', i.createdAt) = :date
                AND i.isCancelled = :cancelled
                ORDER BY i.createdAt DESC
                """)
            List<Invoice> findAllByDateAndCancelledWithDetails(
                @Param("date") LocalDate date,
                @Param("cancelled") boolean cancelled);

            @Query("""
                SELECT DISTINCT i
                FROM Invoice i
                LEFT JOIN FETCH i.mechanic
                LEFT JOIN FETCH i.vehicle
                LEFT JOIN FETCH i.items
                WHERE FUNCTION('DATE', i.createdAt) = :date
                  AND i.isCancelled = :cancelled
                  AND i.invoiceType = :invoiceType
                ORDER BY i.createdAt DESC
                """)
            List<Invoice> findAllByDateCancelledAndTypeWithDetails(
                @Param("date") LocalDate date,
                @Param("cancelled") boolean cancelled,
                @Param("invoiceType") InvoiceType invoiceType);

            @Query("""
                SELECT DISTINCT i
                FROM Invoice i
                LEFT JOIN FETCH i.mechanic
                LEFT JOIN FETCH i.vehicle
                LEFT JOIN FETCH i.items
                WHERE FUNCTION('DATE', i.createdAt) = :date
                  AND i.isCancelled = :cancelled
                  AND i.mechanic.id = :mechanicId
                ORDER BY i.createdAt DESC
                """)
            List<Invoice> findAllByDateCancelledAndMechanicWithDetails(
                @Param("date") LocalDate date,
                @Param("cancelled") boolean cancelled,
                @Param("mechanicId") Long mechanicId);

            @Query("""
                SELECT DISTINCT i
                FROM Invoice i
                LEFT JOIN FETCH i.mechanic
                LEFT JOIN FETCH i.vehicle
                LEFT JOIN FETCH i.items
                WHERE FUNCTION('DATE', i.createdAt) = :date
                  AND i.isCancelled = :cancelled
                  AND i.invoiceType = :invoiceType
                  AND i.mechanic.id = :mechanicId
                ORDER BY i.createdAt DESC
                """)
            List<Invoice> findAllByDateCancelledTypeAndMechanicWithDetails(
                @Param("date") LocalDate date,
                @Param("cancelled") boolean cancelled,
                @Param("invoiceType") InvoiceType invoiceType,
                @Param("mechanicId") Long mechanicId);

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.mechanic LEFT JOIN FETCH i.vehicle LEFT JOIN FETCH i.items WHERE i.id = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);

    @Query("SELECT COALESCE(SUM(i.laborAmount), 0) FROM Invoice i WHERE i.invoiceType = :invoiceType AND i.isCancelled = false AND i.mechanic.id = :mechanicId AND FUNCTION('DATE', i.createdAt) = :date")
    BigDecimal sumActiveServiceLaborByMechanicAndDate(@Param("invoiceType") InvoiceType invoiceType, @Param("mechanicId") Long mechanicId, @Param("date") LocalDate date);

    @Query("SELECT COUNT(i) FROM Invoice i WHERE i.invoiceType = :invoiceType AND i.isCancelled = false AND i.mechanic.id = :mechanicId AND FUNCTION('DATE', i.createdAt) = :date")
    long countActiveServiceInvoicesByMechanicAndDate(@Param("invoiceType") InvoiceType invoiceType, @Param("mechanicId") Long mechanicId, @Param("date") LocalDate date);

    @Query("SELECT DISTINCT i.mechanic.id FROM Invoice i JOIN i.mechanic m WHERE i.invoiceType = :invoiceType AND i.isCancelled = false AND m.isActive = true AND FUNCTION('DATE', i.createdAt) = :date")
    List<Long> findActiveMechanicIdsWithActiveServiceInvoicesByDate(@Param("invoiceType") InvoiceType invoiceType, @Param("date") LocalDate date);

    @Query(value = """
        WITH ranked_items AS (
            SELECT 
                ii.invoice_item_id,
                ii.invoice_id,
                ii.description,
                ii.quantity,
                ii.unit_price,
                ii.subtotal,
                ROW_NUMBER() OVER (PARTITION BY ii.description ORDER BY i.created_at DESC) as rn,
                COUNT(*) OVER (PARTITION BY ii.description) as frequency
            FROM invoice_item ii
            JOIN invoice i ON ii.invoice_id = i.invoice_id
            JOIN vehicle v ON i.vehicle_id = v.vehicle_id
            WHERE i.invoice_type = CAST(:invoiceType AS invoice_type)
            AND i.is_cancelled = false
            AND LOWER(v.model) ILIKE LOWER(:model || '%')
            AND LOWER(ii.description) ILIKE LOWER(:descriptionPrefix || '%')
        )
        SELECT 
            invoice_item_id,
            invoice_id,
            description,
            quantity,
            unit_price,
            subtotal
        FROM ranked_items
        WHERE rn = 1
        ORDER BY frequency DESC, description ASC
        LIMIT 10
        """, nativeQuery = true)
    List<InvoiceItem> findSuggestionsByModelAndDescription(
            @Param("invoiceType") String invoiceType,
            @Param("model") String model,
            @Param("descriptionPrefix") String descriptionPrefix);
}

