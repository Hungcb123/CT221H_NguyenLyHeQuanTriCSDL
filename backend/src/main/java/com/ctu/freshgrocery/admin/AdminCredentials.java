package com.ctu.freshgrocery.admin;

import org.springframework.core.env.Environment;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import java.nio.file.*;
import java.nio.file.attribute.PosixFilePermissions;
import java.security.SecureRandom;
import java.util.*;

@Component
public class AdminCredentials {
    private final String username;
    private final String passwordHash;

    public AdminCredentials(Environment environment) throws Exception {
        var encoder = new BCryptPasswordEncoder(12);
        String configured = environment.getProperty("ADMIN_PASSWORD", "");
        if (!configured.isBlank()) {
            if (configured.length() < 12) throw new IllegalStateException("ADMIN_PASSWORD must contain at least 12 characters");
            username = environment.getProperty("ADMIN_USERNAME", "admin");
            passwordHash = encoder.encode(configured);
            return;
        }
        Path directory = Path.of(environment.getProperty("ADMIN_CREDENTIALS_DIR", ".local"));
        Files.createDirectories(directory);
        Path file = directory.resolve("admin.properties");
        Properties properties = new Properties();
        if (Files.exists(file)) {
            try (var input = Files.newInputStream(file)) { properties.load(input); }
        } else {
            byte[] bytes = new byte[24];
            new SecureRandom().nextBytes(bytes);
            String password = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
            properties.setProperty("username", environment.getProperty("ADMIN_USERNAME", "admin"));
            properties.setProperty("passwordHash", encoder.encode(password));
            try (var output = Files.newOutputStream(file, StandardOpenOption.CREATE_NEW)) {
                properties.store(output, "Local admin account; do not commit");
            }
            Path initial = directory.resolve("admin-initial-password.txt");
            Files.writeString(initial, "Username: " + properties.getProperty("username") + "\nPassword: " + password + "\n",
                    StandardOpenOption.CREATE_NEW);
            restrict(file); restrict(initial);
        }
        username = Objects.requireNonNull(properties.getProperty("username"));
        passwordHash = Objects.requireNonNull(properties.getProperty("passwordHash"));
    }

    private void restrict(Path file) throws Exception {
        if (Files.getFileStore(file).supportsFileAttributeView("posix"))
            Files.setPosixFilePermissions(file, PosixFilePermissions.fromString("rw-------"));
    }

    public String username() { return username; }
    public String passwordHash() { return passwordHash; }
}
