package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MechanicServiceTest {

    @Mock
    private MechanicRepository mechanicRepository;

    @InjectMocks
    private MechanicService mechanicService;

    @Test
    void listAll_returnsRepositoryResults() {
        List<Mechanic> expected = List.of(
                Mechanic.restore(1L, "Jose", true),
                Mechanic.restore(2L, "Carlos", false)
        );
        when(mechanicRepository.findAll()).thenReturn(expected);

        List<Mechanic> result = mechanicService.listAll();

        assertEquals(expected, result);
        verify(mechanicRepository).findAll();
    }

    @Test
    void listByActive_returnsRepositoryFilteredResults() {
        List<Mechanic> expected = List.of(Mechanic.restore(1L, "Jose", true));
        when(mechanicRepository.findAllByActive(anyBoolean())).thenReturn(expected);

        List<Mechanic> result = mechanicService.listByActive(true);

        assertEquals(expected, result);
        verify(mechanicRepository).findAllByActive(true);
    }

    @Test
    void getById_whenExists_returnsMechanic() {
        Mechanic expected = Mechanic.restore(1L, "Jose", true);
        when(mechanicRepository.findById(anyLong())).thenReturn(Optional.of(expected));

        Mechanic result = mechanicService.getById(1L);

        assertEquals(expected, result);
        verify(mechanicRepository).findById(1L);
    }

    @Test
    void getById_whenMissing_throwsMechanicNotFoundException() {
        when(mechanicRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(MechanicNotFoundException.class, () -> mechanicService.getById(99L));
        verify(mechanicRepository).findById(99L);
    }

    @Test
    void create_buildsDomainMechanicAndSaves() {
        Mechanic saved = Mechanic.restore(5L, "Carlos", true);
        when(mechanicRepository.save(any(Mechanic.class))).thenReturn(saved);

        Mechanic result = mechanicService.create("  Carlos  ");

        assertEquals(saved, result);
        verify(mechanicRepository).save(any(Mechanic.class));
    }

    @Test
    void changeStatus_whenMechanicExists_updatesAndSaves() {
        Mechanic existing = Mechanic.restore(1L, "Jose", true);
        Mechanic updated = Mechanic.restore(1L, "Jose", false);

        when(mechanicRepository.findById(anyLong())).thenReturn(Optional.of(existing));
        when(mechanicRepository.save(any(Mechanic.class))).thenReturn(updated);

        Mechanic result = mechanicService.changeStatus(1L, false);

        assertEquals(updated, result);
        verify(mechanicRepository).findById(1L);
        verify(mechanicRepository).save(any(Mechanic.class));
    }

    @Test
    void changeStatus_whenMechanicMissing_throwsMechanicNotFoundException() {
        when(mechanicRepository.findById(anyLong())).thenReturn(Optional.empty());

        assertThrows(MechanicNotFoundException.class, () -> mechanicService.changeStatus(99L, false));
        verify(mechanicRepository).findById(99L);
    }

    @Test
    void create_whenNameIsBlank_propagatesDomainValidation() {
        assertThrows(IllegalArgumentException.class, () -> mechanicService.create("   "));
    }
}
