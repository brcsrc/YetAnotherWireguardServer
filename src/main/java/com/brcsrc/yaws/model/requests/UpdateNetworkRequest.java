package com.brcsrc.yaws.model.requests;

import com.brcsrc.yaws.model.NetworkStatus;

/**
 * A request object for updating the tag of a network.
 */
public class UpdateNetworkRequest {

    private String networkTag;
    private NetworkStatus networkStatus;

    public String getNetworkTag() {
        return networkTag;
    }

    public void setNetworkTag(String networkTag) {
        this.networkTag = networkTag;
    }

    public NetworkStatus getNetworkStatus() {
        return networkStatus;
    }

    public void setNetworkStatus(NetworkStatus networkStatus) {
        this.networkStatus = networkStatus;
    }

    @Override
    public String toString() {
        return "UpdateNetworkRequest{"
                + ", newTag='" + networkTag + '\''
                + ",  networkStatus=" + networkStatus
                + '}';
    }
}
