package com.shortlinks.service;

import com.shortlinks.config.AppConfig;
import com.shortlinks.model.ShortLink;
import com.shortlinks.model.UserAccount;
import com.shortlinks.notification.NotificationService;
import com.shortlinks.repository.FileShortLinkRepository;
import com.shortlinks.repository.ShortLinkRepository;
import com.shortlinks.util.ShortLinkGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.*;

class ShortLinkServiceTest {
    private Path tempFile;
    private ShortLinkRepository repository;
    private ShortLinkService shortLinkService;
    private UserService userService;

    @BeforeEach
    void setUp() throws IOException {
        tempFile = Files.createTempFile("shortlinks-test", ".json");
        Files.deleteIfExists(tempFile);
        System.setProperty("app.storage-file", tempFile.toString());
        System.setProperty("app.open-browser", "false");
        System.setProperty("app.default-ttl-hours", "24");
        System.setProperty("app.cleanup-interval-seconds", "60");
        AppConfig config = new AppConfig();
        repository = new FileShortLinkRepository(config.getStorageFile());
        shortLinkService = new ShortLinkService(config, repository, new ShortLinkGenerator(), new SilentNotification());
        userService = new UserService(repository);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.deleteIfExists(tempFile);
        System.clearProperty("app.storage-file");
        System.clearProperty("app.open-browser");
        System.clearProperty("app.default-ttl-hours");
        System.clearProperty("app.cleanup-interval-seconds");
    }

    @Test
    void shouldCreateUniqueCodesForDifferentUsers() {
        UserAccount userA = userService.registerNewUser();
        UserAccount userB = userService.registerNewUser();

        ShortLink linkA = shortLinkService.createShortLink(userA.getId(), "https://example.com/article", 5);
        ShortLink linkB = shortLinkService.createShortLink(userB.getId(), "https://example.com/article", 5);

        assertNotEquals(linkA.getCode(), linkB.getCode());
    }

    @Test
    void shouldBlockWhenLimitReached() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 1);

        ShortLinkService.VisitResult firstVisit = shortLinkService.visit(link.getCode());
        assertEquals(ShortLinkService.VisitStatus.SUCCESS, firstVisit.getStatus());

        ShortLinkService.VisitResult secondVisit = shortLinkService.visit(link.getCode());
        assertEquals(ShortLinkService.VisitStatus.LIMIT_REACHED, secondVisit.getStatus());
    }

    @Test
    void visitShouldExpireLink() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 3);
        link.setExpiresAt(Instant.now().minusSeconds(5));
        repository.save(link);

        ShortLinkService.VisitResult result = shortLinkService.visit(link.getCode());

        assertEquals(ShortLinkService.VisitStatus.EXPIRED, result.getStatus());
        assertTrue(repository.findByCode(link.getCode()).isEmpty());
    }

    @Test
    void cleanupRemovesExpiredLinks() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 3);
        link.setExpiresAt(Instant.now().minusSeconds(5));
        repository.save(link);

        assertEquals(1, shortLinkService.removeExpired().size());
        assertTrue(repository.findAll().isEmpty());
    }

    @Test
    void updateShouldChangeLimitAndRefreshTtl() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 5);
        Instant originalExpiry = link.getExpiresAt();

        ShortLink updatedLimit = shortLinkService.updateShortLink(user.getId(), link.getCode(), 10, false);
        assertEquals(10, updatedLimit.getMaxVisits());

        ShortLink refreshed = shortLinkService.updateShortLink(user.getId(), link.getCode(), null, true);
        assertTrue(refreshed.getExpiresAt().isAfter(originalExpiry));
    }

    private static class SilentNotification implements NotificationService {
        @Override
        public void info(String message) {
        }

        @Override
        public void warning(String message) {
        }

        @Override
        public void error(String message) {
        }
    }
}
