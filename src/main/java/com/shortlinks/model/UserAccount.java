package com.shortlinks.model;

import java.time.Instant;
import java.util.UUID;

public class UserAccount {
    private UUID id;
    private Instant createdAt;

    public UserAccount() {
    }

    public UserAccount(UUID id, Instant createdAt) {
        this.id = id;
        this.createdAt = createdAt;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
