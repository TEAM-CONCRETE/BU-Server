package com.concrete.buildup.domain.auth.controller;

import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.service.AuthService;
import com.concrete.buildup.global.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * AuthController 단위 테스트
 */
@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@ActiveProfiles("test")
@DisplayName("AuthController 테스트")
class AuthControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AuthService authService;

    @Test
    @DisplayName("아이디 중복 확인 API - 존재하는 아이디")
    void checkUserIdExists_ExistingId() throws Exception {
        // given
        String userId = "existinguser";
        UserExistsResponse response = UserExistsResponse.builder()
                .exists(true)
                .build();
        given(authService.checkUserIdExists(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auth/exists")
                        .param("userId", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("아이디 중복 확인이 완료되었습니다"))
                .andExpect(jsonPath("$.data.exists").value(true));

        verify(authService).checkUserIdExists(userId);
    }

    @Test
    @DisplayName("아이디 중복 확인 API - 사용 가능한 아이디")
    void checkUserIdExists_AvailableId() throws Exception {
        // given
        String userId = "newuser";
        UserExistsResponse response = UserExistsResponse.builder()
                .exists(false)
                .build();
        given(authService.checkUserIdExists(userId)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/auth/exists")
                        .param("userId", userId))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("아이디 중복 확인이 완료되었습니다"))
                .andExpect(jsonPath("$.data.exists").value(false));

        verify(authService).checkUserIdExists(userId);
    }

    @Test
    @DisplayName("아이디 중복 확인 API - userId 파라미터 누락")
    void checkUserIdExists_MissingParameter() throws Exception {
        // when & then
        mockMvc.perform(get("/api/auth/exists"))
                .andDo(print())
                .andExpect(status().isBadRequest());
    }
}
