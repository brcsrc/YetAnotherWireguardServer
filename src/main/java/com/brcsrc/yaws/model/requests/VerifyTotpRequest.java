package com.brcsrc.yaws.model.requests;

public class VerifyTotpRequest {
    public String otpCode;

    public VerifyTotpRequest() {}

    public VerifyTotpRequest(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
