package com.ctu.freshgrocery.payment;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class MoMoSchema {
    private final JdbcTemplate jdbc;
    @PostConstruct void migrate() throws java.io.IOException {
        try (var stream = new ClassPathResource("db/momo.sql").getInputStream()) {
            jdbc.execute(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }
}
