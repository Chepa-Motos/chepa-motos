package com.chepamotos.domain.port;

import com.chepamotos.domain.model.DailyLiquidation;

import java.time.LocalDate;
import java.util.List;

public interface DailyLiquidationRepository {

    List<DailyLiquidation> findAll(Long mechanicId, LocalDate date);

    boolean existsByMechanicAndDate(Long mechanicId, LocalDate date);

    DailyLiquidation save(DailyLiquidation dailyLiquidation);
}