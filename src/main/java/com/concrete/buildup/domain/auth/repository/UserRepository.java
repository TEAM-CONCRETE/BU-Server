package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * User Repository
 *
 * <p>사용자 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

    /**
     * 로그인용 ID로 사용자 조회
     *
     * @param userId 로그인용 ID
     * @return 사용자 엔티티 (Optional)
     */
    Optional<User> findByUserId(String userId);

    /**
     * 로그인용 ID 존재 여부 확인
     *
     * @param userId 로그인용 ID
     * @return 존재 여부
     */
    boolean existsByUserId(String userId);

    /**
     * 로그인용 ID로 사용자 + 역할 조회 (Fetch Join)
     * N+1 문제 방지를 위해 Role을 함께 조회
     *
     * @param userId 로그인용 ID
     * @return 사용자 + 역할 (Optional)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.userId = :userId")
    Optional<User> findByUserIdWithRole(@Param("userId") String userId);

    /**
     * ID로 사용자 + 역할 조회 (Fetch Join)
     * N+1 문제 방지를 위해 Role을 함께 조회
     *
     * @param id 사용자 ID
     * @return 사용자 + 역할 (Optional)
     */
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.role WHERE u.id = :id")
    Optional<User> findByIdWithRole(@Param("id") Long id);

    /**
     * 프로필 완성 토큰으로 사용자 조회
     *
     * @param profileToken 프로필 완성 토큰
     * @return 사용자 엔티티 (Optional)
     */
    Optional<User> findByProfileToken(String profileToken);
}