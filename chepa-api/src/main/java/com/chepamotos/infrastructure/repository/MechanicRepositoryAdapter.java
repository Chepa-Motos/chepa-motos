package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.infrastructure.mapper.MechanicEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public class MechanicRepositoryAdapter implements MechanicRepository {

    private final SpringDataMechanicRepository springDataMechanicRepository;

    public MechanicRepositoryAdapter(SpringDataMechanicRepository springDataMechanicRepository) {
        this.springDataMechanicRepository = springDataMechanicRepository;
    }

    @Override
    public List<Mechanic> findAll() {
        return springDataMechanicRepository.findAll()
                .stream()
                .map(MechanicEntityMapper::toDomain)
                .toList();
    }

    @Override
    public List<Mechanic> findAllByActive(boolean active) {
        return springDataMechanicRepository.findByIsActive(active)
                .stream()
                .map(MechanicEntityMapper::toDomain)
                .toList();
    }

    @Override
    public Optional<Mechanic> findById(Long id) {
        return springDataMechanicRepository.findById(id)
                .map(MechanicEntityMapper::toDomain);
    }

    @Override
    public Mechanic save(Mechanic mechanic) {
        var entityToSave = MechanicEntityMapper.toEntity(mechanic);
        var savedEntity = springDataMechanicRepository.save(entityToSave);
        return MechanicEntityMapper.toDomain(savedEntity);
    }
}
