package com.brcsrc.yaws.service;

import com.brcsrc.yaws.model.Constants;
import com.brcsrc.yaws.model.User;
import com.brcsrc.yaws.model.requests.AuthenticateStartResponse;
import com.brcsrc.yaws.model.requests.RegenerateRecoveryCodesResponse;
import com.brcsrc.yaws.model.requests.TotpEnrollStartResponse;
import com.brcsrc.yaws.persistence.UserRepository;
import com.brcsrc.yaws.security.JwtService;
import com.brcsrc.yaws.security.UserDetailsServiceImpl;
import com.brcsrc.yaws.twofa.PreAuthSession;
import com.brcsrc.yaws.twofa.PreAuthSessionService;
import com.brcsrc.yaws.twofa.TwoFactorMethod;
import com.brcsrc.yaws.twofa.recovery.RecoveryCode;
import com.brcsrc.yaws.twofa.recovery.RecoveryCodeRepository;
import com.brcsrc.yaws.twofa.totp.TotpService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final UserDetailsServiceImpl userDetailsService;
    private final JwtService jwtService;
    private final PreAuthSessionService preAuthSessionService;
    private final TotpService totpService;
    private final RecoveryCodeRepository recoveryCodeRepository;

    private static final Logger logger = LoggerFactory.getLogger(UserService.class);
    private final SecureRandom secureRandom = new SecureRandom();

    @Autowired
    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            AuthenticationManager authenticationManager,
            UserDetailsServiceImpl userDetailsService,
            JwtService jwtService,
            PreAuthSessionService preAuthSessionService,
            TotpService totpService,
            RecoveryCodeRepository recoveryCodeRepository
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.authenticationManager = authenticationManager;
        this.userDetailsService = userDetailsService;
        this.jwtService = jwtService;
        this.preAuthSessionService = preAuthSessionService;
        this.totpService = totpService;
        this.recoveryCodeRepository = recoveryCodeRepository;
    }

    public String authenticateAndIssueToken(@RequestBody User user) {
        AuthenticateStartResult startResult = authenticateStart(user.getUserName(), user.getPassword());
        if (startResult.isTwoFactorRequired()) {
            throwAuthenticationFailed();
        }
        return startResult.getIssuedJwt();
    }

    public AuthenticateStartResult authenticateStart(String userName, String password) {
        Authentication authentication  = this.authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(userName, password)
        );

        if (!authentication.isAuthenticated()) {
            String errMsg = "authentication failed for user: " + userName;
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, errMsg);
        }

        User user = this.userRepository.findByUserName(userName)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "authentication failed"));

        if (isTwoFactorGloballyEnabled() && isTotpEnabled(user)) {
            PreAuthSession session = preAuthSessionService.createSession(
                    user.getId(),
                    Constants.PRE_AUTH_MAX_ATTEMPTS,
                    Duration.ofMillis(Constants.PRE_AUTH_TOKEN_VALIDITY_PERIOD_MILLIS)
            );
            List<String> allowedSecondFactors = new ArrayList<>();
            allowedSecondFactors.add(TwoFactorMethod.TOTP.name());
            if (hasActiveRecoveryCodes(user.getId())) {
                allowedSecondFactors.add(TwoFactorMethod.RECOVERY.name());
            }
            return new AuthenticateStartResult(
                    true,
                    null,
                    session.getId(),
                    session.getExpiresAt(),
                    allowedSecondFactors
            );
        }

        String jwt = this.jwtService.generateToken(this.userDetailsService.loadUserByUsername(userName));
        return new AuthenticateStartResult(false, jwt, null, null, List.of());
    }

    public String verifyTotpAndIssueToken(String preAuthSessionId, String otpCode) {
        enforceTwoFactorGloballyEnabledOrThrow();
        PreAuthSession session = preAuthSessionService.requireActiveSession(preAuthSessionId);
        User user = this.userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "authentication failed"));

        if (!isTotpEnabled(user)) {
            preAuthSessionService.expireSession(session);
            throwAuthenticationFailed();
        }

        totpService.validateOtpFormatOrThrow(otpCode);
        OptionalLong matchedCounter = totpService.validateCodeAndGetCounter(user.getTotpSecretEncrypted(), otpCode, Instant.now());
        if (matchedCounter.isEmpty()) {
            preAuthSessionService.incrementAttemptOrLock(session);
            throwAuthenticationFailed();
        }

        if (session.getLastUsedTotpCounter() != null && session.getLastUsedTotpCounter() == matchedCounter.getAsLong()) {
            preAuthSessionService.incrementAttemptOrLock(session);
            throwAuthenticationFailed();
        }

        session.setLastUsedTotpCounter(matchedCounter.getAsLong());

        preAuthSessionService.consumeSession(session);
        return this.jwtService.generateToken(this.userDetailsService.loadUserByUsername(user.getUserName()));
    }

    @Transactional
    public String verifyRecoveryCodeAndIssueToken(String preAuthSessionId, String recoveryCode) {
        enforceTwoFactorGloballyEnabledOrThrow();
        PreAuthSession session = preAuthSessionService.requireActiveSession(preAuthSessionId);
        User user = this.userRepository.findById(session.getUserId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.FORBIDDEN, "authentication failed"));

        if (!isTotpEnabled(user)) {
            preAuthSessionService.expireSession(session);
            throwAuthenticationFailed();
        }

        String normalizedRecoveryCode = normalizeRecoveryCodeOrThrow(recoveryCode);
        List<RecoveryCode> activeRecoveryCodes = recoveryCodeRepository.findByUserIdAndDisabledFalseAndUsedAtIsNull(user.getId());

        RecoveryCode matchedRecoveryCode = null;
        for (RecoveryCode activeRecoveryCode : activeRecoveryCodes) {
            if (passwordEncoder.matches(normalizedRecoveryCode, activeRecoveryCode.getCodeHash())) {
                matchedRecoveryCode = activeRecoveryCode;
                break;
            }
        }

        if (matchedRecoveryCode == null) {
            preAuthSessionService.incrementAttemptOrLock(session);
            throwAuthenticationFailed();
        }

        matchedRecoveryCode.setUsedAt(LocalDateTime.now());
        matchedRecoveryCode.setDisabled(true);
        recoveryCodeRepository.save(matchedRecoveryCode);

        preAuthSessionService.consumeSession(session);
        return this.jwtService.generateToken(this.userDetailsService.loadUserByUsername(user.getUserName()));
    }

    @Transactional
    public TotpEnrollStartResponse startTotpEnrollment(String jwt) {
        enforceTwoFactorGloballyEnabledOrThrow();
        User user = getUserFromJwt(jwt);
        String secret = totpService.generateBase32Secret();
        String encryptedSecret = totpService.encryptSecret(secret);
        String issuer = getTotpIssuer();

        user.setTotpSecretEncrypted(encryptedSecret);
        user.setTwoFactorMethod(TwoFactorMethod.TOTP.name());
        user.setTwoFactorEnabled(false);
        user.setTotpEnabledAt(null);
        userRepository.save(user);

        String otpAuthUri = totpService.buildOtpAuthUri(issuer, user.getUserName(), secret);
        return new TotpEnrollStartResponse(issuer, user.getUserName(), secret, otpAuthUri);
    }

    @Transactional
    public void confirmTotpEnrollment(String jwt, String otpCode) {
        enforceTwoFactorGloballyEnabledOrThrow();
        User user = getUserFromJwt(jwt);
        totpService.validateOtpFormatOrThrow(otpCode);

        if (user.getTotpSecretEncrypted() == null || user.getTotpSecretEncrypted().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "totp setup has not been started");
        }

        if (totpService.validateCodeAndGetCounter(user.getTotpSecretEncrypted(), otpCode, Instant.now()).isEmpty()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "authentication failed");
        }

        user.setTwoFactorEnabled(true);
        user.setTwoFactorMethod(TwoFactorMethod.TOTP.name());
        user.setTotpEnabledAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Transactional
    public RegenerateRecoveryCodesResponse regenerateRecoveryCodes(String jwt) {
        enforceTwoFactorGloballyEnabledOrThrow();
        User user = getUserFromJwt(jwt);
        recoveryCodeRepository.deleteByUserId(user.getId());

        List<String> plaintextCodes = new ArrayList<>();
        List<RecoveryCode> recoveryCodeEntities = new ArrayList<>();
        for (int i = 0; i < Constants.RECOVERY_CODES_COUNT; i++) {
            String code = generateRecoveryCode();
            plaintextCodes.add(code);

            RecoveryCode recoveryCode = new RecoveryCode();
            recoveryCode.setUserId(user.getId());
            recoveryCode.setCodeHash(passwordEncoder.encode(code));
            recoveryCode.setCreatedAt(LocalDateTime.now());
            recoveryCode.setUsedAt(null);
            recoveryCode.setDisabled(false);
            recoveryCodeEntities.add(recoveryCode);
        }

        recoveryCodeRepository.saveAll(recoveryCodeEntities);
        return new RegenerateRecoveryCodesResponse(plaintextCodes);
    }

    private boolean isTotpEnabled(User user) {
        if (!user.isTwoFactorEnabled()) {
            return false;
        }
        if (user.getTwoFactorMethod() == null) {
            return false;
        }
        if (user.getTotpSecretEncrypted() == null || user.getTotpSecretEncrypted().isBlank()) {
            return false;
        }
        return TwoFactorMethod.TOTP.name().equalsIgnoreCase(user.getTwoFactorMethod());
    }

    private boolean hasActiveRecoveryCodes(Long userId) {
        List<RecoveryCode> activeRecoveryCodes = recoveryCodeRepository.findByUserIdAndDisabledFalseAndUsedAtIsNull(userId);
        return activeRecoveryCodes != null && !activeRecoveryCodes.isEmpty();
    }

    private String normalizeRecoveryCodeOrThrow(String recoveryCode) {
        if (recoveryCode == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recovery code must be in format XXXX-XXXX");
        }

        String trimmed = recoveryCode.trim().toUpperCase();
        String compressed = trimmed.replace(" ", "");

        if (compressed.matches("^[A-Z2-9]{8}$")) {
            return compressed.substring(0, 4) + "-" + compressed.substring(4, 8);
        }
        if (compressed.matches("^[A-Z2-9]{4}-[A-Z2-9]{4}$")) {
            return compressed;
        }

        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "recovery code must be in format XXXX-XXXX");
    }

    private String generateRecoveryCode() {
        final String alphabet = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder codeBuilder = new StringBuilder();
        for (int i = 0; i < 8; i++) {
            int idx = secureRandom.nextInt(alphabet.length());
            codeBuilder.append(alphabet.charAt(idx));
        }
        return codeBuilder.substring(0, 4) + "-" + codeBuilder.substring(4, 8);
    }

    private User getUserFromJwt(String jwt) {
        String username = whoami(jwt);
        return userRepository.findByUserName(username)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "authenticated user not found"));
    }

    private String getTotpIssuer() {
        String issuer = System.getenv("TOTP_ISSUER");
        if (issuer == null || issuer.isBlank()) {
            return "YAWS";
        }
        return issuer;
    }

    private void throwAuthenticationFailed() {
        throw new ResponseStatusException(HttpStatus.FORBIDDEN, "authentication failed");
    }

    public boolean isTwoFactorGloballyEnabled() {
        String twoFaEnabled = System.getenv("TWO_FA_ENABLED");
        if (twoFaEnabled == null || twoFaEnabled.isBlank()) {
            return true;
        }
        return Boolean.parseBoolean(twoFaEnabled);
    }

    private void enforceTwoFactorGloballyEnabledOrThrow() {
        if (!isTwoFactorGloballyEnabled()) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "two-factor authentication is globally disabled by TWO_FA_ENABLED");
        }
    }

    public String whoami(String jwt) {
        return this.jwtService.extractUsernameFromJwt(jwt);
    }

    protected static boolean passwordMeetsComplexityRequirements(String requestedPassword) {
        int lowercaseAlphaChars = 0;
        int upperCaseAlphaChars = 0;
        int specialChars = 0;
        int numberChars = 0;
        int unmatchedChars = 0;

        int minLowercaseChars = 2;
        int minUpperCaseChars = 2;
        int minSpecialChars = 1;
        int minNumberChars = 1;
        int maxUnmatchedChars = 0;

        int passwordLength = requestedPassword.length();

        for (int i = 0; i < passwordLength; i++) {
            Character c = requestedPassword.charAt(i);

            if (Character.isUpperCase(c)) {
                upperCaseAlphaChars++;
                continue;
            } else if (Character.isLowerCase(c)) {
                lowercaseAlphaChars++;
                continue;
            } else if (Character.isDigit(c)) {
                numberChars++;
                continue;
            } else if (Constants.ADMIN_USER_PASSWORD_ALLOWED_SPECIAL_CHARS.contains(c.toString())) {
                specialChars++;
                continue;
            }
            // if we get to this block then the char is unmatched and we dont want it
            unmatchedChars++;
        }

        boolean meetsComplexityRequirements = (
            lowercaseAlphaChars >= minLowercaseChars &&
            upperCaseAlphaChars >= minUpperCaseChars &&
            specialChars >= minSpecialChars &&
            numberChars >= minNumberChars &&
            unmatchedChars <= maxUnmatchedChars &&
            passwordLength >= Constants.ADMIN_USER_PASSWORD_MIN_LENGTH
        );

        return meetsComplexityRequirements;
    }

    @Transactional
    public User createAdminUser(User newUser) {
        // input validation
        if (!newUser.getUserName().matches(Constants.CHAR_32_ALPHANUMERIC_DASHES_UNDERSC_REGEX)) {
            String errMsg = "user name must be 4-32 characters alphanumeric with dashes or underscores and with no spaces";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        if (newUser.getPassword().length() < Constants.ADMIN_USER_PASSWORD_MIN_LENGTH) {
            String errMsg = "password must be at least 12 characters long";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        if (!passwordMeetsComplexityRequirements(newUser.getPassword())) {
            String errMsg = "password must have 2 lowercase letters, 2 uppercase letters, 1 special character and 1 number";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        // if there already is an admin user we reject the registration request
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isPresent()) {
            String errMsg = "cannot create admin user, admin user already exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }

        User user = new User();
        user.setId(Constants.ADMIN_USER_ID);
        user.setUserName(newUser.getUserName());

        // bcrypt the password so it is stored not in plaintext
        user.setPassword(passwordEncoder.encode(newUser.getPassword()));
        User savedUser = userRepository.save(user);
        logger.info("successfully created admin user");
        return savedUser;
    }

    @Transactional
    public User updateAdminUserName(String newUserName) {
        // TODO validate new user name input
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isEmpty()) {
            String errMsg = "cannot update admin user name, no admin user exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        User adminUser = adminUserOpt.get();
        adminUser.setUserName(newUserName);
        return userRepository.save(adminUser);
    }

    @Transactional
    public User updateAdminUserPassword(String newPassword) {
        // TODO validate new password input
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isEmpty()) {
            String errMsg = "cannot update admin password, no admin user exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, errMsg);
        }
        // TODO create complexity requirements for password
        User adminUser = adminUserOpt.get();
        adminUser.setPassword(passwordEncoder.encode(newPassword));
        User savedUser = userRepository.save(adminUser);
        logger.info("successfully successfully updated admin user password");
        return savedUser;
    }

    public User getAdminUser() {
        Optional<User> adminUserOpt = userRepository.findById(Constants.ADMIN_USER_ID);
        if (adminUserOpt.isEmpty()) {
            String errMsg = "cannot get admin user, no admin user exists";
            logger.error(errMsg);
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, errMsg);
        }
        return adminUserOpt.get();
    }

    public static class AuthenticateStartResult {
        private final boolean twoFactorRequired;
        private final String issuedJwt;
        private final String preAuthSessionId;
        private final java.time.LocalDateTime challengeExpiresAt;
        private final List<String> allowedSecondFactors;

        public AuthenticateStartResult(
                boolean twoFactorRequired,
                String issuedJwt,
                String preAuthSessionId,
                java.time.LocalDateTime challengeExpiresAt,
                List<String> allowedSecondFactors
        ) {
            this.twoFactorRequired = twoFactorRequired;
            this.issuedJwt = issuedJwt;
            this.preAuthSessionId = preAuthSessionId;
            this.challengeExpiresAt = challengeExpiresAt;
            this.allowedSecondFactors = allowedSecondFactors;
        }

        public boolean isTwoFactorRequired() {
            return twoFactorRequired;
        }

        public String getIssuedJwt() {
            return issuedJwt;
        }

        public String getPreAuthSessionId() {
            return preAuthSessionId;
        }

        public java.time.LocalDateTime getChallengeExpiresAt() {
            return challengeExpiresAt;
        }

        public List<String> getAllowedSecondFactors() {
            return allowedSecondFactors;
        }

        public AuthenticateStartResponse toResponse() {
            return new AuthenticateStartResponse(twoFactorRequired, allowedSecondFactors, challengeExpiresAt);
        }
    }
}
