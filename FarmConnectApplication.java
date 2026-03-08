package com.farmconnect;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
@EnableJpaAuditing
public class FarmConnectApplication {
    public static void main(String[] args) {
        SpringApplication.run(FarmConnectApplication.class, args);
    }
}
