package com.chepamotos.infrastructure.application;

import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.usecase.mechanic.ChangeMechanicStatusUseCase;
import com.chepamotos.domain.usecase.mechanic.CreateMechanicUseCase;
import com.chepamotos.domain.usecase.mechanic.GetMechanicByIdUseCase;
import com.chepamotos.domain.usecase.mechanic.ListMechanicsUseCase;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MechanicApplicationService {

    // This class is an outer-layer facade: Spring + transactions live here.
    // Domain use cases remain framework-free under domain/usecase.

    private final ListMechanicsUseCase listMechanicsUseCase;
    private final GetMechanicByIdUseCase getMechanicByIdUseCase;
    private final CreateMechanicUseCase createMechanicUseCase;
    private final ChangeMechanicStatusUseCase changeMechanicStatusUseCase;

    public MechanicApplicationService(
            ListMechanicsUseCase listMechanicsUseCase,
            GetMechanicByIdUseCase getMechanicByIdUseCase,
            CreateMechanicUseCase createMechanicUseCase,
            ChangeMechanicStatusUseCase changeMechanicStatusUseCase
    ) {
        this.listMechanicsUseCase = listMechanicsUseCase;
        this.getMechanicByIdUseCase = getMechanicByIdUseCase;
        this.createMechanicUseCase = createMechanicUseCase;
        this.changeMechanicStatusUseCase = changeMechanicStatusUseCase;
    }

    // Queries
    @Transactional(readOnly = true)
    public List<Mechanic> listByActive(boolean active) {
        return listMechanicsUseCase.execute(active);
    }

    @Transactional(readOnly = true)
    public Mechanic getById(Long mechanicId) {
        return getMechanicByIdUseCase.execute(mechanicId);
    }

    // Commands
    @Transactional
    public Mechanic create(String name) {
        return createMechanicUseCase.execute(name);
    }

    @Transactional
    public Mechanic changeStatus(Long mechanicId, boolean active) {
        return changeMechanicStatusUseCase.execute(mechanicId, active);
    }
}
