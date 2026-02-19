package com.brcsrc.yaws.model.requests;

import java.time.LocalDateTime;
import java.util.List;

public class AuthenticateStartResponse {
    public boolean twoFactorRequired;
    public List<String> allowedSecondFactors;
    public LocalDateTime challengeExpiresAt;

    public AuthenticateStartResponse() {}

    public AuthenticateStartResponse(boolean twoFactorRequired, List<String> allowedSecondFactors, LocalDateTime challengeExpiresAt) {
        this.twoFactorRequired = twoFactorRequired;
        this.allowedSecondFactors = allowedSecondFactors;
        this.challengeExpiresAt = challengeExpiresAt;
    }

    public boolean isTwoFactorRequired() {
        return twoFactorRequired;
    }

    public void setTwoFactorRequired(boolean twoFactorRequired) {
        this.twoFactorRequired = twoFactorRequired;
    }

    public List<String> getAllowedSecondFactors() {
        return allowedSecondFactors;
    }

    public void setAllowedSecondFactors(List<String> allowedSecondFactors) {
        this.allowedSecondFactors = allowedSecondFactors;
    }

    public LocalDateTime getChallengeExpiresAt() {
        return challengeExpiresAt;
    }

    public void setChallengeExpiresAt(LocalDateTime challengeExpiresAt) {
        this.challengeExpiresAt = challengeExpiresAt;
    }
}
