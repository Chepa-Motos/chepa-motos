package com.chepamotos.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "vehicle")
public class Vehicle {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "vehicle_id")
    private Long id;

    @Column(name = "plate", nullable = false, unique = true, length = 20)
    private String plate;

    @Column(name = "model", nullable = false, length = 100)
    private String model;

}