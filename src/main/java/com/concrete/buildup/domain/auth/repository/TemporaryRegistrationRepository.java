package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.TemporaryRegistration;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;

/**
 * 임시 회원가입 정보 Repository
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface TemporaryRegistrationRepository extends JpaRepository<TemporaryRegistration, Long> {

    /**
     * 등록 토큰으로 임시 등록 정보 조회
     *
     * @param registrationToken 등록 토큰
     * @return Optional<TemporaryRegistration>
     */
    Optional<TemporaryRegistration> findByRegistrationToken(String registrationToken);

    /**
     * 사용자 ID로 임시 등록 정보 조회
     *
     * @param userId 사용자 ID
     * @return Optional<TemporaryRegistration>
     */
    Optional<TemporaryRegistration> findByUserId(String userId);

    /**
     * 사용자 ID 존재 여부 확인
     *
     * @param userId 사용자 ID
     * @return true if exists, false otherwise
     */
    boolean existsByUserId(String userId);

    /**
     * 만료된 임시 등록 정보 삭제
     *
     * @param expiresAt 만료 시간 기준
     */
    void deleteByExpiresAtBefore(LocalDateTime expiresAt);
}
