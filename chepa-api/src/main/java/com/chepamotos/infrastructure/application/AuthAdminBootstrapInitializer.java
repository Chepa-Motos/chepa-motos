package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.model.UserRole;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.PasswordHasher;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;

@Component
public class AuthAdminBootstrapInitializer implements ApplicationRunner {

    private final AppUserRepository appUserRepository;
    private final PasswordHasher passwordHasher;
    private final Clock clock;
    private final String adminUsername;
    private final String adminPassword;

    public AuthAdminBootstrapInitializer(
            AppUserRepository appUserRepository,
            PasswordHasher passwordHasher,
            Clock clock,
            @Value("${auth.bootstrap.admin-username:}") String adminUsername,
            @Value("${auth.bootstrap.admin-password:}") String adminPassword
    ) {
        this.appUserRepository = appUserRepository;
        this.passwordHasher = passwordHasher;
        this.clock = clock;
        this.adminUsername = adminUsername;
        this.adminPassword = adminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        String normalizedUsername = adminUsername == null ? "" : adminUsername.trim();
        String normalizedPassword = adminPassword == null ? "" : adminPassword.trim();

        if (normalizedUsername.isEmpty() && normalizedPassword.isEmpty()) {
            return;
        }

        if (normalizedUsername.isEmpty() || normalizedPassword.isEmpty()) {
            throw new IllegalStateException("Both admin bootstrap username and password must be provided");
        }

        if (appUserRepository.findByUsername(normalizedUsername).isPresent()) {
            return;
        }

        String passwordHash = passwordHasher.hash(normalizedPassword);
        AppUser adminUser = AppUser.createNew(
                normalizedUsername,
                passwordHash,
                UserRole.GERENTE,
                LocalDateTime.now(clock)
        );
        appUserRepository.save(adminUser);
    }
}
