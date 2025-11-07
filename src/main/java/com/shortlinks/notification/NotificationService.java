package com.shortlinks.notification;

public interface NotificationService {
    void info(String message);

    void warning(String message);

    void error(String message);
}
