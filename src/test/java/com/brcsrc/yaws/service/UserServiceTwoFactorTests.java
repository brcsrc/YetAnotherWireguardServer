package com.brcsrc.yaws.service;

import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.requests.RegenerateRecoveryCodesResponse;
import com.brcsrc.yaws.model.requests.TotpEnrollStartResponse;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.security.JwtService;
import com.brcsrc.yaws.security.UserDetailsServiceImpl;
import com.brcsrc.yaws.twofa.PreAuthSession;
import com.brcsrc.yaws.twofa.PreAuthSessionService;
import com.brcsrc.yaws.twofa.PreAuthSessionState;
import com.brcsrc.yaws.twofa.recovery.RecoveryCode;
import com.brcsrc.yaws.twofa.recovery.RecoveryCodeRepository;
import com.brcsrc.yaws.twofa.totp.TotpService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTwoFactorTests {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private UserDetailsServiceImpl userDetailsService;
    @Mock
    private JwtService jwtService;
    @Mock
    private PreAuthSessionService preAuthSessionService;
    @Mock
    private TotpService totpService;
    @Mock
    private RecoveryCodeRepository recoveryCodeRepository;
    @Mock
    private Authentication authentication;

    private UserService userService;

    @BeforeEach
    public void setup() {
        userService = new UserService(
                userRepository,
                passwordEncoder,
                authenticationManager,
                userDetailsService,
                jwtService,
                preAuthSessionService,
                totpService,
                recoveryCodeRepository
        );
    }

    @Test
    public void authenticateStartIssuesJwtWhenTwoFactorDisabled() {
        User user = buildUser(false, null, null);
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password("hash")
                .authorities("USER")
                .build();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(user.getUserName())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        UserService.AuthenticateStartResult result = userService.authenticateStart(user.getUserName(), "password");

        assertFalse(result.isTwoFactorRequired());
        assertEquals("jwt-token", result.getIssuedJwt());
        assertNull(result.getPreAuthSessionId());
        assertTrue(result.getAllowedSecondFactors().isEmpty());
        verify(preAuthSessionService, never()).createSession(anyLong(), anyInt(), any());
    }

    @Test
    public void authenticateStartReturnsChallengeWhenTwoFactorEnabled() {
        User user = buildUser(true, "TOTP", "encrypted-secret");

        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);
        preAuthSession.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(preAuthSessionService.createSession(anyLong(), anyInt(), any())).thenReturn(preAuthSession);
        when(recoveryCodeRepository.findByUserIdAndDisabledFalseAndUsedAtIsNull(user.getId())).thenReturn(List.of());

        UserService.AuthenticateStartResult result = userService.authenticateStart(user.getUserName(), "password");

        assertTrue(result.isTwoFactorRequired());
        assertNull(result.getIssuedJwt());
        assertEquals("pre-auth-id", result.getPreAuthSessionId());
        assertEquals(1, result.getAllowedSecondFactors().size());
        assertEquals("TOTP", result.getAllowedSecondFactors().get(0));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    public void authenticateStartIncludesRecoveryWhenActiveCodesExist() {
        User user = buildUser(true, "TOTP", "encrypted-secret");

        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);
        preAuthSession.setExpiresAt(LocalDateTime.now().plusMinutes(5));

        RecoveryCode activeRecoveryCode = new RecoveryCode();
        activeRecoveryCode.setCodeHash("hashed");
        activeRecoveryCode.setDisabled(false);
        activeRecoveryCode.setUsedAt(null);

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(preAuthSessionService.createSession(anyLong(), anyInt(), any())).thenReturn(preAuthSession);
        when(recoveryCodeRepository.findByUserIdAndDisabledFalseAndUsedAtIsNull(user.getId()))
                .thenReturn(List.of(activeRecoveryCode));

        UserService.AuthenticateStartResult result = userService.authenticateStart(user.getUserName(), "password");

        assertTrue(result.isTwoFactorRequired());
        assertTrue(result.getAllowedSecondFactors().contains("TOTP"));
        assertTrue(result.getAllowedSecondFactors().contains("RECOVERY"));
    }

    @Test
    public void authenticateStartSkipsSecondFactorWhenGloballyDisabled() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password("hash")
                .authorities("USER")
                .build();

        UserService serviceWithGlobalDisable = spy(userService);
        doReturn(false).when(serviceWithGlobalDisable).isTwoFactorGloballyEnabled();

        when(authenticationManager.authenticate(any())).thenReturn(authentication);
        when(authentication.isAuthenticated()).thenReturn(true);
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(userDetailsService.loadUserByUsername(user.getUserName())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("jwt-token");

        UserService.AuthenticateStartResult result = serviceWithGlobalDisable.authenticateStart(user.getUserName(), "password");

        assertFalse(result.isTwoFactorRequired());
        assertEquals("jwt-token", result.getIssuedJwt());
        verify(preAuthSessionService, never()).createSession(anyLong(), anyInt(), any());
    }

    @Test
    public void verifyTotpAndIssueTokenIssuesJwtOnValidCode() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password("hash")
                .authorities("USER")
                .build();

        when(preAuthSessionService.requireActiveSession("pre-auth-id")).thenReturn(preAuthSession);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.validateCodeAndGetCounter(eq(user.getTotpSecretEncrypted()), eq("123456"), any()))
                .thenReturn(OptionalLong.of(123L));
        when(userDetailsService.loadUserByUsername(user.getUserName())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("final-jwt");

        String issuedJwt = userService.verifyTotpAndIssueToken("pre-auth-id", "123456");

        assertEquals("final-jwt", issuedJwt);
        verify(preAuthSessionService).consumeSession(preAuthSession);
        verify(preAuthSessionService, never()).incrementAttemptOrLock(any());
    }

    @Test
    public void verifyTotpAndIssueTokenRejectsInvalidCodeAndIncrementsAttempts() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);

        when(preAuthSessionService.requireActiveSession("pre-auth-id")).thenReturn(preAuthSession);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.validateCodeAndGetCounter(eq(user.getTotpSecretEncrypted()), eq("123456"), any()))
                .thenReturn(OptionalLong.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.verifyTotpAndIssueToken("pre-auth-id", "123456"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("authentication failed", exception.getReason());
        verify(preAuthSessionService).incrementAttemptOrLock(preAuthSession);
        verify(preAuthSessionService, never()).consumeSession(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    public void verifyTotpAndIssueTokenRejectsReplayCounterAndIncrementsAttempts() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);
        preAuthSession.setLastUsedTotpCounter(123L);

        when(preAuthSessionService.requireActiveSession("pre-auth-id")).thenReturn(preAuthSession);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(totpService.validateCodeAndGetCounter(eq(user.getTotpSecretEncrypted()), eq("123456"), any()))
                .thenReturn(OptionalLong.of(123L));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.verifyTotpAndIssueToken("pre-auth-id", "123456"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("authentication failed", exception.getReason());
        verify(preAuthSessionService).incrementAttemptOrLock(preAuthSession);
        verify(preAuthSessionService, never()).consumeSession(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    public void verifyRecoveryCodeAndIssueTokenIssuesJwtOnValidCode() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);

        RecoveryCode recoveryCode = new RecoveryCode();
        recoveryCode.setId(10L);
        recoveryCode.setUserId(user.getId());
        recoveryCode.setCodeHash("hashed-recovery");

        UserDetails userDetails = org.springframework.security.core.userdetails.User
                .withUsername(user.getUserName())
                .password("hash")
                .authorities("USER")
                .build();

        when(preAuthSessionService.requireActiveSession("pre-auth-id")).thenReturn(preAuthSession);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recoveryCodeRepository.findByUserIdAndDisabledFalseAndUsedAtIsNull(user.getId()))
                .thenReturn(List.of(recoveryCode));
        when(passwordEncoder.matches("ABCD-2345", "hashed-recovery")).thenReturn(true);
        when(userDetailsService.loadUserByUsername(user.getUserName())).thenReturn(userDetails);
        when(jwtService.generateToken(userDetails)).thenReturn("final-jwt");

        String issuedJwt = userService.verifyRecoveryCodeAndIssueToken("pre-auth-id", "abcd-2345");

        assertEquals("final-jwt", issuedJwt);
        assertNotNull(recoveryCode.getUsedAt());
        assertTrue(recoveryCode.isDisabled());
        verify(recoveryCodeRepository).save(recoveryCode);
        verify(preAuthSessionService).consumeSession(preAuthSession);
    }

    @Test
    public void verifyRecoveryCodeAndIssueTokenRejectsInvalidCodeAndIncrementsAttempts() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        PreAuthSession preAuthSession = new PreAuthSession();
        preAuthSession.setId("pre-auth-id");
        preAuthSession.setUserId(user.getId());
        preAuthSession.setState(PreAuthSessionState.ACTIVE);

        RecoveryCode recoveryCode = new RecoveryCode();
        recoveryCode.setCodeHash("hashed-recovery");

        when(preAuthSessionService.requireActiveSession("pre-auth-id")).thenReturn(preAuthSession);
        when(userRepository.findById(user.getId())).thenReturn(Optional.of(user));
        when(recoveryCodeRepository.findByUserIdAndDisabledFalseAndUsedAtIsNull(user.getId()))
                .thenReturn(List.of(recoveryCode));
        when(passwordEncoder.matches("ABCD-2345", "hashed-recovery")).thenReturn(false);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> userService.verifyRecoveryCodeAndIssueToken("pre-auth-id", "abcd-2345"));

        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
        assertEquals("authentication failed", exception.getReason());
        verify(preAuthSessionService).incrementAttemptOrLock(preAuthSession);
        verify(preAuthSessionService, never()).consumeSession(any());
    }

    @Test
    public void startTotpEnrollmentPersistsSecretAndReturnsSetupResponse() {
        User user = buildUser(false, null, null);
        String jwt = "jwt-token";

        when(jwtService.extractUsernameFromJwt(jwt)).thenReturn(user.getUserName());
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(totpService.generateBase32Secret()).thenReturn("ABCDEFGH23456789");
        when(totpService.encryptSecret("ABCDEFGH23456789")).thenReturn("encrypted-secret");
        when(totpService.buildOtpAuthUri(anyString(), eq(user.getUserName()), eq("ABCDEFGH23456789")))
                .thenReturn("otpauth://totp/YAWS:admin?secret=ABCDEFGH23456789&issuer=YAWS");

        TotpEnrollStartResponse response = userService.startTotpEnrollment(jwt);

        assertEquals("admin", response.getAccountName());
        assertEquals("ABCDEFGH23456789", response.getManualEntryKey());
        assertNotNull(response.getOtpauthUri());
        assertEquals("encrypted-secret", user.getTotpSecretEncrypted());
        assertEquals("TOTP", user.getTwoFactorMethod());
        assertFalse(user.isTwoFactorEnabled());
        verify(userRepository).save(user);
    }

    @Test
    public void confirmTotpEnrollmentEnablesTwoFactorOnValidCode() {
        User user = buildUser(false, "TOTP", "encrypted-secret");
        String jwt = "jwt-token";

        when(jwtService.extractUsernameFromJwt(jwt)).thenReturn(user.getUserName());
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(totpService.validateCodeAndGetCounter(eq("encrypted-secret"), eq("123456"), any()))
                .thenReturn(OptionalLong.of(123L));

        userService.confirmTotpEnrollment(jwt, "123456");

        assertTrue(user.isTwoFactorEnabled());
        assertEquals("TOTP", user.getTwoFactorMethod());
        assertNotNull(user.getTotpEnabledAt());
        verify(userRepository).save(user);
    }

    @Test
    public void regenerateRecoveryCodesReturnsConfiguredNumberOfCodes() {
        User user = buildUser(true, "TOTP", "encrypted-secret");
        String jwt = "jwt-token";

        when(jwtService.extractUsernameFromJwt(jwt)).thenReturn(user.getUserName());
        when(userRepository.findByUserName(user.getUserName())).thenReturn(Optional.of(user));
        when(passwordEncoder.encode(anyString())).thenAnswer(invocation -> "hashed-" + invocation.getArgument(0));

        RegenerateRecoveryCodesResponse response = userService.regenerateRecoveryCodes(jwt);

        assertNotNull(response.getCodes());
        assertEquals(10, response.getCodes().size());
        assertTrue(response.getCodes().stream().allMatch(code -> code.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}$")));
        verify(recoveryCodeRepository).deleteByUserId(user.getId());
        verify(recoveryCodeRepository).saveAll(anyList());
    }

    private User buildUser(boolean twoFactorEnabled, String method, String encryptedSecret) {
        User user = new User();
        user.setId(1L);
        user.setUserName("admin");
        user.setPassword("hashed-password");
        user.setTwoFactorEnabled(twoFactorEnabled);
        user.setTwoFactorMethod(method);
        user.setTotpSecretEncrypted(encryptedSecret);
        return user;
    }
}
