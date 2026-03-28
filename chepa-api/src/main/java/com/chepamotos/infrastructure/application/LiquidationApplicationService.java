package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.usecase.liquidation.CreateLiquidationUseCase;
import com.chepamotos.domain.usecase.liquidation.ListLiquidationsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
public class LiquidationApplicationService {

    private final ListLiquidationsUseCase listLiquidationsUseCase;
    private final CreateLiquidationUseCase createLiquidationUseCase;

    public LiquidationApplicationService(
            ListLiquidationsUseCase listLiquidationsUseCase,
            CreateLiquidationUseCase createLiquidationUseCase) {
        this.listLiquidationsUseCase = listLiquidationsUseCase;
        this.createLiquidationUseCase = createLiquidationUseCase;
    }

    @Transactional(readOnly = true)
    public List<DailyLiquidation> list(Long mechanicId, LocalDate date) {
        return listLiquidationsUseCase.execute(mechanicId, date);
    }

    @Transactional
    public List<DailyLiquidation> create(LocalDate date, Long mechanicId) {
        return createLiquidationUseCase.execute(date, mechanicId);
    }
}