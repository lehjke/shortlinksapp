package com.shortlinks.repository;

import com.shortlinks.model.DataStore;
import com.shortlinks.model.ShortLink;
import com.shortlinks.model.UserAccount;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class FileShortLinkRepository implements ShortLinkRepository {
    private final Path storagePath;
    private final ObjectMapper objectMapper;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    private DataStore dataStore;

    public FileShortLinkRepository(Path storagePath) {
        this.storagePath = storagePath;
        this.objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        load();
    }

    @Override
    public Optional<ShortLink> findByCode(String code) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(dataStore.getShortLinks().get(code));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<ShortLink> findByOwner(UUID ownerId) {
        lock.readLock().lock();
        try {
            return dataStore.getShortLinks()
                    .values()
                    .stream()
                    .filter(link -> link.getOwnerId().equals(ownerId))
                    .map(FileShortLinkRepository::cloneLink)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public List<ShortLink> findAll() {
        lock.readLock().lock();
        try {
            return dataStore.getShortLinks()
                    .values()
                    .stream()
                    .map(FileShortLinkRepository::cloneLink)
                    .collect(Collectors.toList());
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public ShortLink save(ShortLink shortLink) {
        lock.writeLock().lock();
        try {
            dataStore.getShortLinks().put(shortLink.getCode(), shortLink);
            persist();
            return shortLink;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public boolean delete(String code) {
        lock.writeLock().lock();
        try {
            ShortLink removed = dataStore.getShortLinks().remove(code);
            if (removed != null) {
                persist();
                return true;
            }
            return false;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public List<ShortLink> deleteExpired(Instant now) {
        lock.writeLock().lock();
        try {
            Map<String, ShortLink> links = dataStore.getShortLinks();
            List<String> expiredCodes = new ArrayList<>();
            for (Map.Entry<String, ShortLink> entry : links.entrySet()) {
                if (entry.getValue().isExpired(now)) {
                    expiredCodes.add(entry.getKey());
                }
            }
            List<ShortLink> removed = expiredCodes.stream()
                    .map(links::remove)
                    .collect(Collectors.toList());
            if (!removed.isEmpty()) {
                persist();
            }
            return removed;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public UserAccount saveUser(UserAccount userAccount) {
        lock.writeLock().lock();
        try {
            dataStore.getUsers().put(userAccount.getId(), userAccount);
            persist();
            return userAccount;
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    public Optional<UserAccount> findUser(UUID userId) {
        lock.readLock().lock();
        try {
            return Optional.ofNullable(dataStore.getUsers().get(userId));
        } finally {
            lock.readLock().unlock();
        }
    }

    @Override
    public Collection<UserAccount> findAllUsers() {
        lock.readLock().lock();
        try {
            return new ArrayList<>(dataStore.getUsers().values());
        } finally {
            lock.readLock().unlock();
        }
    }

    private void load() {
        try {
            if (Files.exists(storagePath)) {
                dataStore = objectMapper.readValue(storagePath.toFile(), DataStore.class);
            } else {
                Path parent = storagePath.getParent();
                if (parent != null) {
                    Files.createDirectories(parent);
                }
                dataStore = new DataStore();
                persist();
            }
        } catch (IOException e) {
            throw new IllegalStateException("Unable to load storage file", e);
        }
    }

    private void persist() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(storagePath.toFile(), dataStore);
        } catch (IOException e) {
            throw new IllegalStateException("Unable to persist storage file", e);
        }
    }

    private static ShortLink cloneLink(ShortLink link) {
        return new ShortLink(
                link.getCode(),
                link.getOwnerId(),
                link.getOriginalUrl(),
                link.getMaxVisits(),
                link.getVisitCount(),
                link.getCreatedAt(),
                link.getExpiresAt()
        );
    }
}
