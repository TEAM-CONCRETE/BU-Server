package com.concrete.buildup.domain.auth.service;

import com.concrete.buildup.domain.auth.dto.UserExistsResponse;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 인증/인가 서비스
 *
 * <p>회원가입, 로그인, 아이디 중복 확인 등 인증 관련 비즈니스 로직을 처리합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;

    /**
     * 아이디 중복 확인
     *
     * @param userId 확인할 사용자 ID
     * @return UserExistsResponse - exists: true(사용 중), false(사용 가능)
     */
    public UserExistsResponse checkUserIdExists(String userId) {
        log.debug("아이디 중복 확인 요청: userId={}", userId);

        boolean exists = userRepository.existsByUserId(userId);

        log.debug("아이디 중복 확인 결과: userId={}, exists={}", userId, exists);

        return UserExistsResponse.builder()
                .exists(exists)
                .build();
    }
}
