package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.DailyLiquidation;
import com.chepamotos.domain.port.DailyLiquidationRepository;
import com.chepamotos.infrastructure.mapper.DailyLiquidationEntityMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public class DailyLiquidationRepositoryAdapter implements DailyLiquidationRepository {

    private final SpringDataDailyLiquidationRepository springDataDailyLiquidationRepository;

    public DailyLiquidationRepositoryAdapter(SpringDataDailyLiquidationRepository springDataDailyLiquidationRepository) {
        this.springDataDailyLiquidationRepository = springDataDailyLiquidationRepository;
    }

    @Override
    public List<DailyLiquidation> findAll(Long mechanicId, LocalDate date) {
        List<com.chepamotos.infrastructure.entity.DailyLiquidation> liquidations;
        if (mechanicId != null && date != null) {
            liquidations = springDataDailyLiquidationRepository.findAllByMechanicIdAndDate(mechanicId, date);
        } else if (mechanicId != null) {
            liquidations = springDataDailyLiquidationRepository.findAllByMechanicId(mechanicId);
        } else if (date != null) {
            liquidations = springDataDailyLiquidationRepository.findAllByDate(date);
        } else {
            liquidations = springDataDailyLiquidationRepository.findAllWithMechanic();
        }

        return liquidations
                .stream()
                .map(DailyLiquidationEntityMapper::toDomain)
                .toList();
    }

    @Override
    public boolean existsByMechanicAndDate(Long mechanicId, LocalDate date) {
        return springDataDailyLiquidationRepository.existsByMechanic_IdAndDate(mechanicId, date);
    }

    @Override
    public DailyLiquidation save(DailyLiquidation dailyLiquidation) {
        var entity = DailyLiquidationEntityMapper.toEntity(dailyLiquidation);
        var saved = springDataDailyLiquidationRepository.save(entity);
        return DailyLiquidationEntityMapper.toDomain(saved);
    }
}