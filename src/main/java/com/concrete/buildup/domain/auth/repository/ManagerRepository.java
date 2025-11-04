package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Manager;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Manager Repository
 *
 * <p>현장 관리자 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface ManagerRepository extends JpaRepository<Manager, Long> {

    /**
     * User ID로 관리자 조회 (1:1 관계)
     *
     * @param userId User 엔티티 ID
     * @return 관리자 엔티티 (Optional)
     */
    @Query("SELECT m FROM Manager m WHERE m.user.id = :userId")
    Optional<Manager> findByUserId(@Param("userId") Long userId);

    /**
     * 관리자 이름으로 관리자 목록 조회 (Like 검색)
     *
     * @param managerName 관리자 이름
     * @return 관리자 목록
     */
    List<Manager> findByManagerNameContaining(String managerName);

    /**
     * Manager ID로 관리자 + User 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User를 함께 조회
     *
     * @param id 관리자 ID
     * @return 관리자 + User (Optional)
     */
    @Query("SELECT m FROM Manager m JOIN FETCH m.user WHERE m.id = :id")
    Optional<Manager> findByIdWithUser(@Param("id") Long id);

    /**
     * User ID로 관리자 + User + Role 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User와 Role을 함께 조회
     *
     * @param userId User 엔티티 ID
     * @return 관리자 + User + Role (Optional)
     */
    @Query("SELECT m FROM Manager m " +
           "JOIN FETCH m.user u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE u.id = :userId")
    Optional<Manager> findByUserIdWithUserAndRole(@Param("userId") Long userId);
}