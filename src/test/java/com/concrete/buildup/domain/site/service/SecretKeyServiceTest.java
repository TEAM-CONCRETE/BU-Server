package com.concrete.buildup.domain.site.service;

import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.global.exception.SecretKeyGenerationException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * SecretKeyService 단위 테스트
 *
 * @author Build-Up Team
 * @since 1.0
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("SecretKeyService 테스트")
class SecretKeyServiceTest {

    @Mock
    private SiteRepository siteRepository;

    @InjectMocks
    private SecretKeyService secretKeyService;

    @Test
    @DisplayName("유니크한 시크릿키 생성 성공 - 중복 없음")
    void generateUniqueSecretKeys_Success_NoDuplication() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;
        given(siteRepository.existsByManagerSecretKey(anyString())).willReturn(false);
        given(siteRepository.existsByEmployeeSecretKey(anyString())).willReturn(false);

        // when
        SecretKeyService.SecretKeyPair result = secretKeyService.generateUniqueSecretKeys(corpName, siteId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getManagerSecretKey()).isNotNull();
        assertThat(result.getEmployeeSecretKey()).isNotNull();
        assertThat(result.getManagerSecretKey()).matches("^[A-Z]{4}-\\d+-[A-Z]{3}-\\d{4}$");
        assertThat(result.getEmployeeSecretKey()).matches("^[A-Z]{4}-\\d+-[A-Z]{3}-\\d{4}$");
        assertThat(result.getManagerSecretKey()).isNotEqualTo(result.getEmployeeSecretKey());

        verify(siteRepository, times(1)).existsByManagerSecretKey(anyString());
        verify(siteRepository, times(1)).existsByEmployeeSecretKey(anyString());
    }

    @Test
    @DisplayName("유니크한 시크릿키 생성 성공 - 첫 시도에서 중복, 재시도 성공")
    void generateUniqueSecretKeys_Success_AfterRetry() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;

        // 첫 번째 시도는 중복, 두 번째 시도는 성공
        given(siteRepository.existsByManagerSecretKey(anyString()))
                .willReturn(true)   // 첫 번째 시도: 중복
                .willReturn(false); // 두 번째 시도: 성공
        given(siteRepository.existsByEmployeeSecretKey(anyString()))
                .willReturn(false);

        // when
        SecretKeyService.SecretKeyPair result = secretKeyService.generateUniqueSecretKeys(corpName, siteId);

        // then
        assertThat(result).isNotNull();
        assertThat(result.getManagerSecretKey()).isNotNull();
        assertThat(result.getEmployeeSecretKey()).isNotNull();

        verify(siteRepository, times(2)).existsByManagerSecretKey(anyString());
        verify(siteRepository, times(1)).existsByEmployeeSecretKey(anyString());
    }

    @Test
    @DisplayName("유니크한 시크릿키 생성 실패 - 최대 재시도 횟수 초과")
    void generateUniqueSecretKeys_Fail_MaxRetryExceeded() {
        // given
        String corpName = "CONCRETE";
        Long siteId = 1L;

        // 항상 중복으로 반환
        given(siteRepository.existsByManagerSecretKey(anyString())).willReturn(true);
        given(siteRepository.existsByEmployeeSecretKey(anyString())).willReturn(true);

        // when & then
        assertThatThrownBy(() -> secretKeyService.generateUniqueSecretKeys(corpName, siteId))
                .isInstanceOf(SecretKeyGenerationException.class)
                .hasMessageContaining("유니크한 시크릿키 생성에 실패했습니다");

        verify(siteRepository, times(10)).existsByManagerSecretKey(anyString());
        verify(siteRepository, times(10)).existsByEmployeeSecretKey(anyString());
    }

    @Test
    @DisplayName("SecretKeyPair 생성 검증")
    void secretKeyPair_ShouldHaveCorrectValues() {
        // given
        String managerKey = "CONC-1-ABC-1234";
        String employeeKey = "CONC-1-XYZ-5678";

        // when
        SecretKeyService.SecretKeyPair pair = new SecretKeyService.SecretKeyPair(managerKey, employeeKey);

        // then
        assertThat(pair.getManagerSecretKey()).isEqualTo(managerKey);
        assertThat(pair.getEmployeeSecretKey()).isEqualTo(employeeKey);
    }

    @Test
    @DisplayName("시크릿키 포맷 검증 - 기업명 prefix 포함")
    void secretKey_ShouldContainCorpNamePrefix() {
        // given
        String corpName = "BUILD UP";
        Long siteId = 5L;
        given(siteRepository.existsByManagerSecretKey(anyString())).willReturn(false);
        given(siteRepository.existsByEmployeeSecretKey(anyString())).willReturn(false);

        // when
        SecretKeyService.SecretKeyPair result = secretKeyService.generateUniqueSecretKeys(corpName, siteId);

        // then
        assertThat(result.getManagerSecretKey()).startsWith("BUIL-5-");
        assertThat(result.getEmployeeSecretKey()).startsWith("BUIL-5-");
    }
}
