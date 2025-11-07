package com.shortlinks.service;

import com.shortlinks.model.ShortLink;
import com.shortlinks.notification.NotificationService;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;

public class ShortLinkCleaner implements AutoCloseable {
    private final ScheduledExecutorService executorService;
    private final ShortLinkService shortLinkService;
    private final NotificationService notificationService;

    public ShortLinkCleaner(ShortLinkService shortLinkService,
                            NotificationService notificationService,
                            Duration interval) {
        this.shortLinkService = shortLinkService;
        this.notificationService = notificationService;
        this.executorService = Executors.newSingleThreadScheduledExecutor(new CleanerThreadFactory());
        long seconds = Math.max(5, interval.getSeconds());
        executorService.scheduleAtFixedRate(this::cleanup, seconds, seconds, TimeUnit.SECONDS);
    }

    private void cleanup() {
        try {
            List<ShortLink> removed = shortLinkService.removeExpired();
            if (!removed.isEmpty()) {
                notificationService.info("Удалено просроченных ссылок: " + removed.size());
            }
        } catch (Exception e) {
            notificationService.error("Ошибка фоновой очистки: " + e.getMessage());
        }
    }

    @Override
    public void close() {
        executorService.shutdownNow();
    }

    private static class CleanerThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable r) {
            Thread thread = new Thread(r, "short-link-cleaner");
            thread.setDaemon(true);
            return thread;
        }
    }
}
