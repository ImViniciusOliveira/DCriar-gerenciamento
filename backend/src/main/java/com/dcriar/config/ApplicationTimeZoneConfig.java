package com.dcriar.config;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;

import java.time.ZoneId;
import java.util.TimeZone;

@Configuration
public class ApplicationTimeZoneConfig {

    @Value("${app.time-zone:America/Sao_Paulo}")
    private String applicationTimeZone;

    @PostConstruct
    void applyDefaultTimeZone() {
        TimeZone.setDefault(TimeZone.getTimeZone(ZoneId.of(applicationTimeZone)));
    }
}
