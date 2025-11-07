package com.shortlinks.repository;

import com.shortlinks.model.ShortLink;
import com.shortlinks.model.UserAccount;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository {
    Optional<ShortLink> findByCode(String code);

    List<ShortLink> findByOwner(UUID ownerId);

    List<ShortLink> findAll();

    ShortLink save(ShortLink shortLink);

    boolean delete(String code);

    List<ShortLink> deleteExpired(Instant now);

    UserAccount saveUser(UserAccount userAccount);

    Optional<UserAccount> findUser(UUID userId);

    Collection<UserAccount> findAllUsers();
}
