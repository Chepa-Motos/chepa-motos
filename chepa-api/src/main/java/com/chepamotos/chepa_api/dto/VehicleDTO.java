package com.chepamotos.chepa_api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {
    private String plate;
    private String brand;
    private String model;
}