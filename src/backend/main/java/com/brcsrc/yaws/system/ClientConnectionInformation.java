package com.brcsrc.yaws.system;

public class ClientConnectionInformation {
    private String publicKey;
    private String presharedKey;
    private String endpoint;
    private String allowedIps;
    private Long latestHandshakeEpochSeconds;
    private Long bytesReceived;
    private Long bytesSent;
    private Long persistentKeepalive;

    public ClientConnectionInformation() {
    }

    public ClientConnectionInformation(String publicKey, String presharedKey, String endpoint,
                                      String allowedIps, Long latestHandshakeEpochSeconds,
                                      Long bytesReceived, Long bytesSent, Long persistentKeepalive) {
        this.publicKey = publicKey;
        this.presharedKey = presharedKey;
        this.endpoint = endpoint;
        this.allowedIps = allowedIps;
        this.latestHandshakeEpochSeconds = latestHandshakeEpochSeconds;
        this.bytesReceived = bytesReceived;
        this.bytesSent = bytesSent;
        this.persistentKeepalive = persistentKeepalive;
    }

    public String getPublicKey() {
        return publicKey;
    }

    public void setPublicKey(String publicKey) {
        this.publicKey = publicKey;
    }

    public String getPresharedKey() {
        return presharedKey;
    }

    public void setPresharedKey(String presharedKey) {
        this.presharedKey = presharedKey;
    }

    public String getEndpoint() {
        return endpoint;
    }

    public void setEndpoint(String endpoint) {
        this.endpoint = endpoint;
    }

    public String getAllowedIps() {
        return allowedIps;
    }

    public void setAllowedIps(String allowedIps) {
        this.allowedIps = allowedIps;
    }

    public Long getLatestHandshakeEpochSeconds() {
        return latestHandshakeEpochSeconds;
    }

    public void setLatestHandshakeEpochSeconds(Long latestHandshakeEpochSeconds) {
        this.latestHandshakeEpochSeconds = latestHandshakeEpochSeconds;
    }

    public Long getBytesReceived() {
        return bytesReceived;
    }

    public void setBytesReceived(Long bytesReceived) {
        this.bytesReceived = bytesReceived;
    }

    public Long getBytesSent() {
        return bytesSent;
    }

    public void setBytesSent(Long bytesSent) {
        this.bytesSent = bytesSent;
    }

    public Long getPersistentKeepalive() {
        return persistentKeepalive;
    }

    public void setPersistentKeepalive(Long persistentKeepalive) {
        this.persistentKeepalive = persistentKeepalive;
    }

    @Override
    public String toString() {
        return "ClientConnectionInformation{" +
                "publicKey='" + publicKey + '\'' +
                ", presharedKey='" + presharedKey + '\'' +
                ", endpoint='" + endpoint + '\'' +
                ", allowedIps='" + allowedIps + '\'' +
                ", latestHandshakeEpochSeconds=" + latestHandshakeEpochSeconds +
                ", bytesReceived=" + bytesReceived +
                ", bytesSent=" + bytesSent +
                ", persistentKeepalive=" + persistentKeepalive +
                '}';
    }
}
