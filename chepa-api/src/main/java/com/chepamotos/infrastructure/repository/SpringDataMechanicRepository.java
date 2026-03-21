package com.chepamotos.infrastructure.repository;

import com.chepamotos.infrastructure.entity.Mechanic;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SpringDataMechanicRepository extends JpaRepository<Mechanic, Long> {

    List<Mechanic> findByIsActive(boolean isActive);
}
