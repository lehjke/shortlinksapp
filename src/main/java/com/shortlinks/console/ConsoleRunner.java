package com.shortlinks.console;

import com.shortlinks.model.ShortLink;
import com.shortlinks.model.UserAccount;
import com.shortlinks.notification.NotificationService;
import com.shortlinks.service.ShortLinkService;
import com.shortlinks.service.UserService;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;
import java.util.UUID;

public class ConsoleRunner {
    private final UserService userService;
    private final ShortLinkService shortLinkService;
    private final NotificationService notificationService;
    private final Scanner scanner = new Scanner(System.in);

    public ConsoleRunner(UserService userService,
                         ShortLinkService shortLinkService,
                         NotificationService notificationService) {
        this.userService = userService;
        this.shortLinkService = shortLinkService;
        this.notificationService = notificationService;
    }

    public void run() {
        notificationService.info("Консоль сервиса готова к работе.");
        boolean exit = false;
        while (!exit) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> registerUser();
                case "2" -> login();
                case "3" -> openShortLinkFlow();
                case "0" -> exit = true;
                default -> notificationService.warning("Неизвестная команда.");
            }
        }
        notificationService.info("До встречи!");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("==== Сервис коротких ссылок ====");
        System.out.println("1. Зарегистрировать нового пользователя");
        System.out.println("2. Войти по UUID");
        System.out.println("3. Открыть короткую ссылку");
        System.out.println("0. Выход");
        System.out.print("Выберите пункт: ");
    }

    private void registerUser() {
        UserAccount account = userService.registerNewUser();
        System.out.println("Ваш UUID: " + account.getId());
        System.out.println("Сохраните его — только с ним можно управлять вашими ссылками.");
    }

    private void login() {
        System.out.print("Введите ваш UUID: ");
        String input = scanner.nextLine().trim();
        try {
            UUID userId = UUID.fromString(input);
            Optional<UserAccount> optional = userService.findUser(userId);
            if (optional.isEmpty()) {
                notificationService.warning("Пользователь не найден.");
                return;
            }
            handleUserSession(optional.get());
        } catch (IllegalArgumentException e) {
            notificationService.error("Некорректный формат UUID.");
        }
    }

    private void handleUserSession(UserAccount user) {
        boolean exit = false;
        while (!exit) {
            printUserMenu(user);
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> createShortLink(user);
                case "2" -> listMyLinks(user);
                case "3" -> deleteMyLink(user);
                case "4" -> openShortLinkFlow();
                case "5" -> updateLink(user);
                case "0" -> exit = true;
                default -> notificationService.warning("Неизвестная команда.");
            }
        }
    }

    private void printUserMenu(UserAccount user) {
        System.out.println();
        System.out.println("==== Пользователь " + user.getId() + " ====");
        System.out.println("1. Создать короткую ссылку");
        System.out.println("2. Показать мои ссылки");
        System.out.println("3. Удалить ссылку");
        System.out.println("4. Открыть короткую ссылку");
        System.out.println("5. Обновить лимит/TTL ссылки");
        System.out.println("0. Выйти из аккаунта");
        System.out.print("Выберите пункт: ");
    }

    private void createShortLink(UserAccount user) {
        try {
            System.out.print("Исходный URL: ");
            String url = scanner.nextLine();
            System.out.print("Лимит переходов: ");
            int maxVisits = Integer.parseInt(scanner.nextLine().trim());
            ShortLink link = shortLinkService.createShortLink(user.getId(), url, maxVisits);
            System.out.println("Короткая ссылка: " + shortLinkService.toFullShortUrl(link.getCode()));
            System.out.println("Истекает: " + link.getExpiresAt());
        } catch (Exception e) {
            notificationService.error("Не удалось создать ссылку: " + e.getMessage());
        }
    }

    private void listMyLinks(UserAccount user) {
        List<ShortLink> links = shortLinkService.listLinks(user.getId());
        if (links.isEmpty()) {
            System.out.println("Ссылок пока нет.");
            return;
        }
        Instant now = Instant.now();
        System.out.println("Код | URL | Переходы (исп./лимит) | TTL");
        for (ShortLink link : links) {
            String ttl = formatTtl(now, link);
            System.out.printf("%s | %s | %d/%d | %s%n",
                    shortLinkService.toFullShortUrl(link.getCode()),
                    link.getOriginalUrl(),
                    link.getVisitCount(),
                    link.getMaxVisits(),
                    ttl);
        }
    }

    private void deleteMyLink(UserAccount user) {
        System.out.print("Введите короткую ссылку или код для удаления: ");
        String code = scanner.nextLine();
        try {
            boolean deleted = shortLinkService.deleteShortLink(user.getId(), code);
            if (deleted) {
                notificationService.info("Ссылка удалена.");
            } else {
                notificationService.warning("Ссылка не найдена.");
            }
        } catch (Exception e) {
            notificationService.error("Не удалось удалить ссылку: " + e.getMessage());
        }
    }

    private void openShortLinkFlow() {
        System.out.print("Введите короткую ссылку для открытия: ");
        String code = scanner.nextLine();
        try {
            shortLinkService.visit(code);
        } catch (Exception e) {
            notificationService.error("Не удалось открыть ссылку: " + e.getMessage());
        }
    }

    private String formatTtl(Instant now, ShortLink link) {
        if (link.isExpired(now)) {
            return "просрочена";
        }
        Duration duration = Duration.between(now, link.getExpiresAt());
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return hours + " ч " + minutes + " мин";
    }

    private void updateLink(UserAccount user) {
        System.out.print("Введите короткую ссылку или код для обновления: ");
        String code = scanner.nextLine();
        System.out.print("Новый лимит переходов (Enter — оставить текущий): ");
        String limitInput = scanner.nextLine().trim();
        Integer newLimit = null;
        if (!limitInput.isBlank()) {
            try {
                newLimit = Integer.parseInt(limitInput);
            } catch (NumberFormatException e) {
                notificationService.error("Некорректное число.");
                return;
            }
        }
        System.out.print("Продлить срок действия на стандартный TTL? (y/n): ");
        String ttlAnswer = scanner.nextLine().trim().toLowerCase();
        boolean refreshTtl = ttlAnswer.equals("y") || ttlAnswer.equals("yes")
                || ttlAnswer.equals("д") || ttlAnswer.equals("да");
        try {
            ShortLink updated = shortLinkService.updateShortLink(user.getId(), code, newLimit, refreshTtl);
            System.out.println("Ссылка обновлена. Текущий лимит: " + updated.getVisitCount()
                    + "/" + updated.getMaxVisits());
            System.out.println("Активна до: " + updated.getExpiresAt());
        } catch (Exception e) {
            notificationService.error("Не удалось обновить ссылку: " + e.getMessage());
        }
    }
}
