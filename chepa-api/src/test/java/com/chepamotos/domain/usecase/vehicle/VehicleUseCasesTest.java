package com.chepamotos.domain.usecase.vehicle;

import com.chepamotos.domain.exception.VehicleNotFoundException;
import com.chepamotos.domain.model.Vehicle;
import com.chepamotos.domain.port.VehicleRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VehicleUseCasesTest {

    @Mock
    private VehicleRepository vehicleRepository;

    // --- GetVehicleByPlateUseCase ---

    @Test
    void getByPlate_normalizesTrimAndUppercase_beforeLookup() {
        Vehicle expected = Vehicle.restore(1L, "BXR42H", "Boxer 150 2021");
        when(vehicleRepository.findByPlate("BXR42H")).thenReturn(Optional.of(expected));

        GetVehicleByPlateUseCase useCase = new GetVehicleByPlateUseCase(vehicleRepository);
        Vehicle result = useCase.execute("  bxr42h  ");

        assertEquals(expected, result);
        verify(vehicleRepository).findByPlate("BXR42H");
    }

    @Test
    void getByPlate_whenPlateExists_returnsVehicle() {
        Vehicle expected = Vehicle.restore(2L, "YMH19F", "Yamaha FZ 2022");
        when(vehicleRepository.findByPlate("YMH19F")).thenReturn(Optional.of(expected));

        GetVehicleByPlateUseCase useCase = new GetVehicleByPlateUseCase(vehicleRepository);
        Vehicle result = useCase.execute("YMH19F");

        assertEquals(expected, result);
        verify(vehicleRepository).findByPlate("YMH19F");
    }

    @Test
    void getByPlate_whenBlank_throwsIllegalArgumentException() {
        GetVehicleByPlateUseCase useCase = new GetVehicleByPlateUseCase(vehicleRepository);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("   "));
    }

    @Test
    void getByPlate_whenMissing_throwsVehicleNotFoundException() {
        when(vehicleRepository.findByPlate("NOEXISTE1")).thenReturn(Optional.empty());

        GetVehicleByPlateUseCase useCase = new GetVehicleByPlateUseCase(vehicleRepository);
        assertThrows(VehicleNotFoundException.class, () -> useCase.execute("noexiste1"));

        verify(vehicleRepository).findByPlate("NOEXISTE1");
    }

    // --- ResolveVehicleForServiceInvoiceUseCase ---

    @Test
    void resolve_whenPlateMissing_createsVehicle() {
        when(vehicleRepository.findByPlate("NEW123")).thenReturn(Optional.empty());
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveVehicleForServiceInvoiceUseCase useCase = new ResolveVehicleForServiceInvoiceUseCase(vehicleRepository);
        Vehicle result = useCase.execute("new123", " Boxer 150 2024 ");

        assertEquals("NEW123", result.plate());
        assertEquals("Boxer 150 2024", result.model());
        verify(vehicleRepository).findByPlate("NEW123");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void resolve_whenPlateExists_updatesModel() {
        Vehicle existing = Vehicle.restore(1L, "BXR42H", "Old Model");
        when(vehicleRepository.findByPlate("BXR42H")).thenReturn(Optional.of(existing));
        when(vehicleRepository.save(any(Vehicle.class))).thenAnswer(inv -> inv.getArgument(0));

        ResolveVehicleForServiceInvoiceUseCase useCase = new ResolveVehicleForServiceInvoiceUseCase(vehicleRepository);
        Vehicle result = useCase.execute("bxr42h", "New Model 2025");

        assertEquals(1L, result.id());
        assertEquals("BXR42H", result.plate());
        assertEquals("New Model 2025", result.model());
        verify(vehicleRepository).findByPlate("BXR42H");
        verify(vehicleRepository).save(any(Vehicle.class));
    }

    @Test
    void resolve_whenModelIsBlank_throwsIllegalArgumentException() {
        ResolveVehicleForServiceInvoiceUseCase useCase = new ResolveVehicleForServiceInvoiceUseCase(vehicleRepository);
        assertThrows(IllegalArgumentException.class, () -> useCase.execute("BXR42H", "   "));
    }
}
