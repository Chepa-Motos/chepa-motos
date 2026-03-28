package com.chepamotos.domain.usecase.mechanic;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 */
public final class GetMechanicByIdUseCase {

    private final MechanicRepository mechanicRepository;

    public GetMechanicByIdUseCase(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    public Mechanic execute(Long mechanicId) {
        return mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new MechanicNotFoundException(mechanicId));
    }
}
