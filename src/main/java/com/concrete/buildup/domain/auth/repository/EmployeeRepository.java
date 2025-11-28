package com.concrete.buildup.domain.auth.repository;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Employee Repository
 *
 * <p>근로자 엔티티에 대한 데이터 접근 계층입니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Repository
public interface EmployeeRepository extends JpaRepository<Employee, Long> {

    /**
     * User ID로 근로자 조회 (1:1 관계)
     *
     * @param userId User 엔티티 ID
     * @return 근로자 엔티티 (Optional)
     */
    @Query("SELECT e FROM Employee e WHERE e.user.id = :userId")
    Optional<Employee> findByUserId(@Param("userId") Long userId);

    /**
     * 근로자 유형으로 근로자 목록 조회
     *
     * @param empType 근로자 유형 (DAILY/PERMANENT)
     * @return 근로자 목록
     */
    List<Employee> findByEmpType(String empType);

    /**
     * 미계약 근로자 목록 조회 (emp_type이 NULL인 근로자)
     *
     * @return 미계약 근로자 목록
     */
    List<Employee> findByEmpTypeIsNull();

    /**
     * 근로자 이름으로 근로자 목록 조회 (Like 검색)
     *
     * @param empName 근로자 이름
     * @return 근로자 목록
     */
    List<Employee> findByEmpNameContaining(String empName);

    /**
     * Employee ID로 근로자 + User 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User를 함께 조회
     *
     * @param id 근로자 ID
     * @return 근로자 + User (Optional)
     */
    @Query("SELECT e FROM Employee e JOIN FETCH e.user WHERE e.id = :id")
    Optional<Employee> findByIdWithUser(@Param("id") Long id);

    /**
     * User ID로 근로자 + User + Role 조회 (Fetch Join)
     * N+1 문제 방지를 위해 User와 Role을 함께 조회
     *
     * @param userId User 엔티티 ID
     * @return 근로자 + User + Role (Optional)
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.user u " +
           "LEFT JOIN FETCH u.role " +
           "WHERE u.id = :userId")
    Optional<Employee> findByUserIdWithUserAndRole(@Param("userId") Long userId);

    /**
     * User 엔티티로 근로자 조회
     *
     * @param user User 엔티티
     * @return 근로자 엔티티 (Optional)
     */
    Optional<Employee> findByUser(User user);

    /**
     * 전화번호로 근로자 조회 (출퇴근 검증용)
     * User의 phone 또는 Employee의 sub_phone으로 근로자를 조회합니다.
     * 전화번호 비교 시 하이픈, 공백, 괄호를 제거하여 정규화된 형태로 비교합니다.
     *
     * @param phone 전화번호 (정규화된 형태, 숫자만)
     * @return 근로자 + User (Optional)
     */
    @Query(value = "SELECT e.* FROM employees e " +
                   "JOIN users u ON e.user_id = u.id " +
                   "WHERE REPLACE(REPLACE(REPLACE(u.phone, '-', ''), ' ', ''), '(', '') = :phone " +
                   "   OR REPLACE(REPLACE(REPLACE(IFNULL(e.sub_phone, ''), '-', ''), ' ', ''), '(', '') = :phone " +
                   "LIMIT 1",
           nativeQuery = true)
    Optional<Employee> findByPhoneWithUser(@Param("phone") String phone);

    /**
     * 현장 ID로 미계약 근로자 목록 조회 (emp_type이 NULL인 근로자)
     *
     * <p>User 테이블의 site_id를 기준으로 해당 현장에 소속된 미계약 근로자를 조회합니다.</p>
     * <p>UNCONTRACTED 근로자는 Contract 테이블에 레코드가 없으므로,
     * User.siteId를 통해 현장 필터링이 필요합니다.</p>
     *
     * @param siteId 현장 ID
     * @return 해당 현장의 미계약 근로자 목록 (Employee + User)
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.user u " +
           "WHERE e.empType IS NULL " +
           "AND u.siteId = :siteId")
    List<Employee> findUncontractedBySiteId(@Param("siteId") Long siteId);

    /**
     * 현장 ID와 근로자 유형으로 근로자 목록 조회
     *
     * <p>User 테이블의 site_id를 기준으로 해당 현장에 소속된 특정 유형의 근로자를 조회합니다.</p>
     *
     * @param siteId 현장 ID
     * @param empType 근로자 유형 (DAILY/PERMANENT)
     * @return 해당 현장의 근로자 목록 (Employee + User)
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.user u " +
           "WHERE e.empType = :empType " +
           "AND u.siteId = :siteId")
    List<Employee> findByEmpTypeAndSiteId(@Param("empType") String empType, @Param("siteId") Long siteId);

    /**
     * Employee ID 목록으로 근로자 + User 일괄 조회 (Fetch Join)
     *
     * <p>N+1 문제 방지를 위해 User를 함께 조회합니다.</p>
     * <p>기본 findAllById() 대신 이 메서드를 사용하여 성능을 최적화합니다.</p>
     *
     * @param ids Employee ID 목록
     * @return 근로자 + User 목록
     */
    @Query("SELECT e FROM Employee e " +
           "JOIN FETCH e.user " +
           "WHERE e.id IN :ids")
    List<Employee> findAllByIdInWithUser(@Param("ids") List<Long> ids);
}