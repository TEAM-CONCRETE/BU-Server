package com.concrete.buildup.global.security;

import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * CustomUserDetailsService
 *
 * Spring Security의 UserDetailsService 구현체
 * 사용자 인증 시 userId로 사용자 정보를 조회합니다.
 */
@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String userId) throws UsernameNotFoundException {
        User user = userRepository.findByUserIdWithRole(userId)
                .orElseThrow(() -> new UsernameNotFoundException("사용자를 찾을 수 없습니다: " + userId));

        // Role이 없는 경우 기본 권한 부여
        String authority = "ROLE_USER";
        if (user.getRole() != null && user.getRole().getRoleName() != null) {
            authority = "ROLE_" + user.getRole().getRoleName();
        }

        return org.springframework.security.core.userdetails.User.builder()
                .username(user.getUserId())
                .password(user.getPassword())
                .authorities(authority)
                .build();
    }
}
