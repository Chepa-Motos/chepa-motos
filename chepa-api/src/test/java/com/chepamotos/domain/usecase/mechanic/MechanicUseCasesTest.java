package com.chepamotos.domain.usecase.mechanic;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MechanicUseCasesTest {

    @Mock
    private MechanicRepository mechanicRepository;

    @Test
    void listUseCase_executeAll_returnsRepositoryResults() {
        List<Mechanic> expected = List.of(
                Mechanic.restore(1L, "Jose", true),
                Mechanic.restore(2L, "Carlos", false)
        );
        when(mechanicRepository.findAll()).thenReturn(expected);

        ListMechanicsUseCase useCase = new ListMechanicsUseCase(mechanicRepository);
        List<Mechanic> result = useCase.executeAll();

        assertEquals(expected, result);
        verify(mechanicRepository).findAll();
    }

    @Test
    void listUseCase_executeActive_returnsRepositoryFilteredResults() {
        List<Mechanic> expected = List.of(Mechanic.restore(1L, "Jose", true));
        when(mechanicRepository.findAllByActive(true)).thenReturn(expected);

        ListMechanicsUseCase useCase = new ListMechanicsUseCase(mechanicRepository);
        List<Mechanic> result = useCase.execute(true);

        assertEquals(expected, result);
        verify(mechanicRepository).findAllByActive(true);
    }

    @Test
    void getByIdUseCase_whenExists_returnsMechanic() {
        Mechanic expected = Mechanic.restore(1L, "Jose", true);
        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(expected));

        GetMechanicByIdUseCase useCase = new GetMechanicByIdUseCase(mechanicRepository);
        Mechanic result = useCase.execute(1L);

        assertEquals(expected, result);
        verify(mechanicRepository).findById(1L);
    }

    @Test
    void getByIdUseCase_whenMissing_throwsMechanicNotFoundException() {
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        GetMechanicByIdUseCase useCase = new GetMechanicByIdUseCase(mechanicRepository);
        assertThrows(MechanicNotFoundException.class, () -> useCase.execute(99L));

        verify(mechanicRepository).findById(99L);
    }

    @Test
    void createUseCase_buildsDomainMechanicAndSaves() {
        Mechanic saved = Mechanic.restore(5L, "Carlos", true);
        when(mechanicRepository.save(any(Mechanic.class))).thenReturn(saved);

        CreateMechanicUseCase useCase = new CreateMechanicUseCase(mechanicRepository);
        Mechanic result = useCase.execute("  Carlos  ");

        assertEquals(saved, result);
        verify(mechanicRepository).save(any(Mechanic.class));
    }

    @Test
    void createUseCase_whenNameIsBlank_propagatesDomainValidation() {
        CreateMechanicUseCase useCase = new CreateMechanicUseCase(mechanicRepository);

        assertThrows(IllegalArgumentException.class, () -> useCase.execute("   "));
    }

    @Test
    void changeStatusUseCase_whenMechanicExists_updatesAndSaves() {
        Mechanic existing = Mechanic.restore(1L, "Jose", true);
        Mechanic updated = Mechanic.restore(1L, "Jose", false);

        when(mechanicRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(mechanicRepository.save(any(Mechanic.class))).thenReturn(updated);

        ChangeMechanicStatusUseCase useCase = new ChangeMechanicStatusUseCase(mechanicRepository);
        Mechanic result = useCase.execute(1L, false);

        assertEquals(updated, result);
        verify(mechanicRepository).findById(1L);
        verify(mechanicRepository).save(any(Mechanic.class));
    }

    @Test
    void changeStatusUseCase_whenMechanicMissing_throwsMechanicNotFoundException() {
        when(mechanicRepository.findById(99L)).thenReturn(Optional.empty());

        ChangeMechanicStatusUseCase useCase = new ChangeMechanicStatusUseCase(mechanicRepository);
        assertThrows(MechanicNotFoundException.class, () -> useCase.execute(99L, false));

        verify(mechanicRepository).findById(99L);
    }
}
