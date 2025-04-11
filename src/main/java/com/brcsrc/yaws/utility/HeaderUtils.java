package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.model.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

public class HeaderUtils {
    /**
     * mutator function to set the HttpOnly authentication token. it will als set the
     * cookie attributes that inform the browser on how to treat the cookie
     * @param response HttpServletResponse - the response object to modify
     * @param jwt String - the JSON web token to add to the cookie
     * @return response HttpServletResponse
     */
    public static String createResponseHttpOnlyAuthTokenCookieValue(String jwt) {
        String cookieValue = "accessToken=" + jwt + "; " +                  // token value stored in authToken key
                "HttpOnly; " +                                              // ensure that browser javascript has no access, mitigate some XSS
                //"Secure; " +                                              // force the browser to only use HTTPS proto TODO set via env var if TLS enabled
                "SameSite=Strict; " +                                       // prevent the cookie from being used on other sites
                "Path=/; " +                                                // use on all paths on the site
                "Max-Age=" + Constants.AUTH_TOKEN_VALIDITY_PERIOD_SECONDS;  // time in seconds before the browser deletes the cookie

        return cookieValue;
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
