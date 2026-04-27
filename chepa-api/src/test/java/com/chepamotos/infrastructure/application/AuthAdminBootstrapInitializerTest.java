package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.AppUser;
import com.chepamotos.domain.port.AppUserRepository;
import com.chepamotos.domain.port.PasswordHasher;
import org.junit.jupiter.api.Test;
import org.springframework.boot.DefaultApplicationArguments;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class AuthAdminBootstrapInitializerTest {

    private static final Clock FIXED_CLOCK = Clock.fixed(Instant.parse("2026-01-28T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void run_whenBootstrapCredentialsMissing_doesNothing() throws Exception {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);

        AuthAdminBootstrapInitializer initializer = new AuthAdminBootstrapInitializer(
                appUserRepository,
                passwordHasher,
                FIXED_CLOCK,
                "",
                ""
        );

        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(appUserRepository, never()).save(any());
        verify(passwordHasher, never()).hash(any());
    }

    @Test
    void run_whenOnlyOneBootstrapCredentialProvided_throwsIllegalStateException() {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);

        AuthAdminBootstrapInitializer initializer = new AuthAdminBootstrapInitializer(
                appUserRepository,
                passwordHasher,
                FIXED_CLOCK,
                "gerente",
                ""
        );

        assertThrows(IllegalStateException.class, () -> initializer.run(new DefaultApplicationArguments(new String[]{})));
    }

    @Test
    void run_whenUserAlreadyExists_doesNotCreateDuplicate() throws Exception {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);

        when(appUserRepository.findByUsername("gerente"))
                .thenReturn(Optional.of(AppUser.restore(1L, "gerente", "hash", com.chepamotos.domain.model.UserRole.GERENTE, true, java.time.LocalDateTime.now(FIXED_CLOCK))));

        AuthAdminBootstrapInitializer initializer = new AuthAdminBootstrapInitializer(
                appUserRepository,
                passwordHasher,
                FIXED_CLOCK,
                "gerente",
                "password"
        );

        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(passwordHasher, never()).hash(any());
        verify(appUserRepository, never()).save(any());
    }

    @Test
    void run_whenBootstrapCredentialsProvidedAndUserMissing_createsAdminUser() throws Exception {
        AppUserRepository appUserRepository = mock(AppUserRepository.class);
        PasswordHasher passwordHasher = mock(PasswordHasher.class);

        when(appUserRepository.findByUsername("gerente")).thenReturn(Optional.empty());
        when(passwordHasher.hash("password")).thenReturn("hashed-password");

        AuthAdminBootstrapInitializer initializer = new AuthAdminBootstrapInitializer(
                appUserRepository,
                passwordHasher,
                FIXED_CLOCK,
                "gerente",
                "password"
        );

        initializer.run(new DefaultApplicationArguments(new String[]{}));

        verify(appUserRepository).findByUsername("gerente");
        verify(passwordHasher).hash("password");
        verify(appUserRepository).save(any(AppUser.class));
    }
}
