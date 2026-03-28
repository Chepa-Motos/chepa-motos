package com.chepamotos.domain.usecase.vehicle;

import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;

import java.util.Objects;

/**
 * Pure domain use case (strict clean architecture): no Spring annotations/imports.
 *
 * Vehicle upsert logic for SERVICE invoices:
 * - plate exists  → update model, reuse record
 * - plate missing → create new vehicle record
 * Both paths are transactional via the outer application service facade.
 */
public final class ResolveVehicleForServiceInvoiceUseCase {

    private final VehicleRepository vehicleRepository;

    public ResolveVehicleForServiceInvoiceUseCase(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    public Vehicle execute(String rawPlate, String rawModel) {
        String normalizedPlate = GetVehicleByPlateUseCase.normalizePlate(rawPlate);
        String normalizedModel = normalizeModel(rawModel);

        return vehicleRepository.findByPlate(normalizedPlate)
                .map(existing -> vehicleRepository.save(existing.withModel(normalizedModel)))
                .orElseGet(() -> vehicleRepository.save(Vehicle.createNew(normalizedPlate, normalizedModel)));
    }

    private static String normalizeModel(String rawModel) {
        String normalized = Objects.requireNonNull(rawModel, "Vehicle model is required")
                .trim();

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vehicle model cannot be blank");
        }

        if (normalized.length() > 100) {
            throw new IllegalArgumentException("Vehicle model cannot exceed 100 characters");
        }

        return normalized;
    }
}
