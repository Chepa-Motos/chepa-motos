package com.chepamotos.chepa_api.servicio;

import com.chepamotos.chepa_api.dto.MechanicDTO;
import com.chepamotos.chepa_api.modelo.Mechanic;
import com.chepamotos.chepa_api.repositorio.MechanicRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MechanicService {
    
    private final MechanicRepository mechanicRepository;
    
    public List<MechanicDTO> getAllActiveMechanics() {
        return mechanicRepository.findByIsActiveTrue()
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    public MechanicDTO getMechanicById(Long id) {
        Mechanic mechanic = mechanicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + id));
        return convertToDTO(mechanic);
    }
    
    @Transactional
    public MechanicDTO createMechanic(MechanicDTO mechanicDTO) {
        Mechanic mechanic = Mechanic.builder()
                .name(mechanicDTO.getName())
                .isActive(true)
                .build();
        
        Mechanic savedMechanic = mechanicRepository.save(mechanic);
        return convertToDTO(savedMechanic);
    }
    
    @Transactional
    public MechanicDTO updateMechanic(Long id, MechanicDTO mechanicDTO) {
        Mechanic mechanic = mechanicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + id));
        
        mechanic.setName(mechanicDTO.getName());
        mechanic.setIsActive(mechanicDTO.getIsActive());
        
        Mechanic updatedMechanic = mechanicRepository.save(mechanic);
        return convertToDTO(updatedMechanic);
    }
    
    @Transactional
    public void deactivateMechanic(Long id) {
        Mechanic mechanic = mechanicRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Mecánico no encontrado con ID: " + id));
        
        mechanic.setIsActive(false);
        mechanicRepository.save(mechanic);
    }
    
    private MechanicDTO convertToDTO(Mechanic mechanic) {
        return MechanicDTO.builder()
                .mechanicId(mechanic.getMechanicId())
                .name(mechanic.getName())
                .isActive(mechanic.getIsActive())
                .build();
    }
}