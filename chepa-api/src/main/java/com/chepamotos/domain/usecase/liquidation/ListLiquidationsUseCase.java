package com.chepamotos.domain.usecase.liquidation;

import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.port.DailyLiquidationRepository;

import java.time.LocalDate;
import java.util.List;

public class ListLiquidationsUseCase {

    private final DailyLiquidationRepository dailyLiquidationRepository;

    public ListLiquidationsUseCase(DailyLiquidationRepository dailyLiquidationRepository) {
        this.dailyLiquidationRepository = dailyLiquidationRepository;
    }

    public List<DailyLiquidation> execute(Long mechanicId, LocalDate date) {
        return dailyLiquidationRepository.findAll(mechanicId, date);
    }
}