package com.chepamotos.domain.exception;

import java.time.LocalDate;

public class LiquidationAlreadyExistsException extends RuntimeException {

    public LiquidationAlreadyExistsException(Long mechanicId, LocalDate date) {
        super("Liquidation already exists for mechanic " + mechanicId + " on date " + date);
    }
}