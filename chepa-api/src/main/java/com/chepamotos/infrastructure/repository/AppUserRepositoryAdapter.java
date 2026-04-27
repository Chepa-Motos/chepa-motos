package com.chepamotos.infrastructure.repository;

import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.infrastructure.mapper.AppUserEntityMapper;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class AppUserRepositoryAdapter implements AppUserRepository {

    private final SpringDataAppUserRepository springDataAppUserRepository;

    public AppUserRepositoryAdapter(SpringDataAppUserRepository springDataAppUserRepository) {
        this.springDataAppUserRepository = springDataAppUserRepository;
    }

    @Override
    public Optional<AppUser> findById(Long id) {
        return springDataAppUserRepository.findById(id)
                .map(AppUserEntityMapper::toDomain);
    }

    @Override
    public Optional<AppUser> findByUsername(String username) {
        return springDataAppUserRepository.findByUsername(username == null ? null : username.trim().toLowerCase())
                .map(AppUserEntityMapper::toDomain);
    }

    @Override
    public AppUser save(AppUser user) {
        var entityToSave = AppUserEntityMapper.toEntity(user);
        var savedEntity = springDataAppUserRepository.save(entityToSave);
        return AppUserEntityMapper.toDomain(savedEntity);
    }
}
