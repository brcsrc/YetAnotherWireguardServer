package com.brcsrc.yaws.model.requests;

public class WhoamiResponse {
    public String user;

    public WhoamiResponse() {}

    public WhoamiResponse(String user) {
        this.user = user;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    @Override
    public String toString() {
        return "WhoamiResponse{" +
                "user='" + user + '\'' +
                '}';
    }
}