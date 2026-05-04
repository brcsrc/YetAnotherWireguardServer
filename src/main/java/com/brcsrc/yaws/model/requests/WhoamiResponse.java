package com.brcsrc.yaws.model.requests;

public class WhoamiResponse {
    public String user;
    public Boolean twoFactorGloballyEnabled;

    public WhoamiResponse() {}

    public WhoamiResponse(String user) {
        this.user = user;
    }

    public WhoamiResponse(String user, Boolean twoFactorGloballyEnabled) {
        this.user = user;
        this.twoFactorGloballyEnabled = twoFactorGloballyEnabled;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public Boolean getTwoFactorGloballyEnabled() {
        return twoFactorGloballyEnabled;
    }

    public void setTwoFactorGloballyEnabled(Boolean twoFactorGloballyEnabled) {
        this.twoFactorGloballyEnabled = twoFactorGloballyEnabled;
    }

    @Override
    public String toString() {
        return "WhoamiResponse{" +
                "user='" + user + '\'' +
                ", twoFactorGloballyEnabled='" + twoFactorGloballyEnabled + '\'' +
                '}';
    }
}
