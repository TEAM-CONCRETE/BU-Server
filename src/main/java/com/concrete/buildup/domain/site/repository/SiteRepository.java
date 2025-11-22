package com.concrete.buildup.domain.site.repository;

import com.concrete.buildup.domain.site.entity.Site;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * 현장 Repository
 */
@Repository
public interface SiteRepository extends JpaRepository<Site, Long> {

    /**
     * 현장 관리자용 시크릿키로 현장 조회
     *
     * @param managerSecretKey 현장 관리자용 시크릿키
     * @return 현장 정보
     */
    Optional<Site> findByManagerSecretKey(String managerSecretKey);

    /**
     * 근로자용 시크릿키로 현장 조회
     *
     * @param employeeSecretKey 근로자용 시크릿키
     * @return 현장 정보
     */
    Optional<Site> findByEmployeeSecretKey(String employeeSecretKey);

    /**
     * 현장 관리자 User ID로 현장 조회
     *
     * @param userId 현장 관리자 User ID
     * @return 현장 정보
     */
    @Query("SELECT s FROM Site s JOIN s.manager m WHERE m.user.id = :userId")
    Optional<Site> findByManagerUserId(@Param("userId") Long userId);

    /**
     * 현장 관리자로 현장 조회
     *
     * @param manager 현장 관리자 엔티티
     * @return 현장 정보
     */
    Optional<Site> findByManager(com.concrete.buildup.domain.auth.entity.Manager manager);

    /**
     * 현장 관리자용 시크릿키 중복 확인
     *
     * @param managerSecretKey 현장 관리자용 시크릿키
     * @return 존재 여부
     */
    boolean existsByManagerSecretKey(String managerSecretKey);

    /**
     * 근로자용 시크릿키 중복 확인
     *
     * @param employeeSecretKey 근로자용 시크릿키
     * @return 존재 여부
     */
    boolean existsByEmployeeSecretKey(String employeeSecretKey);

    /**
     * 현장 ID로 조회 (Manager JOIN FETCH)
     *
     * <p>N+1 문제 방지를 위해 Manager를 함께 조회합니다.</p>
     *
     * @param siteId 현장 ID
     * @return 현장 정보 (Manager 포함)
     */
    @Query("SELECT s FROM Site s LEFT JOIN FETCH s.manager WHERE s.id = :siteId")
    Optional<Site> findByIdWithManager(@Param("siteId") Long siteId);
}
