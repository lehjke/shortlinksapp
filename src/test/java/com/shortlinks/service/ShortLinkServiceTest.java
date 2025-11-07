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
import java.util.List;

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

    @Test
    void createShortLinkShouldValidateUrl() {
        UserAccount user = userService.registerNewUser();
        assertThrows(IllegalArgumentException.class,
                () -> shortLinkService.createShortLink(user.getId(), "invalid-url", 3));
    }

    @Test
    void createShortLinkShouldRejectNonPositiveLimit() {
        UserAccount user = userService.registerNewUser();
        assertThrows(IllegalArgumentException.class,
                () -> shortLinkService.createShortLink(user.getId(), "https://example.com", 0));
    }

    @Test
    void deleteShouldRemoveOwnLink() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 2);

        assertTrue(shortLinkService.deleteShortLink(user.getId(), link.getCode()));
        assertTrue(repository.findByCode(link.getCode()).isEmpty());
    }

    @Test
    void deleteShouldRejectForeignLink() {
        UserAccount owner = userService.registerNewUser();
        UserAccount other = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(owner.getId(), "https://example.com/resource", 2);

        assertThrows(IllegalStateException.class,
                () -> shortLinkService.deleteShortLink(other.getId(), link.getCode()));
    }

    @Test
    void deleteShouldReturnFalseWhenLinkMissing() {
        UserAccount user = userService.registerNewUser();
        assertFalse(shortLinkService.deleteShortLink(user.getId(), "UNKNOWN"));
    }

    @Test
    void visitShouldAcceptFullUrlWithQueryAndFragment() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 3);

        ShortLinkService.VisitResult result = shortLinkService.visit(
                shortLinkService.toFullShortUrl(link.getCode()) + "?utm=test#section");

        assertEquals(ShortLinkService.VisitStatus.SUCCESS, result.getStatus());
        assertEquals(1, repository.findByCode(link.getCode()).orElseThrow().getVisitCount());
    }

    @Test
    void listLinksShouldExposeOnlyOwnerLinks() {
        UserAccount first = userService.registerNewUser();
        UserAccount second = userService.registerNewUser();
        ShortLink firstLink = shortLinkService.createShortLink(first.getId(), "https://example.com/a", 3);
        shortLinkService.createShortLink(second.getId(), "https://example.com/b", 3);

        List<ShortLink> firstLinks = shortLinkService.listLinks(first.getId());
        assertEquals(1, firstLinks.size());
        assertEquals(firstLink.getCode(), firstLinks.get(0).getCode());
    }

    @Test
    void updateShouldRequireAtLeastOneChange() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 3);

        assertThrows(IllegalArgumentException.class,
                () -> shortLinkService.updateShortLink(user.getId(), link.getCode(), null, false));
    }

    @Test
    void updateShouldRejectLimitLowerThanUsed() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 3);
        shortLinkService.visit(link.getCode());
        shortLinkService.visit(link.getCode());

        assertThrows(IllegalArgumentException.class,
                () -> shortLinkService.updateShortLink(user.getId(), link.getCode(), 1, false));
    }

    @Test
    void cleanupShouldLeaveActiveLinks() {
        UserAccount user = userService.registerNewUser();
        ShortLink link = shortLinkService.createShortLink(user.getId(), "https://example.com/resource", 3);

        assertTrue(shortLinkService.removeExpired().isEmpty());
        assertTrue(repository.findByCode(link.getCode()).isPresent());
    }

    @Test
    void userRegistrationShouldPersistAccount() {
        UserAccount user = userService.registerNewUser();
        assertTrue(repository.findUser(user.getId()).isPresent());
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
