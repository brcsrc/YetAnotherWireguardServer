package com.brcsrc.yaws.twofa;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PreAuthSessionServiceTests {

    @Mock
    private PreAuthSessionRepository preAuthSessionRepository;

    private PreAuthSessionService preAuthSessionService;

    @BeforeEach
    public void setup() {
        preAuthSessionService = new PreAuthSessionService(preAuthSessionRepository);
    }

    @Test
    public void createSessionCreatesActiveSessionWithExpectedDefaults() {
        when(preAuthSessionRepository.save(any(PreAuthSession.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PreAuthSession createdSession = preAuthSessionService.createSession(1L, 5, Duration.ofMinutes(5));

        assertNotNull(createdSession.getId());
        assertEquals(1L, createdSession.getUserId());
        assertEquals(0, createdSession.getAttemptCount());
        assertEquals(5, createdSession.getMaxAttempts());
        assertEquals(PreAuthSessionState.ACTIVE, createdSession.getState());
        assertNotNull(createdSession.getCreatedAt());
        assertNotNull(createdSession.getExpiresAt());
        assertTrue(createdSession.getExpiresAt().isAfter(createdSession.getCreatedAt()));
        verify(preAuthSessionRepository, times(1)).save(any(PreAuthSession.class));
    }

    @Test
    public void requireActiveSessionThrows403OnNullSessionId() {
        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preAuthSessionService.requireActiveSession(null));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("authentication failed", exception.getReason());
    }

    @Test
    public void requireActiveSessionExpiresAndRejectsExpiredSession() {
        PreAuthSession session = new PreAuthSession();
        session.setId("session-1");
        session.setState(PreAuthSessionState.ACTIVE);
        session.setExpiresAt(LocalDateTime.now().minusMinutes(1));

        when(preAuthSessionRepository.findById("session-1")).thenReturn(Optional.of(session));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> preAuthSessionService.requireActiveSession("session-1"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("authentication failed", exception.getReason());

        ArgumentCaptor<PreAuthSession> sessionCaptor = ArgumentCaptor.forClass(PreAuthSession.class);
        verify(preAuthSessionRepository).save(sessionCaptor.capture());
        assertEquals(PreAuthSessionState.EXPIRED, sessionCaptor.getValue().getState());
    }

    @Test
    public void incrementAttemptOrLockLocksSessionAtMaxAttempts() {
        PreAuthSession session = new PreAuthSession();
        session.setAttemptCount(4);
        session.setMaxAttempts(5);
        session.setState(PreAuthSessionState.ACTIVE);

        preAuthSessionService.incrementAttemptOrLock(session);

        assertEquals(5, session.getAttemptCount());
        assertEquals(PreAuthSessionState.LOCKED, session.getState());
        verify(preAuthSessionRepository).save(session);
    }

    @Test
    public void consumeSessionMarksSessionAsConsumed() {
        PreAuthSession session = new PreAuthSession();
        session.setState(PreAuthSessionState.ACTIVE);

        preAuthSessionService.consumeSession(session);

        assertEquals(PreAuthSessionState.CONSUMED, session.getState());
        verify(preAuthSessionRepository).save(session);
    }
}
