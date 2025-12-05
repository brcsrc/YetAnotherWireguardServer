package com.brcsrc.yaws.model.requests;

/**
 * A request object for streaming network WireGuard connection information.
 */
public class StreamNetworkConnectionInfoRequest {

    private String networkPublicKeyValue;

    public String getNetworkPublicKeyValue() {
        return networkPublicKeyValue;
    }

    public void setNetworkPublicKeyValue(String networkPublicKeyValue) {
        this.networkPublicKeyValue = networkPublicKeyValue;
    }

    @Override
    public String toString() {
        return "StreamNetworkConnectionInfoRequest{"
                + "networkPublicKeyValue='" + networkPublicKeyValue + '\''
                + '}';
    }
}
