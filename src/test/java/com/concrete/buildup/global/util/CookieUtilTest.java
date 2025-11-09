package com.concrete.buildup.global.util;

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * CookieUtil 단위 테스트
 */
@DisplayName("CookieUtil 테스트")
class CookieUtilTest {

    @Test
    @DisplayName("쿠키 생성 - 기본 옵션")
    void createCookie_Default() {
        // given
        String name = "testCookie";
        String value = "testValue";
        int maxAge = 3600;

        // when
        Cookie cookie = CookieUtil.createCookie(name, value, maxAge, true, true);

        // then
        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo(name);
        assertThat(cookie.getValue()).isEqualTo(value);
        assertThat(cookie.getMaxAge()).isEqualTo(maxAge);
        assertThat(cookie.isHttpOnly()).isTrue();
        assertThat(cookie.getSecure()).isTrue();
        assertThat(cookie.getPath()).isEqualTo("/");
    }

    @Test
    @DisplayName("쿠키 생성 - HttpOnly 및 Secure 플래그 false")
    void createCookie_WithoutSecurityFlags() {
        // given
        String name = "insecureCookie";
        String value = "insecureValue";
        int maxAge = 1800;

        // when
        Cookie cookie = CookieUtil.createCookie(name, value, maxAge, false, false);

        // then
        assertThat(cookie).isNotNull();
        assertThat(cookie.getName()).isEqualTo(name);
        assertThat(cookie.getValue()).isEqualTo(value);
        assertThat(cookie.isHttpOnly()).isFalse();
        assertThat(cookie.getSecure()).isFalse();
    }

    @Test
    @DisplayName("쿠키 값 조회 - 쿠키가 존재하는 경우")
    void getCookieValue_CookieExists() {
        // given
        String cookieName = "refreshToken";
        String cookieValue = "test-refresh-token-value";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie(cookieName, cookieValue));

        // when
        Optional<String> result = CookieUtil.getCookieValue(request, cookieName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(cookieValue);
    }

    @Test
    @DisplayName("쿠키 값 조회 - 쿠키가 존재하지 않는 경우")
    void getCookieValue_CookieNotExists() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(new Cookie("otherCookie", "otherValue"));

        // when
        Optional<String> result = CookieUtil.getCookieValue(request, "refreshToken");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("쿠키 값 조회 - 요청에 쿠키가 없는 경우")
    void getCookieValue_NoCookies() {
        // given
        MockHttpServletRequest request = new MockHttpServletRequest();
        // 쿠키를 설정하지 않음

        // when
        Optional<String> result = CookieUtil.getCookieValue(request, "refreshToken");

        // then
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("쿠키 값 조회 - 여러 쿠키 중에서 특정 쿠키 찾기")
    void getCookieValue_MultipleCookies() {
        // given
        String targetCookieName = "refreshToken";
        String targetCookieValue = "correct-token";

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setCookies(
                new Cookie("sessionId", "session123"),
                new Cookie(targetCookieName, targetCookieValue),
                new Cookie("userId", "user456")
        );

        // when
        Optional<String> result = CookieUtil.getCookieValue(request, targetCookieName);

        // then
        assertThat(result).isPresent();
        assertThat(result.get()).isEqualTo(targetCookieValue);
    }

    @Test
    @DisplayName("쿠키 삭제 - 정상 삭제")
    void deleteCookie_Success() {
        // given
        String cookieName = "refreshToken";
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        CookieUtil.deleteCookie(response, cookieName);

        // then
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSize(1);

        Cookie deletedCookie = cookies[0];
        assertThat(deletedCookie.getName()).isEqualTo(cookieName);
        assertThat(deletedCookie.getValue()).isNull();
        assertThat(deletedCookie.getMaxAge()).isEqualTo(0);  // 즉시 만료
        assertThat(deletedCookie.isHttpOnly()).isTrue();
        assertThat(deletedCookie.getSecure()).isTrue();
        assertThat(deletedCookie.getPath()).isEqualTo("/");
    }

    @Test
    @DisplayName("쿠키 삭제 - 여러 쿠키 삭제")
    void deleteCookie_MultipleCookies() {
        // given
        MockHttpServletResponse response = new MockHttpServletResponse();

        // when
        CookieUtil.deleteCookie(response, "cookie1");
        CookieUtil.deleteCookie(response, "cookie2");

        // then
        Cookie[] cookies = response.getCookies();
        assertThat(cookies).isNotNull();
        assertThat(cookies).hasSize(2);

        assertThat(cookies[0].getName()).isEqualTo("cookie1");
        assertThat(cookies[0].getMaxAge()).isEqualTo(0);

        assertThat(cookies[1].getName()).isEqualTo("cookie2");
        assertThat(cookies[1].getMaxAge()).isEqualTo(0);
    }
}