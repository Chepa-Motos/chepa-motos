package com.chepamotos.chepa_api.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "mechanic")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Mechanic {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "mechanic_id")
    private Long mechanicId;
    
    @Column(name = "name", nullable = false, length = 100)
    private String name;
    
    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;
}