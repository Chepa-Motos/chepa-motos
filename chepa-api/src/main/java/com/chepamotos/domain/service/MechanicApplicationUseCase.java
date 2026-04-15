package com.chepamotos.domain.service;

import com.chepamotos.domain.model.Mechanic;

import java.util.List;

public interface MechanicApplicationUseCase {

    List<Mechanic> listByActive(boolean active);

    Mechanic getById(Long mechanicId);

    Mechanic create(String name);

    Mechanic changeStatus(Long mechanicId, boolean active);
}
