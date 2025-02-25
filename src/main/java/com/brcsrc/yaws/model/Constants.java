package com.brcsrc.yaws.model;

public class Constants {
    // base controller url route
    public static final String BASE_URL = "/api/v1";
    // input validation regular expressions
    // network and client names
    public static final String CHAR_64_ALPHANUMERIC_REGEXP = "^[a-zA-Z0-9]+$";
    // ip related fields
    public static final String IPV4_CIDR_REGEXP = "^(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\/(3[0-2]|[1-2][0-9]|[0-9]))$";
    public static final String IPV4_ADDRESS_REGEXP = "^((25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])\\.){3}(25[0-5]|2[0-4][0-9]|1[0-9]{2}|[1-9]?[0-9])$";
}
