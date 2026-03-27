package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.LiquidationAlreadyExistsException;
import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.DailyLiquidationRepository;
import com.chepamotos.domain.port.InvoiceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
public class LiquidationService {

    private final DailyLiquidationRepository dailyLiquidationRepository;
    private final InvoiceRepository invoiceRepository;
    private final MechanicService mechanicService;

    public LiquidationService(
            DailyLiquidationRepository dailyLiquidationRepository,
            InvoiceRepository invoiceRepository,
            MechanicService mechanicService
    ) {
        this.dailyLiquidationRepository = dailyLiquidationRepository;
        this.invoiceRepository = invoiceRepository;
        this.mechanicService = mechanicService;
    }

    @Transactional(readOnly = true)
    public List<DailyLiquidation> list(Long mechanicId, LocalDate date) {
        return dailyLiquidationRepository.findAll(mechanicId, date);
    }

    @Transactional
    public List<DailyLiquidation> create(LocalDate date, Long mechanicId) {
        if (date == null) {
            throw new IllegalArgumentException("date is required");
        }
        LocalDate liquidationDate = date;

        if (mechanicId != null) {
            Mechanic mechanic = mechanicService.getById(mechanicId);
            return List.of(createForMechanic(mechanic, liquidationDate));
        }

        List<Long> mechanicIds = invoiceRepository.findActiveMechanicIdsWithActiveServiceInvoicesByDate(liquidationDate)
                .stream()
                .sorted(Comparator.naturalOrder())
                .toList();

        List<DailyLiquidation> created = new ArrayList<>();
        for (Long id : mechanicIds) {
            Mechanic mechanic = mechanicService.getById(id);
            created.add(createForMechanic(mechanic, liquidationDate));
        }

        return List.copyOf(created);
    }

    private DailyLiquidation createForMechanic(Mechanic mechanic, LocalDate date) {
        if (dailyLiquidationRepository.existsByMechanicAndDate(mechanic.id(), date)) {
            throw new LiquidationAlreadyExistsException(mechanic.id(), date);
        }

        var totalRevenue = invoiceRepository.sumActiveServiceLaborByMechanicAndDate(mechanic.id(), date);
        int invoiceCount = invoiceRepository.countActiveServiceInvoicesByMechanicAndDate(mechanic.id(), date);

        DailyLiquidation liquidation = DailyLiquidation.create(mechanic, date, totalRevenue, invoiceCount);
        return dailyLiquidationRepository.save(liquidation);
    }
}
