package com.chepamotos.infrastructure.config;

import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.time.Clock;
import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class TimeConfig {

    private static final ZoneId BOGOTA_ZONE = ZoneId.of("America/Bogota");

    @PostConstruct
    void applyDefaultTimezone() {
        TimeZone.setDefault(TimeZone.getTimeZone(BOGOTA_ZONE));
    }

    @Bean
    @Primary
    public Clock systemClock() {
        return Clock.system(BOGOTA_ZONE);
    }
}
