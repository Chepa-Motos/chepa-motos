package com.chepamotos.chepa_api.modelo;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vehicle")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vehicle {
    
    @Id
    @Column(name = "plate", length = 6)
    private String plate;
    
    @Column(name = "brand", nullable = false, length = 50)
    private String brand;
    
    @Column(name = "model", nullable = false, length = 50)
    private String model;
}