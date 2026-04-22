package com.chepamotos.domain.port.in;

import com.chepamotos.domain.model.DailyLiquidation;

import java.time.LocalDate;
import java.util.List;

public interface LiquidationApplicationUseCase {

    List<DailyLiquidation> list(Long mechanicId, LocalDate date);

    List<DailyLiquidation> create(LocalDate date, Long mechanicId);
}