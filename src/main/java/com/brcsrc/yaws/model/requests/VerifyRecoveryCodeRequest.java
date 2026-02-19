package com.brcsrc.yaws.model.requests;

public class VerifyRecoveryCodeRequest {
    public String recoveryCode;

    public VerifyRecoveryCodeRequest() {}

    public VerifyRecoveryCodeRequest(String recoveryCode) {
        this.recoveryCode = recoveryCode;
    }

    public String getRecoveryCode() {
        return recoveryCode;
    }

    public void setRecoveryCode(String recoveryCode) {
        this.recoveryCode = recoveryCode;
    }
}
