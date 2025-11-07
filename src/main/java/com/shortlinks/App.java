package com.shortlinks;

import com.shortlinks.config.AppConfig;
import com.shortlinks.console.ConsoleRunner;
import com.shortlinks.notification.ConsoleNotificationService;
import com.shortlinks.notification.NotificationService;
import com.shortlinks.repository.FileShortLinkRepository;
import com.shortlinks.repository.ShortLinkRepository;
import com.shortlinks.service.ShortLinkCleaner;
import com.shortlinks.service.ShortLinkService;
import com.shortlinks.service.UserService;
import com.shortlinks.util.ShortLinkGenerator;

public class App {
    public static void main(String[] args) {
        AppConfig config = new AppConfig();
        NotificationService notificationService = new ConsoleNotificationService();
        ShortLinkRepository repository = new FileShortLinkRepository(config.getStorageFile());
        ShortLinkService shortLinkService = new ShortLinkService(
                config,
                repository,
                new ShortLinkGenerator(),
                notificationService
        );
        UserService userService = new UserService(repository);

        try (ShortLinkCleaner ignored = new ShortLinkCleaner(
                shortLinkService,
                notificationService,
                config.getCleanupInterval())) {

            ConsoleRunner runner = new ConsoleRunner(userService, shortLinkService, notificationService);
            runner.run();
        }
    }
}
