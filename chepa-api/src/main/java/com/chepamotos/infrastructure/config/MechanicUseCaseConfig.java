package com.chepamotos.infrastructure.config;

import com.chepamotos.domain.port.MechanicRepository;
import com.chepamotos.domain.usecase.mechanic.ChangeMechanicStatusUseCase;
import com.chepamotos.domain.usecase.mechanic.CreateMechanicUseCase;
import com.chepamotos.domain.usecase.mechanic.GetMechanicByIdUseCase;
import com.chepamotos.domain.usecase.mechanic.ListMechanicsUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MechanicUseCaseConfig {

    // Query use case beans
    @Bean
    public ListMechanicsUseCase listMechanicsUseCase(MechanicRepository mechanicRepository) {
        return new ListMechanicsUseCase(mechanicRepository);
    }

    @Bean
    public GetMechanicByIdUseCase getMechanicByIdUseCase(MechanicRepository mechanicRepository) {
        return new GetMechanicByIdUseCase(mechanicRepository);
    }

    // Command use case beans
    @Bean
    public CreateMechanicUseCase createMechanicUseCase(MechanicRepository mechanicRepository) {
        return new CreateMechanicUseCase(mechanicRepository);
    }

    @Bean
    public ChangeMechanicStatusUseCase changeMechanicStatusUseCase(MechanicRepository mechanicRepository) {
        return new ChangeMechanicStatusUseCase(mechanicRepository);
    }
}
