package com.brcsrc.yaws.security;

import io.jsonwebtoken.Jwts;
import jakarta.xml.bind.DatatypeConverter;

import javax.crypto.SecretKey;

public class JwtSecretKeyGenerator {
    /**
     * utility function for building the server side secret key that is used to sign issued JWTs returned to the
     * client on successful auth as well as verifying JWTs attached to supposedly authenticated client requests
     * @return encodedKey String - the String representation of the secret key
     */
    public static String generateSecretKey() {
        SecretKey key = Jwts.SIG.HS512.key().build();
        return DatatypeConverter.printHexBinary(key.getEncoded());
    }
}
