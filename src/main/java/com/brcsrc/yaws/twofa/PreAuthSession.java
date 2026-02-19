package com.brcsrc.yaws.twofa;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "pre_auth_sessions")
public class PreAuthSession {

    @Id
    private String id;
    private Long userId;
    private int attemptCount;
    private int maxAttempts;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private Long lastUsedTotpCounter;

    @Version
    private Long version;

    @Enumerated(EnumType.STRING)
    private PreAuthSessionState state;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public int getAttemptCount() {
        return attemptCount;
    }

    public void setAttemptCount(int attemptCount) {
        this.attemptCount = attemptCount;
    }

    public int getMaxAttempts() {
        return maxAttempts;
    }

    public void setMaxAttempts(int maxAttempts) {
        this.maxAttempts = maxAttempts;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public PreAuthSessionState getState() {
        return state;
    }

    public void setState(PreAuthSessionState state) {
        this.state = state;
    }

    public Long getLastUsedTotpCounter() {
        return lastUsedTotpCounter;
    }

    public void setLastUsedTotpCounter(Long lastUsedTotpCounter) {
        this.lastUsedTotpCounter = lastUsedTotpCounter;
    }

    public Long getVersion() {
        return version;
    }
}
