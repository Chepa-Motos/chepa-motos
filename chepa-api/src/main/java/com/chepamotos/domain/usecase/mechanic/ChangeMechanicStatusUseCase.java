package com.chepamotos.domain.usecase.mechanic;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 */
public final class ChangeMechanicStatusUseCase {

    private final MechanicRepository mechanicRepository;

    public ChangeMechanicStatusUseCase(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    public Mechanic execute(Long mechanicId, boolean active) {
        Mechanic existingMechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new MechanicNotFoundException(mechanicId));

        Mechanic updatedMechanic = existingMechanic.withStatus(active);
        return mechanicRepository.save(updatedMechanic);
    }
}
