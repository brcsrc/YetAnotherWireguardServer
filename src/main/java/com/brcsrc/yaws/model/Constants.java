package com.brcsrc.yaws.model;

public class Constants {
    // base controller url route
    public static final String BASE_URL = "/api/v1";
    // base wireguard dir
    public static final String BASE_WIREGUARD_DIR = "/etc/wireguard";
    // input validation regular expressions
    // network and client names
    public static final String CHAR_64_ALPHANUMERIC_DASHES_UNDERSC_REGEXP = "^[a-zA-Z0-9_-]{4,64}$";
    // ip related fields
    public static final String IPV4_CIDR_REGEXP = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(3[0-2]|[1-2][0-9]|[0-9]))$";
    public static final String IPV4_ADDRESS_REGEXP = "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$";
    // admin user row id is always 1L because we can only have one
    public static final Long ADMIN_USER_ID = 1L;
    public static final int ADMIN_USER_PASSWORD_MIN_LENGTH = 12;
    public static final String ADMIN_USER_PASSWORD_ALLOWED_SPECIAL_CHARS = "/*!@#$%^&*()\"{}_[]|\\?/<>,.=";
    // pattern for usernames, 32 chars alphanumeric with dashes or underscores, no special chars
    public static final String CHAR_32_ALPHANUMERIC_DASHES_UNDERSC_REGEX = "^[a-zA-Z0-9_-]{4,32}$";
}
