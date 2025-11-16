package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.SecretKeyGenerationException;
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
     * @throws SecretKeyGenerationException 최대 재시도 횟수 초과 시
     */
    public SecretKeyPair generateUniqueSecretKeys(String corpName, Long siteId) {
        int attempt = 0;
        String managerSecretKey = null;
        String employeeSecretKey = null;
        boolean regenerateManager = true;
        boolean regenerateEmployee = true;

        while (attempt < MAX_RETRY) {
            attempt++;

            // 첫 번째 시도 또는 충돌 발생한 키만 재생성
            if (regenerateManager) {
                managerSecretKey = SecretKeyGenerator.generateManagerSecretKey(corpName, siteId);
            }
            if (regenerateEmployee) {
                employeeSecretKey = SecretKeyGenerator.generateEmployeeSecretKey(corpName, siteId);
            }

            // 재생성된 키만 중복 확인
            boolean managerKeyExists = regenerateManager && siteRepository.existsByManagerSecretKey(managerSecretKey);
            boolean employeeKeyExists = regenerateEmployee && siteRepository.existsByEmployeeSecretKey(employeeSecretKey);

            // 두 키 모두 유니크하면 성공
            if (!managerKeyExists && !employeeKeyExists) {
                log.info("유니크한 시크릿키 생성 성공 - 시도 횟수: {}, siteId: {}", attempt, siteId);
                return new SecretKeyPair(managerSecretKey, employeeSecretKey);
            }

            // 다음 반복에서 재생성할 키 결정
            regenerateManager = managerKeyExists;
            regenerateEmployee = employeeKeyExists;

            // 재생성할 키에 대한 로그 출력
            String regenerationTarget;
            if (managerKeyExists && employeeKeyExists) {
                regenerationTarget = "관리자키, 근로자키";
            } else if (managerKeyExists) {
                regenerationTarget = "관리자키";
            } else {
                regenerationTarget = "근로자키";
            }

            log.warn("시크릿키 중복 발생 - 시도 횟수: {}, siteId: {}, 재생성 대상: {}",
                    attempt, siteId, regenerationTarget);
        }

        log.error("유니크한 시크릿키 생성 실패 - 최대 재시도 횟수 초과, siteId: {}", siteId);
        throw new SecretKeyGenerationException("유니크한 시크릿키 생성에 실패했습니다. 최대 재시도 횟수를 초과했습니다.");
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
