package com.concrete.buildup.global.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * JwtTokenProvider 단위 테스트
 */
class JwtTokenProviderTest {

    private JwtTokenProvider jwtTokenProvider;
    private String testUserId;
    private String testRole;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider();

        // 테스트용 설정 주입
        ReflectionTestUtils.setField(jwtTokenProvider, "secretKey",
            "test-secret-key-min-256-bits-long-for-hs256-algorithm-test");
        ReflectionTestUtils.setField(jwtTokenProvider, "accessTokenExpiration", 3600000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenExpiration", 604800000L);
        ReflectionTestUtils.setField(jwtTokenProvider, "refreshTokenRememberMeExpiration", 2592000000L);

        // init() 메서드 호출하여 초기화
        jwtTokenProvider.init();

        testUserId = "testUser";
        testRole = "EMPLOYEE";
    }

    @Test
    @DisplayName("Access Token 생성 및 검증 테스트")
    void testGenerateAndValidateAccessToken() {
        // given & when
        String token = jwtTokenProvider.generateAccessToken(testUserId, testRole);

        // then
        assertThat(token).isNotNull();
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Access Token에서 사용자 ID 추출 테스트")
    void testGetUserIdFromToken() {
        // given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testRole);

        // when
        String extractedUserId = jwtTokenProvider.getUserIdFromToken(token);

        // then
        assertEquals(testUserId, extractedUserId);
    }

    @Test
    @DisplayName("Access Token에서 역할(Role) 추출 테스트")
    void testGetRoleFromToken() {
        // given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testRole);

        // when
        String extractedRole = jwtTokenProvider.getRoleFromToken(token);

        // then
        assertEquals(testRole, extractedRole);
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증 테스트 (자동 로그인 X)")
    void testGenerateAndValidateRefreshToken() {
        // given & when
        String token = jwtTokenProvider.generateRefreshToken(testUserId, false);

        // then
        assertThat(token).isNotNull();
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("Refresh Token 생성 및 검증 테스트 (자동 로그인 O)")
    void testGenerateAndValidateRefreshTokenWithRememberMe() {
        // given & when
        String token = jwtTokenProvider.generateRefreshToken(testUserId, true);

        // then
        assertThat(token).isNotNull();
        assertTrue(jwtTokenProvider.validateToken(token));
    }

    @Test
    @DisplayName("토큰 타입 확인 테스트 - Access Token")
    void testGetTokenTypeForAccessToken() {
        // given
        String token = jwtTokenProvider.generateAccessToken(testUserId, testRole);

        // when
        String tokenType = jwtTokenProvider.getTokenType(token);

        // then
        assertEquals("access", tokenType);
    }

    @Test
    @DisplayName("토큰 타입 확인 테스트 - Refresh Token")
    void testGetTokenTypeForRefreshToken() {
        // given
        String token = jwtTokenProvider.generateRefreshToken(testUserId, false);

        // when
        String tokenType = jwtTokenProvider.getTokenType(token);

        // then
        assertEquals("refresh", tokenType);
    }

    @Test
    @DisplayName("잘못된 토큰 검증 실패 테스트")
    void testValidateInvalidToken() {
        // given
        String invalidToken = "invalid.token.value";

        // when & then
        assertFalse(jwtTokenProvider.validateToken(invalidToken));
    }

    @Test
    @DisplayName("여러 역할로 토큰 생성 테스트")
    void testGenerateTokenWithDifferentRoles() {
        // given
        String managerRole = "MANAGER";
        String corporationRole = "CORPORATION";

        // when
        String managerToken = jwtTokenProvider.generateAccessToken(testUserId, managerRole);
        String corporationToken = jwtTokenProvider.generateAccessToken(testUserId, corporationRole);

        // then
        assertEquals(managerRole, jwtTokenProvider.getRoleFromToken(managerToken));
        assertEquals(corporationRole, jwtTokenProvider.getRoleFromToken(corporationToken));
    }
}
