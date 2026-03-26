package com.chepamotos.infrastructure.repository;

import com.chepamotos.infrastructure.entity.DailyLiquidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface SpringDataDailyLiquidationRepository extends JpaRepository<DailyLiquidation, Long> {

    @Query("SELECT d FROM DailyLiquidation d JOIN FETCH d.mechanic")
    List<DailyLiquidation> findAllWithMechanic();

    @Query("SELECT d FROM DailyLiquidation d JOIN FETCH d.mechanic m WHERE m.id = :mechanicId")
    List<DailyLiquidation> findAllByMechanicId(@Param("mechanicId") Long mechanicId);

    @Query("SELECT d FROM DailyLiquidation d JOIN FETCH d.mechanic WHERE d.date = :date")
    List<DailyLiquidation> findAllByDate(@Param("date") LocalDate date);

    @Query("SELECT d FROM DailyLiquidation d JOIN FETCH d.mechanic m WHERE m.id = :mechanicId AND d.date = :date")
    List<DailyLiquidation> findAllByMechanicIdAndDate(@Param("mechanicId") Long mechanicId, @Param("date") LocalDate date);

    boolean existsByMechanic_IdAndDate(Long mechanicId, LocalDate date);
}