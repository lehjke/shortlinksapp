package com.shortlinks.service;

import com.shortlinks.model.ShortLink;
import com.shortlinks.model.UserAccount;
import com.shortlinks.repository.ShortLinkRepository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public class UserService {
    private final ShortLinkRepository repository;

    public UserService(ShortLinkRepository repository) {
        this.repository = repository;
    }

    public UserAccount registerNewUser() {
        UserAccount account = new UserAccount(UUID.randomUUID(), Instant.now());
        return repository.saveUser(account);
    }

    public Optional<UserAccount> findUser(UUID userId) {
        return repository.findUser(userId);
    }

    public List<ShortLink> getUserLinks(UUID userId) {
        return repository.findByOwner(userId);
    }
}
