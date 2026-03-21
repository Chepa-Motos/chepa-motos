package com.chepamotos.domain.service;

import com.chepamotos.domain.exception.MechanicNotFoundException;
import com.chepamotos.domain.model.Mechanic;
import com.chepamotos.domain.port.MechanicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class MechanicService {

    private final MechanicRepository mechanicRepository;

    public MechanicService(MechanicRepository mechanicRepository) {
        this.mechanicRepository = mechanicRepository;
    }

    @Transactional(readOnly = true)
    public List<Mechanic> listAll() {
        return mechanicRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<Mechanic> listByActive(boolean active) {
        return mechanicRepository.findAllByActive(active);
    }

    @Transactional(readOnly = true)
    public Mechanic getById(Long mechanicId) {
        return mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new MechanicNotFoundException(mechanicId));
    }

    @Transactional
    public Mechanic create(String name) {
        Mechanic newMechanic = Mechanic.createNew(name);
        return mechanicRepository.save(newMechanic);
    }

    @Transactional
    public Mechanic changeStatus(Long mechanicId, boolean active) {
        Mechanic existingMechanic = mechanicRepository.findById(mechanicId)
                .orElseThrow(() -> new MechanicNotFoundException(mechanicId));

        Mechanic updatedMechanic = existingMechanic.withStatus(active);
        return mechanicRepository.save(updatedMechanic);
    }
}
