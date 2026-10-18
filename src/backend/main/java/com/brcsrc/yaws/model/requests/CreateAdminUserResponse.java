package com.brcsrc.yaws.model.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A response object for creating the admin user.
 *
 * deliberately carries only the user name. the previous version of this endpoint returned the
 * User entity, which serialized the bcrypt password hash back to the caller.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateAdminUserResponse {

    private String userName;
}
