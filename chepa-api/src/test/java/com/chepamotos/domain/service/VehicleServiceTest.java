package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.VehicleNotFoundException;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleServiceTest {

    @Mock
    private VehicleRepository vehicleRepository;

    @InjectMocks
    private VehicleService vehicleService;

    @Test
    void getByPlate_normalizesTrimAndUppercase_beforeLookup() {
        Vehicle expected = Vehicle.restore(1L, "BXR42H", "Boxer 150 2021");
        when(vehicleRepository.findByPlate("BXR42H")).thenReturn(Optional.of(expected));

        Vehicle result = vehicleService.getByPlate("  bxr42h  ");

        assertEquals(expected, result);
        verify(vehicleRepository).findByPlate("BXR42H");
    }

    @Test
    void getByPlate_whenPlateExists_returnsVehicle() {
        Vehicle expected = Vehicle.restore(2L, "YMH19F", "Yamaha FZ 2022");
        when(vehicleRepository.findByPlate("YMH19F")).thenReturn(Optional.of(expected));

        Vehicle result = vehicleService.getByPlate("YMH19F");

        assertEquals(expected, result);
        verify(vehicleRepository).findByPlate("YMH19F");
    }

    @Test
    void getByPlate_whenBlank_throwsValidationError() {
        assertThrows(IllegalArgumentException.class, () -> vehicleService.getByPlate("   "));
    }

    @Test
    void getByPlate_whenMissing_throwsVehicleNotFoundException() {
        when(vehicleRepository.findByPlate("NOEXISTE1")).thenReturn(Optional.empty());

        assertThrows(VehicleNotFoundException.class, () -> vehicleService.getByPlate("noexiste1"));
        verify(vehicleRepository).findByPlate("NOEXISTE1");
    }
}
