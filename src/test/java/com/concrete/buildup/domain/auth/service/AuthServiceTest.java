package com.concrete.buildup.domain.auth.service;

import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;

/**
 * AuthService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService 테스트")
class AuthServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthService authService;

    @Test
    @DisplayName("아이디 중복 확인 - 존재하는 아이디")
    void checkUserIdExists_ExistingId() {
        // given
        String userId = "existinguser";
        given(userRepository.existsByUserId(userId)).willReturn(true);

        // when
        UserExistsResponse response = authService.checkUserIdExists(userId);

        // then
        assertThat(response.isExists()).isTrue();
        verify(userRepository).existsByUserId(userId);
    }

    @Test
    @DisplayName("아이디 중복 확인 - 존재하지 않는 아이디 (사용 가능)")
    void checkUserIdExists_AvailableId() {
        // given
        String userId = "newuser";
        given(userRepository.existsByUserId(userId)).willReturn(false);

        // when
        UserExistsResponse response = authService.checkUserIdExists(userId);

        // then
        assertThat(response.isExists()).isFalse();
        verify(userRepository).existsByUserId(userId);
    }
}
