package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.AuthTokens;
import com.chepamotos.domain.port.in.AuthApplicationUseCase;
import com.chepamotos.domain.usecase.auth.LoginUseCase;
import com.chepamotos.domain.usecase.auth.LogoutUseCase;
import com.chepamotos.domain.usecase.auth.RefreshSessionUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AuthApplicationService implements AuthApplicationUseCase {

    private final LoginUseCase loginUseCase;
    private final RefreshSessionUseCase refreshSessionUseCase;
    private final LogoutUseCase logoutUseCase;

    public AuthApplicationService(LoginUseCase loginUseCase,
                                  RefreshSessionUseCase refreshSessionUseCase,
                                  LogoutUseCase logoutUseCase) {
        this.loginUseCase = loginUseCase;
        this.refreshSessionUseCase = refreshSessionUseCase;
        this.logoutUseCase = logoutUseCase;
    }

    @Transactional
    @Override
    public AuthTokens login(String username, String password) {
        return loginUseCase.execute(username, password);
    }

    @Transactional
    @Override
    public AuthTokens refresh(String refreshToken) {
        return refreshSessionUseCase.execute(refreshToken);
    }

    @Transactional
    @Override
    public void logout(String refreshToken) {
        logoutUseCase.execute(refreshToken);
    }
}
