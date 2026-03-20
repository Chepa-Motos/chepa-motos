package com.chepamotos.domain.port;

import com.chepamotos.domain.model.Mechanic;

import java.util.List;
import java.util.Optional;

public interface MechanicRepository {

    List<Mechanic> findAll();

    List<Mechanic> findAllByActive(boolean active);

    Optional<Mechanic> findById(Long id);

    Mechanic save(Mechanic mechanic);
}
