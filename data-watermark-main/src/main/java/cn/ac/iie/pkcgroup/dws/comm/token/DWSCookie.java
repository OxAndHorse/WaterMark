package cn.ac.iie.pkcgroup.dws.comm.token;

import javax.servlet.http.Cookie;

public class DWSCookie {
    private final static int expireTime = 30 * 60; // 30min

    public Cookie generateDWSCookie(String cookieName, String cookieValue) {
        Cookie cookie = new Cookie(cookieName, cookieValue);
        cookie.setMaxAge(expireTime);

        return cookie;
    }
}
