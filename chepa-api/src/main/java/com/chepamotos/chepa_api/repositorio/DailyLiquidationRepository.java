package com.chepamotos.chepa_api.repositorio;

import com.chepamotos.chepa_api.modelo.DailyLiquidation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface DailyLiquidationRepository extends JpaRepository<DailyLiquidation, Long> {
    
    // Buscar liquidaciones por mecánico
    List<DailyLiquidation> findByMechanic_MechanicId(Long mechanicId);
    
    // Buscar liquidaciones por fecha
    List<DailyLiquidation> findByLiquidationDate(LocalDate date);
    
    // Buscar liquidación específica de un mecánico en una fecha
    Optional<DailyLiquidation> findByMechanic_MechanicIdAndLiquidationDate(Long mechanicId, LocalDate date);
    
    // Verificar si existe liquidación para un mecánico en una fecha
    boolean existsByMechanic_MechanicIdAndLiquidationDate(Long mechanicId, LocalDate date);
}