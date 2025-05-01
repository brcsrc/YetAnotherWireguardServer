package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.model.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;
import org.springframework.http.ResponseCookie;

import java.time.Duration;

public class HeaderUtils {
    /**
     * mutator function to set the HttpOnly authentication token. it will als set the
     * cookie attributes that inform the browser on how to treat the cookie
     * @param jwt String - the JSON web token to add to the cookie
     * @return response HttpServletResponse
     */
    public static String createResponseHttpOnlyAuthTokenCookieValue(String jwt) {
        String cookieValue = "accessToken=" + jwt + "; " +                         // token value stored in authToken key
                "HttpOnly; " +                                                     // ensure that browser javascript has no access, mitigate some XSS
                "Path=/; " +                                                       // use on all paths on the site
                "Max-Age=" + Constants.AUTH_TOKEN_VALIDITY_PERIOD_SECONDS + "; ";  // time in seconds before the browser deletes the cookie

        boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
        if (isDev) {
            cookieValue += "SameSite=Lax;";                                         // if dev allow cross origin use of the cookie for vite dev server
        } else {
            cookieValue += "SameSite=None; Secure;";                                // if not in dev then prevent cross origin and also tell the browser to
        }                                                                           // not store the cookie unless it was issued under https communication

        return cookieValue;
//    public static String createResponseHttpOnlyAuthTokenCookieValue(String jwt) {
//        ResponseCookie.ResponseCookieBuilder cookie =
//                ResponseCookie.from("accessToken", jwt)
//                        .httpOnly(true)
//                        .path("/")
//                        .maxAge(Duration.ofHours(3));
//
//        boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
//        if (isDev) {
//            cookie.sameSite("Lax");
//        } else {
//            cookie.sameSite("None").secure(true);
//        }
//
//        return cookie.build().toString();
    }

    /**
     * retrieves the authToken from a requests cookies
     * @param request HttpsServletRequest - the incoming request object
     * @return token String - the token that is expected to be on the request
     */
    public static String getRequestHttpOnlyAuthTokenCookieValue(HttpServletRequest request) {
        String token = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    token = cookie.getValue();
                }
            }
        }
        return token;
    }
}
