package com.brcsrc.yaws.utility;

import com.brcsrc.yaws.model.Constants;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.Cookie;

public class HeaderUtils {
    private static final String ACCESS_TOKEN_COOKIE_KEY = "accessToken";
    private static final String PRE_AUTH_SESSION_COOKIE_KEY = "preAuthSession";

    /**
     * mutator function to set the HttpOnly authentication token. it will als set the
     * cookie attributes that inform the browser on how to treat the cookie
     * @param jwt String - the JSON web token to add to the cookie
     * @return response HttpServletResponse
     */
    public static String createResponseHttpOnlyAuthTokenCookieValue(String jwt) {
        return createCookieValue(ACCESS_TOKEN_COOKIE_KEY, jwt, Constants.AUTH_TOKEN_VALIDITY_PERIOD_SECONDS);
    }

    /**
     * creates an expired authentication token cookie to clear the user's session
     * @return String - the cookie header value with Max-Age=0 to delete the cookie
     */
    public static String createExpiredAuthTokenCookie() {
        return createCookieValue(ACCESS_TOKEN_COOKIE_KEY, "", 0);
    }

    public static String createResponsePreAuthSessionCookieValue(String sessionId, long maxAgeSeconds) {
        return createCookieValue(PRE_AUTH_SESSION_COOKIE_KEY, sessionId, maxAgeSeconds);
    }

    public static String createExpiredPreAuthSessionCookie() {
        return createCookieValue(PRE_AUTH_SESSION_COOKIE_KEY, "", 0);
    }

    /**
     * retrieves the authToken from a requests cookies
     * @param request HttpsServletRequest - the incoming request object
     * @return token String - the token that is expected to be on the request
     */
    public static String getRequestHttpOnlyAuthTokenCookieValue(HttpServletRequest request) {
        return getRequestCookieValueByKey(request, ACCESS_TOKEN_COOKIE_KEY);
    }

    public static String getRequestPreAuthSessionCookieValue(HttpServletRequest request) {
        return getRequestCookieValueByKey(request, PRE_AUTH_SESSION_COOKIE_KEY);
    }

    private static String getRequestCookieValueByKey(HttpServletRequest request, String cookieKey) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (cookieKey.equals(cookie.getName())) {
                return cookie.getValue();
            }
        }
        return null;
    }

    private static String createCookieValue(String cookieKey, String value, long maxAgeSeconds) {
        String cookieValue = cookieKey + "=" + value + "; " +
                "HttpOnly; " +
                "Path=/; " +
                "Max-Age=" + maxAgeSeconds + "; ";

        boolean isDev = Boolean.parseBoolean(System.getenv("DEV"));
        if (isDev) {
            cookieValue += "Domain=localhost; ";
            cookieValue += "SameSite=Lax;";
        } else {
            cookieValue += "SameSite=None; Secure;";
        }

        return cookieValue;
    }
}
