package com.chepamotos.domain.port.in;

import com.chepamotos.domain.model.AuthTokens;

public interface AuthApplicationUseCase {

    AuthTokens login(String username, String password);

    AuthTokens refresh(String refreshToken);

    void logout(String refreshToken);
}
