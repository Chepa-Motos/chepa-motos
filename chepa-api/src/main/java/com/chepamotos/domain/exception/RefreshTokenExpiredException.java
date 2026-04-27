package com.chepamotos.domain.exception;

public class RefreshTokenExpiredException extends RuntimeException {

    public RefreshTokenExpiredException() {
        super("Refresh token has expired");
    }
}
