package com.chepamotos.infrastructure.repository;

import com.chepamotos.infrastructure.entity.Invoice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SpringDataInvoiceRepository extends JpaRepository<Invoice, Long> {

    @Query("SELECT DISTINCT i FROM Invoice i LEFT JOIN FETCH i.mechanic LEFT JOIN FETCH i.vehicle LEFT JOIN FETCH i.items")
    List<Invoice> findAllWithDetails();

    @Query("SELECT i FROM Invoice i LEFT JOIN FETCH i.mechanic LEFT JOIN FETCH i.vehicle LEFT JOIN FETCH i.items WHERE i.id = :id")
    Optional<Invoice> findByIdWithDetails(@Param("id") Long id);
}
