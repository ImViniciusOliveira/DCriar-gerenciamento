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
    private static final String LOCAL_ENV_FILE_NAME = ".env.dev.local";

    @Override
    public void postProcessEnvironment(ConfigurableEnvironment environment, SpringApplication application) {
        String profile = resolveActiveProfile(environment);
        if (!shouldLoadLocalEnv(profile)) {
            return;
        }

        String composeFileName = determineComposeFileName();
        Map<String, Object> props = new HashMap<>();

        Path rootPath = findInParents(Paths.get("").toAbsolutePath(), composeFileName);
        Path baseDir = rootPath != null ? rootPath.getParent() : findBaseDir(Paths.get("").toAbsolutePath());
        Path envPath = baseDir != null ? baseDir.resolve(LOCAL_ENV_FILE_NAME) : null;

        if (envPath != null && Files.exists(envPath)) {
            Map<String, String> fileVars = readEnvFile(envPath);
            props.putAll(fileVars);
        }

        if (shouldAutoConfigureCompose(profile)) {
            if (rootPath != null) {
                props.put("spring.docker.compose.file", rootPath.toString());
                if (envPath != null && Files.exists(envPath)) {
                    props.put("spring.docker.compose.arguments", List.of("--env-file=" + envPath));
                }
            } else {
                props.put("spring.docker.compose.enabled", "false");
            }
        }

        environment.getPropertySources().addFirst(new MapPropertySource("dockerComposeAuto", props));
    }

    private boolean shouldLoadLocalEnv(String profile) {
        if (profile == null || profile.isBlank()) {
            return true;
        }

        String normalized = profile.toLowerCase();
        return normalized.contains("dev") || normalized.contains("local");
    }

    private boolean shouldAutoConfigureCompose(String profile) {
        if (profile == null || profile.isBlank()) {
            return true;
        }

        return profile.toLowerCase().contains("dev");
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

    private String determineComposeFileName() {
        return "docker-compose.dev.yml";
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

    private Path findBaseDir(Path start) {
        Path current = start;
        for (int i = 0; i <= DockerComposeEnvironmentPostProcessor.SEARCH_LEVELS && current != null; i++) {
            if (Files.exists(current.resolve("backend")) || Files.exists(current.resolve(".git"))) {
                return current;
            }
            current = current.getParent();
        }
        return start;
    }
}
