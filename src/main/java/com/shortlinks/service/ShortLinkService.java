package com.shortlinks.service;

import com.shortlinks.config.AppConfig;
import com.shortlinks.model.ShortLink;
import com.shortlinks.notification.NotificationService;
import com.shortlinks.repository.ShortLinkRepository;
import com.shortlinks.util.ShortLinkGenerator;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class ShortLinkService {
    private final ShortLinkRepository repository;
    private final ShortLinkGenerator generator;
    private final Duration ttl;
    private final NotificationService notificationService;
    private final boolean openBrowser;
    private final String shortLinkDomain;
    private final int shortCodeLength;

    public ShortLinkService(AppConfig config,
                            ShortLinkRepository repository,
                            ShortLinkGenerator generator,
                            NotificationService notificationService) {
        this.repository = repository;
        this.generator = generator;
        this.notificationService = notificationService;
        this.ttl = config.getDefaultTtl();
        this.openBrowser = config.isOpenBrowserEnabled();
        this.shortLinkDomain = normalizeDomain(config.getShortLinkDomain());
        this.shortCodeLength = config.getShortCodeLength();
    }

    public ShortLink createShortLink(UUID ownerId, String originalUrl, int maxVisits) {
        repository.findUser(ownerId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        validateUrl(originalUrl);
        if (maxVisits <= 0) {
            throw new IllegalArgumentException("Visit limit must be positive");
        }
        Instant now = Instant.now();
        Instant expiration = now.plus(ttl);
        String code = generateUniqueCode();
        ShortLink shortLink = new ShortLink(code, ownerId, originalUrl.trim(), maxVisits, 0, now, expiration);
        return repository.save(shortLink);
    }

    public VisitResult visit(String rawCodeOrUrl) {
        String code = extractCode(rawCodeOrUrl);
        Optional<ShortLink> optional = repository.findByCode(code);
        if (optional.isEmpty()) {
            notificationService.warning("Short link not found.");
            return new VisitResult(VisitStatus.NOT_FOUND, "Short link not found");
        }
        ShortLink link = optional.get();
        Instant now = Instant.now();
        if (link.isExpired(now)) {
            repository.delete(code);
            notificationService.warning("Link expired and removed.");
            return new VisitResult(VisitStatus.EXPIRED, "Link expired", link);
        }
        if (link.isVisitLimitReached()) {
            notificationService.warning("Visit limit reached for this link.");
            return new VisitResult(VisitStatus.LIMIT_REACHED, "Visit limit reached", link);
        }

        link.setVisitCount(link.getVisitCount() + 1);
        repository.save(link);
        notificationService.info("Redirecting to original URL...");
        openInBrowserIfEnabled(link.getOriginalUrl());
        return new VisitResult(VisitStatus.SUCCESS, "OK", link);
    }

    public boolean deleteShortLink(UUID ownerId, String codeInput) {
        String code = extractCode(codeInput);
        Optional<ShortLink> optional = repository.findByCode(code);
        if (optional.isEmpty()) {
            return false;
        }
        ShortLink link = optional.get();
        if (!link.getOwnerId().equals(ownerId)) {
            throw new IllegalStateException("You can delete only your own links");
        }
        return repository.delete(code);
    }

    public String toFullShortUrl(String code) {
        return shortLinkDomain + code;
    }

    public List<ShortLink> listLinks(UUID ownerId) {
        return repository.findByOwner(ownerId);
    }

    public List<ShortLink> removeExpired() {
        return repository.deleteExpired(Instant.now());
    }

    private String extractCode(String raw) {
        if (raw == null || raw.isBlank()) {
            throw new IllegalArgumentException("Short link cannot be empty");
        }
        String trimmed = raw.trim();
        int queryIndex = trimmed.indexOf('?');
        if (queryIndex >= 0) {
            trimmed = trimmed.substring(0, queryIndex);
        }
        int hashIndex = trimmed.indexOf('#');
        if (hashIndex >= 0) {
            trimmed = trimmed.substring(0, hashIndex);
        }
        int slashIndex = trimmed.lastIndexOf('/');
        if (slashIndex >= 0 && slashIndex < trimmed.length() - 1) {
            return trimmed.substring(slashIndex + 1);
        }
        return trimmed;
    }

    private String generateUniqueCode() {
        String code;
        do {
            code = generator.generate(shortCodeLength);
        } while (repository.findByCode(code).isPresent());
        return code;
    }

    private void validateUrl(String url) {
        try {
            URI uri = new URI(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) {
                throw new IllegalArgumentException("URL must contain scheme and host");
            }
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL format", e);
        }
    }

    private void openInBrowserIfEnabled(String url) {
        if (!openBrowser) {
            return;
        }
        if (!Desktop.isDesktopSupported()) {
            notificationService.warning("Desktop API is not supported. Cannot open browser automatically.");
            return;
        }
        try {
            Desktop.getDesktop().browse(URI.create(url));
        } catch (IOException | IllegalArgumentException e) {
            notificationService.error("Failed to open browser: " + e.getMessage());
        }
    }

    private String normalizeDomain(String domain) {
        String trimmed = domain.trim();
        if (trimmed.isEmpty()) {
            return DEFAULT_DOMAIN_FALLBACK;
        }
        if (!trimmed.endsWith("/")) {
            trimmed = trimmed + "/";
        }
        return trimmed;
    }

    private static final String DEFAULT_DOMAIN_FALLBACK = "https://clck.ru/";

    public enum VisitStatus {
        SUCCESS,
        NOT_FOUND,
        EXPIRED,
        LIMIT_REACHED
    }

    public static class VisitResult {
        private final VisitStatus status;
        private final String message;
        private final ShortLink shortLink;

        public VisitResult(VisitStatus status, String message) {
            this(status, message, null);
        }

        public VisitResult(VisitStatus status, String message, ShortLink shortLink) {
            this.status = status;
            this.message = message;
            this.shortLink = shortLink;
        }

        public VisitStatus getStatus() {
            return status;
        }

        public String getMessage() {
            return message;
        }

        public Optional<ShortLink> getShortLink() {
            return Optional.ofNullable(shortLink);
        }
    }
}
