package org.example.murderhelp;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@EnableJpaAuditing
@SpringBootApplication
public class MurderHelpApplication {
    public static void main(String[] args) {
        SpringApplication.run(MurderHelpApplication.class, args);
    }
}
