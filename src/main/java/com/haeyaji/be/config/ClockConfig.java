package com.haeyaji.be.config;

import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ClockConfig {

    @Bean
    @Primary
    public Clock systemDefaultClock() {
        return Clock.systemDefaultZone();
    }

    @Bean
    public Clock utcClock() {
        return Clock.systemUTC();
    }

}
