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
        return springDataDailyLiquidationRepository.findAllByFilters(mechanicId, date)
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