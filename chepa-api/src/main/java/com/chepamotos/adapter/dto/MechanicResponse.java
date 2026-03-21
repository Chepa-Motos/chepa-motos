package com.chepamotos.adapter.dto;

import com.chepamotos.domain.model.Mechanic;
import com.fasterxml.jackson.annotation.JsonProperty;

public record MechanicResponse(
        Long id,
        String name,
        @JsonProperty("is_active") boolean isActive
) {

    public static MechanicResponse fromDomain(Mechanic mechanic) {
        return new MechanicResponse(mechanic.id(), mechanic.name(), mechanic.active());
    }
}
