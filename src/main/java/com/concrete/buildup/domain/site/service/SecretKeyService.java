package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.util.SecretKeyGenerator;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 시크릿키 생성 서비스
 *
 * 현장 등록 시 중복되지 않는 시크릿키를 생성합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class SecretKeyService {

    private static final int MAX_RETRY = 10;

    private final SiteRepository siteRepository;

    /**
     * 유니크한 시크릿키 쌍 생성
     *
     * @param corpName 기업명
     * @param siteId 현장 ID
     * @return 생성된 시크릿키 쌍
     * @throws IllegalStateException 최대 재시도 횟수 초과 시
     */
    public SecretKeyPair generateUniqueSecretKeys(String corpName, Long siteId) {
        int attempt = 0;

        while (attempt < MAX_RETRY) {
            attempt++;

            // 시크릿키 생성
            String managerSecretKey = SecretKeyGenerator.generateManagerSecretKey(corpName, siteId);
            String employeeSecretKey = SecretKeyGenerator.generateEmployeeSecretKey(corpName, siteId);

            // 중복 확인
            boolean managerKeyExists = siteRepository.existsByManagerSecretKey(managerSecretKey);
            boolean employeeKeyExists = siteRepository.existsByEmployeeSecretKey(employeeSecretKey);

            if (!managerKeyExists && !employeeKeyExists) {
                log.info("유니크한 시크릿키 생성 성공 - 시도 횟수: {}, siteId: {}", attempt, siteId);
                return new SecretKeyPair(managerSecretKey, employeeSecretKey);
            }

            log.warn("시크릿키 중복 발생 - 시도 횟수: {}, siteId: {}, managerKeyExists: {}, employeeKeyExists: {}",
                attempt, siteId, managerKeyExists, employeeKeyExists);
        }

        log.error("유니크한 시크릿키 생성 실패 - 최대 재시도 횟수 초과, siteId: {}", siteId);
        throw new IllegalStateException("유니크한 시크릿키 생성에 실패했습니다. 최대 재시도 횟수를 초과했습니다.");
    }

    /**
     * 시크릿키 쌍 DTO
     */
    @Getter
    @AllArgsConstructor
    public static class SecretKeyPair {
        private final String managerSecretKey;
        private final String employeeSecretKey;
    }
}
