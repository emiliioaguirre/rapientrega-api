package com.universidad.rapientrega;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableScheduling;
import java.time.Clock;

@SpringBootApplication
@EnableScheduling
public class RapientregaApplication {
    public static void main(String[] args) {
        SpringApplication.run(RapientregaApplication.class, args);
    }

    @Bean
    public Clock reloj() {
        return Clock.systemDefaultZone();
    }
}
