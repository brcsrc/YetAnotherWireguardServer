package com.brcsrc.yaws.twofa;

import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
public class PreAuthSessionService {

    private final PreAuthSessionRepository preAuthSessionRepository;

    public PreAuthSessionService(PreAuthSessionRepository preAuthSessionRepository) {
        this.preAuthSessionRepository = preAuthSessionRepository;
    }

    public PreAuthSession createSession(Long userId, int maxAttempts, Duration ttl) {
        LocalDateTime now = LocalDateTime.now();
        PreAuthSession session = new PreAuthSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setAttemptCount(0);
        session.setMaxAttempts(maxAttempts);
        session.setCreatedAt(now);
        session.setExpiresAt(now.plus(ttl));
        session.setState(PreAuthSessionState.ACTIVE);
        return preAuthSessionRepository.save(session);
    }

    public PreAuthSession requireActiveSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throwAuthenticationFailed();
        }

        PreAuthSession session = preAuthSessionRepository.findById(sessionId)
                .orElseThrow(this::buildAuthenticationFailedException);

        if (session.getState() != PreAuthSessionState.ACTIVE) {
            throwAuthenticationFailed();
        }

        if (session.getExpiresAt().isBefore(LocalDateTime.now())) {
            expireSession(session);
            throwAuthenticationFailed();
        }

        return session;
    }

    public void incrementAttemptOrLock(PreAuthSession session) {
        session.setAttemptCount(session.getAttemptCount() + 1);
        if (session.getAttemptCount() >= session.getMaxAttempts()) {
            session.setState(PreAuthSessionState.LOCKED);
        }
        preAuthSessionRepository.save(session);
    }

    public void consumeSession(PreAuthSession session) {
        session.setState(PreAuthSessionState.CONSUMED);
        preAuthSessionRepository.save(session);
    }

    public void expireSession(PreAuthSession session) {
        session.setState(PreAuthSessionState.EXPIRED);
        preAuthSessionRepository.save(session);
    }

    @Scheduled(fixedDelayString = "${yaws.pre-auth.cleanup-fixed-delay-ms:300000}")
    @Transactional
    public void purgeExpiredSessions() {
        preAuthSessionRepository.purgeExpiredSessions(LocalDateTime.now());
    }

    private void throwAuthenticationFailed() {
        throw buildAuthenticationFailedException();
    }

    private ResponseStatusException buildAuthenticationFailedException() {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, "authentication failed");
    }
}
