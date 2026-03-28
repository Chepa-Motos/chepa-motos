package com.chepamotos.domain.usecase.liquidation;

import com.chepamotos.domain.exception.LiquidationAlreadyExistsException;
import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.DailyLiquidationRepository;
import com.chepamotos.domain.port.InvoiceRepository;
import com.chepamotos.domain.port.MechanicRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class CreateLiquidationUseCase {

    private final DailyLiquidationRepository dailyLiquidationRepository;
    private final InvoiceRepository invoiceRepository;
    private final MechanicRepository mechanicRepository;

    public CreateLiquidationUseCase(
            DailyLiquidationRepository dailyLiquidationRepository,
            InvoiceRepository invoiceRepository,
            MechanicRepository mechanicRepository) {
        this.dailyLiquidationRepository = dailyLiquidationRepository;
        this.invoiceRepository = invoiceRepository;
        this.mechanicRepository = mechanicRepository;
    }

    public List<DailyLiquidation> execute(LocalDate date, Long mechanicId) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }

        if (mechanicId != null) {
            Mechanic mechanic = mechanicRepository.findById(mechanicId)
                    .orElseThrow(() -> new MechanicNotFoundException(mechanicId));
            return List.of(createForMechanic(mechanic, date));
        }

        List<Long> mechanicIds = invoiceRepository
                .findActiveMechanicIdsWithActiveServiceInvoicesByDate(date)
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<DailyLiquidation> created = new ArrayList<>();
        for (Long id : mechanicIds) {
            Mechanic mechanic = mechanicRepository.findById(id)
                    .orElseThrow(() -> new MechanicNotFoundException(id));
            created.add(createForMechanic(mechanic, date));
        }

        return List.copyOf(created);
    }

    private DailyLiquidation createForMechanic(Mechanic mechanic, LocalDate date) {
        if (dailyLiquidationRepository.existsByMechanicAndDate(mechanic.id(), date)) {
            throw new LiquidationAlreadyExistsException(mechanic.id(), date);
        }

        BigDecimal totalRevenue = invoiceRepository
                .sumActiveServiceLaborByMechanicAndDate(mechanic.id(), date);
        int invoiceCount = invoiceRepository
                .countActiveServiceInvoicesByMechanicAndDate(mechanic.id(), date);

        DailyLiquidation liquidation = DailyLiquidation.create(mechanic, date, totalRevenue, invoiceCount);
        return dailyLiquidationRepository.save(liquidation);
    }
}