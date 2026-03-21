package com.chepamotos.domain.exception;

public class MechanicNotFoundException extends RuntimeException {

    public MechanicNotFoundException(Long mechanicId) {
        super("Mechanic not found with id: " + mechanicId);
    }
}
