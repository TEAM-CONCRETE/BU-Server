package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.CorporationRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.safetydoc.entity.SafetyEducationLog;
import com.concrete.buildup.domain.safetydoc.repository.SafetyEducationLogRepository;
import com.concrete.buildup.domain.site.dto.SafetyWorkDocumentDto;
import com.concrete.buildup.domain.site.dto.SafetyWorkDocumentListResponse;
import com.concrete.buildup.domain.site.dto.SiteCreateRequest;
import com.concrete.buildup.domain.site.dto.SiteCreateResponse;
import com.concrete.buildup.domain.site.dto.SiteDetailResponse;
import com.concrete.buildup.domain.site.dto.SiteListResponse;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.repository.WorkReportRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.concrete.buildup.global.exception.errorcode.AuthErrorCode;
import com.concrete.buildup.global.exception.errorcode.SiteErrorCode;
import com.concrete.buildup.global.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

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
    private final SafetyEducationLogRepository safetyEducationLogRepository;
    private final WorkReportRepository workReportRepository;

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
     * 기업 관리자의 현장 목록 조회
     *
     * <p>현재 로그인한 기업 관리자가 담당하는 모든 현장 목록을 조회합니다.</p>
     * <p>왼쪽 사이드바에 현장 목록을 표시하는 데 사용됩니다.</p>
     *
     * @return 현장 목록 DTO
     * @throws BusinessException 사용자 또는 기업 정보를 찾을 수 없는 경우
     */
    public SiteListResponse getMySites() {
        // 1. 현재 인증된 사용자 조회
        String currentUserId = SecurityUtil.getCurrentUserId();
        User user = userRepository.findByUserId(currentUserId)
            .orElseThrow(() -> new BusinessException(AuthErrorCode.USER_NOT_FOUND));

        // 2. 사용자의 기업 정보 조회
        Corporation corporation = corporationRepository.findByUserId(user.getId())
            .orElseThrow(() -> new BusinessException(SiteErrorCode.CORPORATION_NOT_FOUND));

        // 3. 기업이 담당하는 현장 목록 조회
        List<Site> sites = siteRepository.findByCorporationIdWithManager(corporation.getId());

        log.info("기업 현장 목록 조회 완료 - corporationId: {}, siteCount: {}",
            corporation.getId(), sites.size());

        return SiteListResponse.from(sites);
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

    /**
     * 현장의 안전/작업 문서 목록 조회 (기업 관리자용)
     *
     * <p>날짜별로 안전교육일지와 작업일보를 통합하여 조회합니다.</p>
     * <p>페이지네이션을 지원하며, 최신 날짜순으로 정렬됩니다.</p>
     * <p>연도/월 필터링 옵션을 지원합니다.</p>
     *
     * @param siteId 현장 ID
     * @param year 연도 (선택)
     * @param month 월 (선택, 1-12)
     * @param pageable 페이지네이션 정보
     * @return 날짜별 안전/작업 문서 목록
     */
    public SafetyWorkDocumentListResponse getSafetyWorkDocuments(
            Long siteId,
            Integer year,
            Integer month,
            Pageable pageable) {
        log.info("안전/작업 문서 목록 조회 - siteId: {}, year: {}, month: {}, page: {}, size: {}",
                siteId, year, month, pageable.getPageNumber(), pageable.getPageSize());

        // 현장 존재 여부 확인
        if (!siteRepository.existsById(siteId)) {
            throw new BusinessException(SiteErrorCode.SITE_NOT_FOUND);
        }

        // 두 테이블에서 전체 날짜 목록 조회 (페이지네이션 없이)
        List<LocalDate> safetyDates;
        List<LocalDate> workReportDates;

        if (year != null && month != null) {
            // 연도/월 필터링 - LocalDateTime 범위로 변환
            java.time.LocalDateTime startDate = java.time.LocalDateTime.of(year, month, 1, 0, 0);
            java.time.LocalDateTime endDate = startDate.plusMonths(1);
            safetyDates = safetyEducationLogRepository.findDistinctDatesBySiteIdAndYearMonth(siteId, startDate, endDate);
            workReportDates = workReportRepository.findDistinctDatesBySiteIdAndYearMonth(siteId, startDate, endDate);
        } else {
            // 전체 조회
            safetyDates = safetyEducationLogRepository.findDistinctDatesBySiteId(siteId);
            workReportDates = workReportRepository.findDistinctDatesBySiteId(siteId);
        }

        // 날짜 병합 및 정렬
        Set<LocalDate> allDates = new HashSet<>();
        allDates.addAll(safetyDates);
        allDates.addAll(workReportDates);

        List<LocalDate> sortedDates = allDates.stream()
                .sorted((d1, d2) -> d2.compareTo(d1))  // 내림차순
                .collect(Collectors.toList());

        // 서비스에서 페이지네이션 적용
        int start = (int) pageable.getOffset();
        int end = Math.min(start + pageable.getPageSize(), sortedDates.size());

        List<LocalDate> pagedDates = start < sortedDates.size()
                ? sortedDates.subList(start, end)
                : List.of();

        // 각 날짜별 문서 정보 조회
        List<SafetyWorkDocumentDto> documents = pagedDates.stream()
                .map(date -> buildDocumentDto(siteId, date))
                .collect(Collectors.toList());

        // 전체 카운트는 병합된 날짜 수
        long totalElements = sortedDates.size();

        Page<SafetyWorkDocumentDto> page = new PageImpl<>(documents, pageable, totalElements);

        log.info("안전/작업 문서 목록 조회 완료 - siteId: {}, totalDates: {}", siteId, totalElements);

        return SafetyWorkDocumentListResponse.from(page);
    }

    private static final Pageable SINGLE_RESULT = PageRequest.of(0, 1);

    /**
     * 특정 날짜의 안전/작업 문서 DTO 생성
     */
    private SafetyWorkDocumentDto buildDocumentDto(Long siteId, LocalDate date) {
        // 해당 날짜의 안전교육일지 조회 (가장 최근 것 1개)
        SafetyWorkDocumentDto.SafetyEducationLogSummary safetySummary =
                safetyEducationLogRepository.findBySiteIdAndDate(siteId, date, SINGLE_RESULT)
                        .stream()
                        .findFirst()
                        .map(safetyLog -> SafetyWorkDocumentDto.SafetyEducationLogSummary.builder()
                                .logId(safetyLog.getId())
                                .status(safetyLog.getStatus())
                                .educationSubject(safetyLog.getEducationSubject())
                                .build())
                        .orElse(null);

        // 해당 날짜의 작업일보 조회 (가장 최근 것 1개)
        SafetyWorkDocumentDto.WorkReportSummary workSummary =
                workReportRepository.findBySiteIdAndDate(siteId, date, SINGLE_RESULT)
                        .stream()
                        .findFirst()
                        .map(workReport -> SafetyWorkDocumentDto.WorkReportSummary.builder()
                                .workReportId(workReport.getId())
                                .sequence(1)
                                .build())
                        .orElse(null);

        return SafetyWorkDocumentDto.builder()
                .date(date)
                .safetyEducationLog(safetySummary)
                .workReport(workSummary)
                .build();
    }
}
