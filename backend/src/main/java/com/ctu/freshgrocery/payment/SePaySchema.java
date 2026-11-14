package com.ctu.freshgrocery.payment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class SePaySchema {
    private final JdbcTemplate jdbc;
    @PostConstruct void migrate() throws java.io.IOException {
        try (var stream = new ClassPathResource("db/sepay.sql").getInputStream()) {
            jdbc.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
