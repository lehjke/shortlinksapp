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
        notificationService.info("Short link console ready.");
        boolean exit = false;
        while (!exit) {
            printMainMenu();
            String choice = scanner.nextLine().trim();
            switch (choice) {
                case "1" -> registerUser();
                case "2" -> login();
                case "3" -> openShortLinkFlow();
                case "0" -> exit = true;
                default -> notificationService.warning("Unknown command.");
            }
        }
        notificationService.info("Good bye!");
    }

    private void printMainMenu() {
        System.out.println();
        System.out.println("==== Short Links ====");
        System.out.println("1. Register new user");
        System.out.println("2. Login with UUID");
        System.out.println("3. Open short link");
        System.out.println("0. Exit");
        System.out.print("Select option: ");
    }

    private void registerUser() {
        UserAccount account = userService.registerNewUser();
        System.out.println("Your UUID: " + account.getId());
        System.out.println("Store it safely – it is the only way to manage your links.");
    }

    private void login() {
        System.out.print("Enter your UUID: ");
        String input = scanner.nextLine().trim();
        try {
            UUID userId = UUID.fromString(input);
            Optional<UserAccount> optional = userService.findUser(userId);
            if (optional.isEmpty()) {
                notificationService.warning("User not found.");
                return;
            }
            handleUserSession(optional.get());
        } catch (IllegalArgumentException e) {
            notificationService.error("Invalid UUID format.");
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
                case "0" -> exit = true;
                default -> notificationService.warning("Unknown command.");
            }
        }
    }

    private void printUserMenu(UserAccount user) {
        System.out.println();
        System.out.println("==== User " + user.getId() + " ====");
        System.out.println("1. Create short link");
        System.out.println("2. List my links");
        System.out.println("3. Delete link");
        System.out.println("4. Open short link");
        System.out.println("0. Logout");
        System.out.print("Select option: ");
    }

    private void createShortLink(UserAccount user) {
        try {
            System.out.print("Original URL: ");
            String url = scanner.nextLine();
            System.out.print("Max visits: ");
            int maxVisits = Integer.parseInt(scanner.nextLine().trim());
            ShortLink link = shortLinkService.createShortLink(user.getId(), url, maxVisits);
            System.out.println("Short link: " + shortLinkService.toFullShortUrl(link.getCode()));
            System.out.println("Expires at: " + link.getExpiresAt());
        } catch (Exception e) {
            notificationService.error("Unable to create link: " + e.getMessage());
        }
    }

    private void listMyLinks(UserAccount user) {
        List<ShortLink> links = shortLinkService.listLinks(user.getId());
        if (links.isEmpty()) {
            System.out.println("No links yet.");
            return;
        }
        Instant now = Instant.now();
        System.out.println("Code | URL | Visits (used/limit) | TTL");
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
        System.out.print("Enter short link or code to delete: ");
        String code = scanner.nextLine();
        try {
            boolean deleted = shortLinkService.deleteShortLink(user.getId(), code);
            if (deleted) {
                notificationService.info("Link removed.");
            } else {
                notificationService.warning("Link not found.");
            }
        } catch (Exception e) {
            notificationService.error("Unable to delete link: " + e.getMessage());
        }
    }

    private void openShortLinkFlow() {
        System.out.print("Enter short link to open: ");
        String code = scanner.nextLine();
        try {
            shortLinkService.visit(code);
        } catch (Exception e) {
            notificationService.error("Unable to open link: " + e.getMessage());
        }
    }

    private String formatTtl(Instant now, ShortLink link) {
        if (link.isExpired(now)) {
            return "expired";
        }
        Duration duration = Duration.between(now, link.getExpiresAt());
        long hours = duration.toHours();
        long minutes = duration.minusHours(hours).toMinutes();
        return hours + "h " + minutes + "m";
    }
}
