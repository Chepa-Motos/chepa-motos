package com.chepamotos.infrastructure.repository;

import com.chepamotos.infrastructure.entity.DailyLiquidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataDailyLiquidationRepository extends JpaRepository<DailyLiquidation, Long> {

    @Query("SELECT d FROM DailyLiquidation d JOIN FETCH d.mechanic m WHERE (:mechanicId IS NULL OR m.id = :mechanicId) AND (:date IS NULL OR d.date = :date)")
    List<DailyLiquidation> findAllByFilters(@Param("mechanicId") Long mechanicId, @Param("date") LocalDate date);

    boolean existsByMechanic_IdAndDate(Long mechanicId, LocalDate date);
}