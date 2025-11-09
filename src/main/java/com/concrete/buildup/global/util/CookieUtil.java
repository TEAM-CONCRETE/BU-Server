package com.concrete.buildup.global.util;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.Arrays;
import java.util.Optional;

/**
 * 쿠키 유틸리티
 *
 * HTTP 쿠키 생성, 조회, 삭제 기능을 제공합니다.
 */
public class CookieUtil {

    /**
     * 쿠키 생성
     *
     * @param name 쿠키 이름
     * @param value 쿠키 값
     * @param maxAge 만료 시간 (초)
     * @param httpOnly HttpOnly 플래그
     * @param secure Secure 플래그
     * @return Cookie 객체
     */
    public static Cookie createCookie(String name, String value, int maxAge, boolean httpOnly, boolean secure) {
        Cookie cookie = new Cookie(name, value);
        cookie.setHttpOnly(httpOnly);
        cookie.setSecure(secure);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        return cookie;
    }

    /**
     * 쿠키 값 조회
     *
     * @param request HTTP 요청
     * @param name 쿠키 이름
     * @return 쿠키 값 (Optional)
     */
    public static Optional<String> getCookieValue(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return Optional.empty();
        }

        return Arrays.stream(cookies)
                .filter(cookie -> name.equals(cookie.getName()))
                .findFirst()
                .map(Cookie::getValue);
    }

    /**
     * 쿠키 삭제
     *
     * @param response HTTP 응답
     * @param name 쿠키 이름
     */
    public static void deleteCookie(HttpServletResponse response, String name) {
        Cookie cookie = new Cookie(name, null);
        cookie.setHttpOnly(true);
        cookie.setSecure(true);
        cookie.setPath("/");
        cookie.setMaxAge(0); // 즉시 만료
        response.addCookie(cookie);
    }
}
