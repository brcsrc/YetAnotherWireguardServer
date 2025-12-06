package com.brcsrc.yaws.model.requests;

/**
 * A request object for streaming client WireGuard connection information.
 */
public class StreamClientConnectionInfoRequest {

    private String clientPublicKeyValue;

    public String getClientPublicKeyValue() {
        return clientPublicKeyValue;
    }

    public void setClientPublicKeyValue(String clientPublicKeyValue) {
        this.clientPublicKeyValue = clientPublicKeyValue;
    }

    @Override
    public String toString() {
        return "StreamClientConnectionInfoRequest{"
                + "clientPublicKeyValue='" + clientPublicKeyValue + '\''
                + '}';
    }
}
