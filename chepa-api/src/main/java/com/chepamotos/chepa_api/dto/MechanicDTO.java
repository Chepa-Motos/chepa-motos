package com.chepamotos.chepa_api.dto;

import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MechanicDTO {
    private Long mechanicId;
    private String name;
    private Boolean isActive;
}