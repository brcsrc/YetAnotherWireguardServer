package com.brcsrc.yaws.model.requests;

import java.util.List;

public class RegenerateRecoveryCodesResponse {
    public List<String> codes;

    public RegenerateRecoveryCodesResponse() {}

    public RegenerateRecoveryCodesResponse(List<String> codes) {
        this.codes = codes;
    }

    public List<String> getCodes() {
        return codes;
    }

    public void setCodes(List<String> codes) {
        this.codes = codes;
    }
}
