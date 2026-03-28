package com.chepamotos.domain.usecase.mechanic;

import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;

import java.util.List;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 */
public final class ListMechanicsUseCase {

    private final MechanicRepository mechanicRepository;

    public ListMechanicsUseCase(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    public List<Mechanic> execute(boolean active) {
        return mechanicRepository.findAllByActive(active);
    }

    // Keep this helper for parity with legacy service while peers migrate other slices.
    public List<Mechanic> executeAll() {
        return mechanicRepository.findAll();
    }
}
