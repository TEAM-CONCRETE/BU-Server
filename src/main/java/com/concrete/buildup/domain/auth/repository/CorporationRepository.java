package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Corporation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Corporation Repository
 *
 * <p>기업 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface CorporationRepository extends JpaRepository<Corporation, Long> {

    /**
     * User ID로 기업 조회 (1:1 관계)
     *
     * @param userId User 엔티티 ID
     * @return 기업 엔티티 (Optional)
     */
    @Query("SELECT c FROM Corporation c WHERE c.user.id = :userId")
    Optional<Corporation> findByUserId(@Param("userId") Long userId);

    /**
     * 회사명으로 기업 목록 조회 (Like 검색)
     *
     * @param corpName 회사명
     * @return 기업 목록
     */
    List<Corporation> findByCorpNameContaining(String corpName);

    /**
     * Corporation ID로 기업 + User 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User를 함께 조회
     *
     * @param id 기업 ID
     * @return 기업 + User (Optional)
     */
    @Query("SELECT c FROM Corporation c JOIN FETCH c.user WHERE c.id = :id")
    Optional<Corporation> findByIdWithUser(@Param("id") Long id);

    /**
     * User ID로 기업 + User + Role 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User와 Role을 함께 조회
     *
     * @param userId User 엔티티 ID
     * @return 기업 + User + Role (Optional)
     */
    @Query("SELECT c FROM Corporation c " +
           "JOIN FETCH c.user u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE u.id = :userId")
    Optional<Corporation> findByUserIdWithUserAndRole(@Param("userId") Long userId);
}