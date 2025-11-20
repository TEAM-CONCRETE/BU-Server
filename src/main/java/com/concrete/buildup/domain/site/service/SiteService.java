package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.dto.SiteDetailResponse;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import com.concrete.buildup.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 현장 관리 서비스
 *
 * <p>현장 등록, 조회 등 현장 관리 기능을 제공합니다.</p>
 *
 * @author Build-Up Team
 * @since 1.0
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SiteService {

    private final SiteRepository siteRepository;
    private final UserRepository userRepository;
    private final CorporationRepository corporationRepository;
    private final SecretKeyService secretKeyService;

    /**
     * 현장 등록
     *
     * <p>CORPORATION 권한 사용자가 새로운 현장을 등록합니다.</p>
     * <p>현장 등록 시 자동으로 managerSecretKey와 employeeSecretKey가 생성됩니다.</p>
     *
     * @param request 현장 등록 요청 DTO
     * @return 현장 등록 응답 DTO (생성된 현장 정보 + 시크릿키 포함)
     * @throws BusinessException 사용자 미인증, 기업 정보 없음, 날짜 범위 오류 시
     */
    @Transactional
    public SiteCreateResponse createSite(SiteCreateRequest request) {
        // 1. 현재 인증된 사용자 조회
        String currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 2. 사용자의 기업 정보 조회
        Corporation corporation = corporationRepository.findByUserId(user.getId())
            .orElseThrow(() -> new BusinessException(SiteErrorCode.CORPORATION_NOT_FOUND));

        // 3. 임시 Site 생성 (ID 생성을 위해 먼저 저장)
        Site site = Site.builder()
            .siteName(request.getSiteName())
            .siteAddress(request.getSiteAddress())
            .clientName(request.getClientName())
            .startDate(request.getStartDate())
            .endDate(request.getEndDate())
            .corporation(corporation)
            .build();

        // 4. 날짜 범위 검증
        if (!site.isDateRangeValid()) {
            throw new BusinessException(SiteErrorCode.INVALID_DATE_RANGE);
        }

        // 5. 임시 저장하여 ID 생성 (시크릿키 생성에 필요)
        Site savedSite = siteRepository.save(site);

        // 6. 유니크한 시크릿키 생성
        SecretKeyService.SecretKeyPair secretKeyPair = secretKeyService.generateUniqueSecretKeys(
            corporation.getCorpName(),
            savedSite.getId()
        );

        // 7. 생성된 시크릿키 설정
        savedSite.setSecretKeys(
            secretKeyPair.getManagerSecretKey(),
            secretKeyPair.getEmployeeSecretKey()
        );

        log.info("현장 등록 완료 - siteId: {}, siteName: {}, corporation: {}",
            savedSite.getId(), savedSite.getSiteName(), corporation.getCorpName());

        // 8. 응답 DTO 생성
        return SiteCreateResponse.of(
            savedSite.getId(),
            savedSite.getSiteName(),
            savedSite.getSiteAddress(),
            savedSite.getClientName(),
            savedSite.getStartDate(),
            savedSite.getEndDate(),
            secretKeyPair.getManagerSecretKey(),
            secretKeyPair.getEmployeeSecretKey()
        );
    }

    /**
     * 현장 상세 조회
     *
     * <p>현장 ID로 현장 상세 정보를 조회합니다.</p>
     * <p>작업일보 작성 페이지 등에서 현장 기본 정보를 표시하는 데 사용됩니다.</p>
     *
     * @param siteId 현장 ID
     * @return 현장 상세 정보 DTO
     * @throws BusinessException 현장을 찾을 수 없는 경우
     */
    public SiteDetailResponse getSiteById(Long siteId) {
        log.info("현장 상세 조회 시작 - siteId: {}", siteId);

        // Manager 정보를 함께 조회 (N+1 문제 방지)
        Site site = siteRepository.findByIdWithManager(siteId)
            .orElseThrow(() -> {
                log.error("현장을 찾을 수 없음 - siteId: {}", siteId);
                return new BusinessException(SiteErrorCode.SITE_NOT_FOUND);
            });

        log.info("현장 상세 조회 완료 - siteId: {}, siteName: {}", siteId, site.getSiteName());

        return SiteDetailResponse.from(site);
    }
}
