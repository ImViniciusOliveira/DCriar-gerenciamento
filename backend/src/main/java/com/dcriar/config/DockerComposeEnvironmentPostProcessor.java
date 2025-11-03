package com.dcriar.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class DockerComposeEnvironmentPostProcessor implements EnvironmentPostProcessor {

    private static final int SEARCH_LEVELS = 3;

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String profile = resolveActiveProfile(environment);
        String composeFileName = determineComposeFileName(profile);
        String envFileName = determineEnvFileName(profile);

        Path composePath = findInParents(Paths.get("").toAbsolutePath(), composeFileName);
        Map<String, Object> props = new HashMap<>();

        if (composePath != null) {
            props.put("spring.docker.compose.file", composePath.toString());
            Path envPath = composePath.getParent().resolve(envFileName);
            if (Files.exists(envPath)) {
                // Passa o argumento --env-file para o docker-compose (para os containers)
                props.put("spring.docker.compose.arguments", List.of("--env-file=" + envPath));
                // E também carrega as variáveis do .env.* para o Environment do Spring
                Map<String, String> fileVars = readEnvFile(envPath);
                // Promove todas as variáveis lidas para propriedades do Spring (ambient)
                props.putAll(fileVars);
            }
        }
        else {
            // Se não encontrou arquivo compose, desativa o recurso para evitar falhas
            props.put("spring.docker.compose.enabled", "false");
        }

        environment.getPropertySources().addFirst(new MapPropertySource("dockerComposeAuto", props));
    }

    private Map<String, String> readEnvFile(Path envPath) {
        try {
            List<String> lines = Files.readAllLines(envPath, StandardCharsets.UTF_8);
            return lines.stream()
                    .map(String::trim)
                    .filter(l -> !l.isEmpty())
                    .filter(l -> !l.startsWith("#"))
                    .map(l -> {
                        int idx = l.indexOf('=');
                        if (idx <= 0) {
                            return null;
                        }
                        String key = l.substring(0, idx).trim();
                        String value = l.substring(idx + 1).trim();
                        // remove surrounding quotes if present
                        if ((value.startsWith("\"") && value.endsWith("\"")) || (value.startsWith("'") && value.endsWith("'"))) {
                            value = value.substring(1, value.length() - 1);
                        }
                        return Map.entry(key, value);
                    })
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        catch (IOException ex) {
            return Map.of();
        }
    }

    private String resolveActiveProfile(ConfigurableEnvironment environment) {
        String[] active = environment.getActiveProfiles();
        if (active.length > 0) {
            return active[0];
        }
        String prop = environment.getProperty("spring.profiles.active");
        if (prop != null && !prop.isBlank()) {
            return prop;
        }
        return System.getenv("SPRING_PROFILES_ACTIVE");
    }

    private String determineComposeFileName(String profile) {
        if (profile != null && profile.toLowerCase().contains("prod")) {
            return "docker-compose.prod.yml";
        }
        return "docker-compose.dev.yml";
    }

    private String determineEnvFileName(String profile) {
        if (profile != null && profile.toLowerCase().contains("prod")) {
            return ".env.prod";
        }
        return ".env.dev";
    }

    private Path findInParents(Path start, String fileName) {
        Path current = start;
        for (int i = 0; i <= DockerComposeEnvironmentPostProcessor.SEARCH_LEVELS && current != null; i++) {
            Path candidate = current.resolve(fileName);
            if (Files.exists(candidate)) {
                return candidate;
            }
            current = current.getParent();
        }
        return null;
    }
}
