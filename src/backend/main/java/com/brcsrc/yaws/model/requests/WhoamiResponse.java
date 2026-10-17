package com.brcsrc.yaws.model.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object carrying the username of the authenticated caller.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WhoamiResponse {

    private String user;
}
