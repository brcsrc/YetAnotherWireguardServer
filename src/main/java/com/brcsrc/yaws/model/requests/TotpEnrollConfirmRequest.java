package com.brcsrc.yaws.model.requests;

public class TotpEnrollConfirmRequest {
    public String otpCode;

    public TotpEnrollConfirmRequest() {}

    public TotpEnrollConfirmRequest(String otpCode) {
        this.otpCode = otpCode;
    }

    public String getOtpCode() {
        return otpCode;
    }

    public void setOtpCode(String otpCode) {
        this.otpCode = otpCode;
    }
}
