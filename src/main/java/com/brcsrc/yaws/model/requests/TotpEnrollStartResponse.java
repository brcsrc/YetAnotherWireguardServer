package com.brcsrc.yaws.model.requests;

public class TotpEnrollStartResponse {
    public String issuer;
    public String accountName;
    public String manualEntryKey;
    public String otpauthUri;

    public TotpEnrollStartResponse() {}

    public TotpEnrollStartResponse(String issuer, String accountName, String manualEntryKey, String otpauthUri) {
        this.issuer = issuer;
        this.accountName = accountName;
        this.manualEntryKey = manualEntryKey;
        this.otpauthUri = otpauthUri;
    }

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getManualEntryKey() {
        return manualEntryKey;
    }

    public void setManualEntryKey(String manualEntryKey) {
        this.manualEntryKey = manualEntryKey;
    }

    public String getOtpauthUri() {
        return otpauthUri;
    }

    public void setOtpauthUri(String otpauthUri) {
        this.otpauthUri = otpauthUri;
    }
}
