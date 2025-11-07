package com.shortlinks.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.time.Instant;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
public class ShortLink {
    private String code;
    private UUID ownerId;
    private String originalUrl;
    private int maxVisits;
    private int visitCount;
    private Instant createdAt;
    private Instant expiresAt;

    public ShortLink() {
    }

    public ShortLink(String code,
                     UUID ownerId,
                     String originalUrl,
                     int maxVisits,
                     int visitCount,
                     Instant createdAt,
                     Instant expiresAt) {
        this.code = code;
        this.ownerId = ownerId;
        this.originalUrl = originalUrl;
        this.maxVisits = maxVisits;
        this.visitCount = visitCount;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public String getOriginalUrl() {
        return originalUrl;
    }

    public void setOriginalUrl(String originalUrl) {
        this.originalUrl = originalUrl;
    }

    public int getMaxVisits() {
        return maxVisits;
    }

    public void setMaxVisits(int maxVisits) {
        this.maxVisits = maxVisits;
    }

    public int getVisitCount() {
        return visitCount;
    }

    public void setVisitCount(int visitCount) {
        this.visitCount = visitCount;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return now.isAfter(expiresAt) || now.equals(expiresAt);
    }

    public boolean isVisitLimitReached() {
        return visitCount >= maxVisits;
    }
}
