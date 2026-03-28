package com.chepamotos.domain.usecase.mechanic;

import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 */
public final class CreateMechanicUseCase {

    private final MechanicRepository mechanicRepository;

    public CreateMechanicUseCase(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    public Mechanic execute(String name) {
        Mechanic newMechanic = Mechanic.createNew(name);
        return mechanicRepository.save(newMechanic);
    }
}
