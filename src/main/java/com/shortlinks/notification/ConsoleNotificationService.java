package com.shortlinks.notification;

public class ConsoleNotificationService implements NotificationService {
    @Override
    public void info(String message) {
        System.out.println("[INFO] " + message);
    }

    @Override
    public void warning(String message) {
        System.out.println("[WARN] " + message);
    }

    @Override
    public void error(String message) {
        System.err.println("[ERROR] " + message);
    }
}
