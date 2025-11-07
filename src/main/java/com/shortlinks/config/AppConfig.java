package com.shortlinks.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Properties;

public class AppConfig {
    private static final String PROPERTIES_FILE = "application.properties";
    private static final String DEFAULT_DOMAIN = "https://clck.ru/";
    private static final int DEFAULT_CODE_LENGTH = 7;
    private static final Duration DEFAULT_TTL = Duration.ofHours(24);
    private static final Duration DEFAULT_CLEANUP_INTERVAL = Duration.ofMinutes(1);
    private static final String DEFAULT_STORAGE = "data/store.json";

    private final Properties properties = new Properties();

    public AppConfig() {
        try (InputStream inputStream = getClass().getClassLoader().getResourceAsStream(PROPERTIES_FILE)) {
            if (inputStream != null) {
                properties.load(inputStream);
            }
        } catch (IOException ignored) {
        }
    }

    public String getShortLinkDomain() {
        return read("app.short-link-domain", DEFAULT_DOMAIN);
    }

    public int getShortCodeLength() {
        return Integer.parseInt(read("app.short-code-length",
                String.valueOf(DEFAULT_CODE_LENGTH)));
    }

    public Duration getDefaultTtl() {
        long hours = Long.parseLong(read("app.default-ttl-hours",
                String.valueOf(DEFAULT_TTL.toHours())));
        return Duration.ofHours(hours);
    }

    public Duration getCleanupInterval() {
        long seconds = Long.parseLong(read("app.cleanup-interval-seconds",
                String.valueOf(DEFAULT_CLEANUP_INTERVAL.toSeconds())));
        return Duration.ofSeconds(seconds);
    }

    public Path getStorageFile() {
        String configured = read("app.storage-file", DEFAULT_STORAGE);
        Path path = Paths.get(configured);
        if (!path.isAbsolute()) {
            return Paths.get(System.getProperty("user.dir")).resolve(path).normalize();
        }
        return path;
    }

    public boolean isOpenBrowserEnabled() {
        return Boolean.parseBoolean(read("app.open-browser", "true"));
    }

    private String read(String key, String defaultValue) {
        return System.getProperty(key, properties.getProperty(key, defaultValue));
    }
}
