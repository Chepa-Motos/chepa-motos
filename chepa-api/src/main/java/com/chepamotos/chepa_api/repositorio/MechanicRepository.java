package com.chepamotos.chepa_api.repositorio;

import com.chepamotos.chepa_api.modelo.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MechanicRepository extends JpaRepository<Mechanic, Long> {
    
    List<Mechanic> findByIsActiveTrue();
    
    List<Mechanic> findByNameContainingIgnoreCase(String name);
}