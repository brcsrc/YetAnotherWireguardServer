package com.brcsrc.yaws.security;

import com.brcsrc.yaws.model.Constants;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.sql.Date;
import java.time.Instant;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class JwtService {
    /**
     * this class will generate new JWTs and parse given JWTs by the expected claim fields:
     *  {
     *      "iss": "yaws.local",    // issuing domain
     *      "sub": "admin",         // username
     *      "iat": 1742973723,      // issued at time
     *      "exp": 1742984523       // expiry time
     *  }
     */

    private static final String SECRET = JwtSecretKeyGenerator.generateSecretKey();     // yaws will start with new secret key on each startup

    /**
     * used to generate a new JWT for a UserDetails object. this will be given to the client to
     * use in later requests to show that they are already authenticated successfully
     * @param userDetails org.springframework.security.core.userdetails.UserDetails
     * @return token String
     */
    public String generateToken(UserDetails userDetails) {
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        Map<String, String> claims = new HashMap<>();
        claims.put("iss", "yaws.local"); // example of adding additional claim fields to jwt
        return Jwts.builder()
                .claims(claims)
                .subject(userDetails.getUsername())
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(Instant.now().plusMillis(Constants.AUTH_TOKEN_VALIDITY_PERIOD_MILLIS)))
                .signWith(secretKey)
                .compact();
        // can test JWT at https://jwt.io/
    }

    // this function is critical to authentication security because it verifies
    // the JWT is signed with the secret key that the JWT was generated from
    private Claims verifyJwtAndParseClaims(String jwt) {
        SecretKey secretKey = Keys.hmacShaKeyFor(Base64.getDecoder().decode(SECRET));
        return Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }

    /**
     * used to parse an existing JWT that is supplied by the client to determine the user
     * that the JWT was issued to by parsing out the subject of the JWT claims
     * @param jwt String
     * @return username String
     */
    public String extractUsernameFromJwt(String jwt) {
        return verifyJwtAndParseClaims(jwt).getSubject();
    }

    /**
     * used to parse an existing JWT that is supplied by the client to check if
     * the "exp" claim (Expiry time) is later than the current time which determines
     * that the JWT is still valid and the user does not need to re-authenticate
     * @param jwt String
     * @return isValid boolean
     */
    public boolean isTokenValid(String jwt) {
        return verifyJwtAndParseClaims(jwt).getExpiration().after(Date.from(Instant.now()));
    }
}
