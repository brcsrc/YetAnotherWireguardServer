package com.brcsrc.yaws.model.requests;

/**
 * A request object for updating the tag of a network.
 */
public class UpdateNetworkRequest {

    private String newTag;

    public String getNewTag() {
        return newTag;
    }

    public void setNewTag(String newTag) {
        this.newTag = newTag;
    }

    @Override
    public String toString() {
        return "UpdateNetworkRequest{" +
                ", newTag='" + newTag + '\'' +
                '}';
    }
}
