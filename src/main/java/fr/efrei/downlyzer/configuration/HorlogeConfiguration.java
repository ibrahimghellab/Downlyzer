package fr.efrei.downlyzer.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class HorlogeConfiguration {

    @Bean
    public Clock horloge() {
        return Clock.systemDefaultZone();
    }
}