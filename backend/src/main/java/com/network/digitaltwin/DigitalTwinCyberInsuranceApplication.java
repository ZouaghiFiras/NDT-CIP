package com.network.digitaltwin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
@EnableAsync
public class DigitalTwinCyberInsuranceApplication {

    public static void main(String[] args) {
        SpringApplication.run(DigitalTwinCyberInsuranceApplication.class, args);
    }
}
