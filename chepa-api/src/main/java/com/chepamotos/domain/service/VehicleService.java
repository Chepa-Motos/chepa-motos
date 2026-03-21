package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.VehicleNotFoundException;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;
import java.util.Objects;

@Service
public class VehicleService {

    private final VehicleRepository vehicleRepository;

    public VehicleService(VehicleRepository vehicleRepository) {
        this.vehicleRepository = vehicleRepository;
    }

    @Transactional(readOnly = true)
    public Vehicle getByPlate(String rawPlate) {
        String normalizedPlate = normalizePlate(rawPlate);
        return vehicleRepository.findByPlate(normalizedPlate)
                .orElseThrow(() -> new VehicleNotFoundException(normalizedPlate));
    }

    private String normalizePlate(String rawPlate) {
        String normalized = Objects.requireNonNull(rawPlate, "Vehicle plate is required")
                .trim()
                .toUpperCase(Locale.ROOT);

        if (normalized.isEmpty()) {
            throw new IllegalArgumentException("Vehicle plate cannot be blank");
        }

        return normalized;
    }
}
