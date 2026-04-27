package com.chepamotos.domain.port;

import com.chepamotos.domain.model.AppUser;

import java.util.Optional;

public interface AppUserRepository {

    Optional<AppUser> findById(Long id);

    Optional<AppUser> findByUsername(String username);

    AppUser save(AppUser user);
}
