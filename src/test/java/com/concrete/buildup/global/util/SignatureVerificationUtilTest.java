package com.concrete.buildup.global.util;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("SignatureVerificationUtil 테스트")
class SignatureVerificationUtilTest {

    @Nested
    @DisplayName("isValidHashFormat 메서드")
    class IsValidHashFormatTest {

        @Test
        @DisplayName("정상적인 SHA-256 해시 형식 - 소문자")
        void validHashFormat_Lowercase() {
            // Given
            String validHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(validHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("정상적인 SHA-256 해시 형식 - 대문자")
        void validHashFormat_Uppercase() {
            // Given
            String validHash = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(validHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("정상적인 SHA-256 해시 형식 - 대소문자 혼합")
        void validHashFormat_MixedCase() {
            // Given
            String validHash = "E3b0C44298Fc1c149aFbF4c8996fB92427aE41e4649B934cA495991B7852b855";

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(validHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("정상적인 SHA-256 해시 형식 - 숫자만")
        void validHashFormat_NumbersOnly() {
            // Given
            String validHash = "1234567890123456789012345678901234567890123456789012345678901234";

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(validHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("null 입력 시 false 반환")
        void invalidHashFormat_Null() {
            // Given
            String nullHash = null;

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(nullHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("빈 문자열 입력 시 false 반환")
        void invalidHashFormat_Empty() {
            // Given
            String emptyHash = "";

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(emptyHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("너무 짧은 해시 (63자) - false 반환")
        void invalidHashFormat_TooShort() {
            // Given
            String shortHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85"; // 63자

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(shortHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("너무 긴 해시 (65자) - false 반환")
        void invalidHashFormat_TooLong() {
            // Given
            String longHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b8555"; // 65자

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(longHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("16진수가 아닌 문자 포함 (g) - false 반환")
        void invalidHashFormat_InvalidCharacter_G() {
            // Given
            String invalidHash = "g3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // 'g' 포함

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(invalidHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("16진수가 아닌 문자 포함 (특수문자) - false 반환")
        void invalidHashFormat_SpecialCharacter() {
            // Given
            String invalidHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85!"; // '!' 포함

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(invalidHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("공백 포함 - false 반환")
        void invalidHashFormat_WithSpace() {
            // Given
            String invalidHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85 "; // 끝에 공백

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(invalidHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("하이픈 포함 - false 반환")
        void invalidHashFormat_WithHyphen() {
            // Given
            String invalidHash = "e3b0c442-98fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // 하이픈 포함

            // When
            boolean result = SignatureVerificationUtil.isValidHashFormat(invalidHash);

            // Then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("calculateSHA256 메서드")
    class CalculateSHA256Test {

        @Test
        @DisplayName("빈 데이터의 SHA-256 해시 계산 - 표준 해시값 반환")
        void calculateSHA256_EmptyData() {
            // Given
            byte[] emptyData = new byte[0];
            InputStream inputStream = new ByteArrayInputStream(emptyData);

            // 빈 문자열의 SHA-256 해시 (표준값)
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // When
            String actualHash = SignatureVerificationUtil.calculateSHA256(inputStream);

            // Then
            assertThat(actualHash).isEqualTo(expectedHash);
            assertThat(actualHash).hasSize(64);
        }

        @Test
        @DisplayName("텍스트 데이터의 SHA-256 해시 계산")
        void calculateSHA256_TextData() {
            // Given
            String text = "Hello, World!";
            byte[] data = text.getBytes(StandardCharsets.UTF_8);
            InputStream inputStream = new ByteArrayInputStream(data);

            // "Hello, World!"의 SHA-256 해시 (표준값)
            String expectedHash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

            // When
            String actualHash = SignatureVerificationUtil.calculateSHA256(inputStream);

            // Then
            assertThat(actualHash).isEqualTo(expectedHash);
            assertThat(actualHash).hasSize(64);
        }

        @Test
        @DisplayName("이미지 바이너리 데이터의 SHA-256 해시 계산")
        void calculateSHA256_BinaryData() {
            // Given - 간단한 PNG 헤더 시그니처
            byte[] pngSignature = new byte[]{
                    (byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
            };
            InputStream inputStream = new ByteArrayInputStream(pngSignature);

            // When
            String actualHash = SignatureVerificationUtil.calculateSHA256(inputStream);

            // Then
            assertThat(actualHash).isNotNull();
            assertThat(actualHash).hasSize(64);
            assertThat(actualHash).matches("^[0-9a-f]{64}$"); // 소문자 16진수
        }

        @Test
        @DisplayName("동일한 데이터는 동일한 해시 생성")
        void calculateSHA256_SameData_SameHash() {
            // Given
            String text = "Test Data";
            byte[] data = text.getBytes(StandardCharsets.UTF_8);

            InputStream inputStream1 = new ByteArrayInputStream(data);
            InputStream inputStream2 = new ByteArrayInputStream(data);

            // When
            String hash1 = SignatureVerificationUtil.calculateSHA256(inputStream1);
            String hash2 = SignatureVerificationUtil.calculateSHA256(inputStream2);

            // Then
            assertThat(hash1).isEqualTo(hash2);
        }

        @Test
        @DisplayName("다른 데이터는 다른 해시 생성")
        void calculateSHA256_DifferentData_DifferentHash() {
            // Given
            String text1 = "Data1";
            String text2 = "Data2";

            InputStream inputStream1 = new ByteArrayInputStream(text1.getBytes(StandardCharsets.UTF_8));
            InputStream inputStream2 = new ByteArrayInputStream(text2.getBytes(StandardCharsets.UTF_8));

            // When
            String hash1 = SignatureVerificationUtil.calculateSHA256(inputStream1);
            String hash2 = SignatureVerificationUtil.calculateSHA256(inputStream2);

            // Then
            assertThat(hash1).isNotEqualTo(hash2);
        }

        @Test
        @DisplayName("1바이트 차이만 있어도 완전히 다른 해시 생성 (Avalanche Effect)")
        void calculateSHA256_OneByteChange_CompletelyDifferentHash() {
            // Given
            byte[] data1 = "signature".getBytes(StandardCharsets.UTF_8);
            byte[] data2 = "Signature".getBytes(StandardCharsets.UTF_8); // 첫 글자만 대문자

            InputStream inputStream1 = new ByteArrayInputStream(data1);
            InputStream inputStream2 = new ByteArrayInputStream(data2);

            // When
            String hash1 = SignatureVerificationUtil.calculateSHA256(inputStream1);
            String hash2 = SignatureVerificationUtil.calculateSHA256(inputStream2);

            // Then
            assertThat(hash1).isNotEqualTo(hash2);

            // 해시 차이 비율 확인 (Avalanche Effect로 인해 거의 50% 차이)
            int differentChars = 0;
            for (int i = 0; i < 64; i++) {
                if (hash1.charAt(i) != hash2.charAt(i)) {
                    differentChars++;
                }
            }
            // SHA-256의 Avalanche Effect로 인해 최소 30% 이상은 달라야 함
            assertThat(differentChars).isGreaterThan(19); // 64 * 0.3 ≈ 19
        }

        @Test
        @DisplayName("큰 데이터의 SHA-256 해시 계산 (버퍼 처리 검증)")
        void calculateSHA256_LargeData() {
            // Given - 10KB 데이터 (버퍼 크기 8KB 초과)
            byte[] largeData = new byte[10240];
            for (int i = 0; i < largeData.length; i++) {
                largeData[i] = (byte) (i % 256);
            }
            InputStream inputStream = new ByteArrayInputStream(largeData);

            // When
            String actualHash = SignatureVerificationUtil.calculateSHA256(inputStream);

            // Then
            assertThat(actualHash).isNotNull();
            assertThat(actualHash).hasSize(64);
            assertThat(actualHash).matches("^[0-9a-f]{64}$");
        }

        @Test
        @DisplayName("null InputStream 입력 시 IllegalArgumentException 발생")
        void calculateSHA256_NullInputStream_ThrowsException() {
            // Given
            InputStream nullStream = null;

            // When & Then
            assertThatThrownBy(() -> SignatureVerificationUtil.calculateSHA256(nullStream))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("null");
        }

        @Test
        @DisplayName("반환된 해시는 유효한 형식이어야 함")
        void calculateSHA256_ReturnedHashIsValid() {
            // Given
            String text = "Test";
            InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

            // When
            String hash = SignatureVerificationUtil.calculateSHA256(inputStream);

            // Then
            assertThat(SignatureVerificationUtil.isValidHashFormat(hash)).isTrue();
        }
    }

    @Nested
    @DisplayName("verifySignatureHash 메서드")
    class VerifySignatureHashTest {

        @Test
        @DisplayName("동일한 해시 - 검증 성공")
        void verifySignatureHash_SameHash_ReturnsTrue() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String actualHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("대소문자만 다른 해시 - 검증 성공 (대소문자 무시)")
        void verifySignatureHash_DifferentCase_ReturnsTrue() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"; // 소문자
            String actualHash = "E3B0C44298FC1C149AFBF4C8996FB92427AE41E4649B934CA495991B7852B855";   // 대문자

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("대소문자 혼합 해시 - 검증 성공")
        void verifySignatureHash_MixedCase_ReturnsTrue() {
            // Given
            String expectedHash = "E3b0C44298Fc1c149aFbF4c8996fB92427aE41e4649B934cA495991B7852b855";
            String actualHash = "e3B0c44298fC1C149AfBf4C8996Fb92427Ae41E4649b934Ca495991b7852B855";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("다른 해시 - 검증 실패")
        void verifySignatureHash_DifferentHash_ReturnsFalse() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String actualHash = "0000000000000000000000000000000000000000000000000000000000000000";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("1자만 다른 해시 - 검증 실패")
        void verifySignatureHash_OneCharDifferent_ReturnsFalse() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String actualHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b856"; // 마지막 글자만 다름

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("expected가 null - 검증 실패")
        void verifySignatureHash_ExpectedNull_ReturnsFalse() {
            // Given
            String expectedHash = null;
            String actualHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("actual이 null - 검증 실패")
        void verifySignatureHash_ActualNull_ReturnsFalse() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String actualHash = null;

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("둘 다 null - 검증 실패")
        void verifySignatureHash_BothNull_ReturnsFalse() {
            // Given
            String expectedHash = null;
            String actualHash = null;

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("expected가 빈 문자열 - 검증 실패")
        void verifySignatureHash_ExpectedEmpty_ReturnsFalse() {
            // Given
            String expectedHash = "";
            String actualHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("actual이 잘못된 형식 - 검증 실패")
        void verifySignatureHash_ActualInvalidFormat_ReturnsFalse() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String actualHash = "not-a-valid-hash";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("expected가 너무 짧음 - 검증 실패")
        void verifySignatureHash_ExpectedTooShort_ReturnsFalse() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b85"; // 63자
            String actualHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("actual이 너무 긺 - 검증 실패")
        void verifySignatureHash_ActualTooLong_ReturnsFalse() {
            // Given
            String expectedHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855";
            String actualHash = "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b8555"; // 65자

            // When
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("실제 사용 시나리오 - 텍스트 데이터 검증")
        void verifySignatureHash_RealScenario_TextData() {
            // Given - "Hello, World!"의 해시
            String text = "Hello, World!";
            InputStream inputStream = new ByteArrayInputStream(text.getBytes(StandardCharsets.UTF_8));

            String expectedHash = "dffd6021bb2bd5b0af676290809ec3a53191dd81c7f70a4b28688a362182986f";

            // When
            String actualHash = SignatureVerificationUtil.calculateSHA256(inputStream);
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("실제 사용 시나리오 - 변조된 데이터 검출")
        void verifySignatureHash_RealScenario_TamperedData() {
            // Given
            String originalText = "Original Signature";
            String tamperedText = "Tampered Signature";

            InputStream originalStream = new ByteArrayInputStream(originalText.getBytes(StandardCharsets.UTF_8));
            InputStream tamperedStream = new ByteArrayInputStream(tamperedText.getBytes(StandardCharsets.UTF_8));

            // When - 원본 데이터의 해시 계산 (DB에 저장되었다고 가정)
            String expectedHash = SignatureVerificationUtil.calculateSHA256(originalStream);

            // 변조된 데이터의 해시 계산
            String actualHash = SignatureVerificationUtil.calculateSHA256(tamperedStream);

            // 검증
            boolean result = SignatureVerificationUtil.verifySignatureHash(expectedHash, actualHash);

            // Then - 검증 실패해야 함 (변조 검출)
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("전체 플로우 통합 테스트")
    class IntegrationTest {

        @Test
        @DisplayName("서명 업로드 → 해시 계산 → 저장 → 검증 전체 플로우")
        void fullFlow_SignatureUploadAndVerification() {
            // Given - 클라이언트가 서명 이미지 업로드
            byte[] signatureImageData = createTestSignatureImage();
            InputStream uploadedImage = new ByteArrayInputStream(signatureImageData);

            // When - Step 1: 서버가 업로드된 이미지의 해시 계산
            String calculatedHash = SignatureVerificationUtil.calculateSHA256(uploadedImage);

            // Then - Step 1 검증
            assertThat(calculatedHash).isNotNull();
            assertThat(calculatedHash).hasSize(64);
            assertThat(SignatureVerificationUtil.isValidHashFormat(calculatedHash)).isTrue();

            // When - Step 2: DB에 저장된 해시를 다시 조회했다고 가정
            String savedHash = calculatedHash; // DB에 저장했다가 다시 조회

            // When - Step 3: 검증 시점에 S3에서 이미지를 다시 다운로드하여 해시 계산
            InputStream downloadedImage = new ByteArrayInputStream(signatureImageData);
            String verificationHash = SignatureVerificationUtil.calculateSHA256(downloadedImage);

            // When - Step 4: 두 해시 비교
            boolean isValid = SignatureVerificationUtil.verifySignatureHash(savedHash, verificationHash);

            // Then - 검증 성공
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("전체 플로우 - 이미지 변조 검출")
        void fullFlow_TamperedImageDetection() {
            // Given - 원본 서명 이미지 업로드
            byte[] originalImageData = "Original Signature Image Data".getBytes(StandardCharsets.UTF_8);
            InputStream originalImage = new ByteArrayInputStream(originalImageData);

            // When - Step 1: 원본 이미지의 해시 계산 및 저장
            String savedHash = SignatureVerificationUtil.calculateSHA256(originalImage);

            // Given - 이미지가 변조됨 (S3에서 파일이 변경되거나 손상됨)
            byte[] tamperedImageData = "Tampered Signature Image Data".getBytes(StandardCharsets.UTF_8);
            InputStream tamperedImage = new ByteArrayInputStream(tamperedImageData);

            // When - Step 2: 변조된 이미지의 해시 계산
            String tamperedHash = SignatureVerificationUtil.calculateSHA256(tamperedImage);

            // When - Step 3: 저장된 해시와 변조된 이미지의 해시 비교
            boolean isValid = SignatureVerificationUtil.verifySignatureHash(savedHash, tamperedHash);

            // Then - 검증 실패 (변조 검출)
            assertThat(isValid).isFalse();
        }

        @Test
        @DisplayName("전체 플로우 - 대소문자 다른 해시로 검증 성공")
        void fullFlow_CaseInsensitiveVerification() {
            // Given
            byte[] imageData = "Test Signature".getBytes(StandardCharsets.UTF_8);
            InputStream image1 = new ByteArrayInputStream(imageData);
            InputStream image2 = new ByteArrayInputStream(imageData);

            // When - 두 번 해시 계산 (소문자로 반환)
            String hash1 = SignatureVerificationUtil.calculateSHA256(image1);
            String hash2 = SignatureVerificationUtil.calculateSHA256(image2);

            // 임의로 대문자로 변환 (클라이언트가 대문자로 보냈다고 가정)
            String uppercaseHash = hash1.toUpperCase();

            // When - 대소문자가 다른 해시로 검증
            boolean isValid = SignatureVerificationUtil.verifySignatureHash(uppercaseHash, hash2);

            // Then - 검증 성공 (대소문자 무시)
            assertThat(isValid).isTrue();
        }

        @Test
        @DisplayName("전체 플로우 - 복수의 서명 검증")
        void fullFlow_MultipleSignaturesVerification() {
            // Given - 3개의 서로 다른 서명 이미지
            byte[] signature1 = "Employee Signature".getBytes(StandardCharsets.UTF_8);
            byte[] signature2 = "Manager Signature".getBytes(StandardCharsets.UTF_8);
            byte[] signature3 = "Corporation Signature".getBytes(StandardCharsets.UTF_8);

            // When - 각 서명의 해시 계산
            String hash1 = SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(signature1));
            String hash2 = SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(signature2));
            String hash3 = SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(signature3));

            // Then - 모두 유효한 형식
            assertThat(SignatureVerificationUtil.isValidHashFormat(hash1)).isTrue();
            assertThat(SignatureVerificationUtil.isValidHashFormat(hash2)).isTrue();
            assertThat(SignatureVerificationUtil.isValidHashFormat(hash3)).isTrue();

            // Then - 각 해시는 서로 다름
            assertThat(hash1).isNotEqualTo(hash2);
            assertThat(hash2).isNotEqualTo(hash3);
            assertThat(hash1).isNotEqualTo(hash3);

            // When - 각 서명 재검증
            boolean valid1 = SignatureVerificationUtil.verifySignatureHash(
                    hash1,
                    SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(signature1))
            );
            boolean valid2 = SignatureVerificationUtil.verifySignatureHash(
                    hash2,
                    SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(signature2))
            );
            boolean valid3 = SignatureVerificationUtil.verifySignatureHash(
                    hash3,
                    SignatureVerificationUtil.calculateSHA256(new ByteArrayInputStream(signature3))
            );

            // Then - 모든 검증 성공
            assertThat(valid1).isTrue();
            assertThat(valid2).isTrue();
            assertThat(valid3).isTrue();
        }

        @Test
        @DisplayName("전체 플로우 - 잘못된 해시 형식 조기 검출")
        void fullFlow_InvalidHashFormatEarlyDetection() {
            // Given - 클라이언트가 잘못된 형식의 해시를 전송
            String invalidHash = "not-a-valid-hash-format";

            // When - 형식 검증
            boolean isValidFormat = SignatureVerificationUtil.isValidHashFormat(invalidHash);

            // Then - 형식 검증 실패
            assertThat(isValidFormat).isFalse();

            // When - 검증 시도 (실제 이미지 다운로드 전에 조기 검출)
            byte[] imageData = "Some Image".getBytes(StandardCharsets.UTF_8);
            String actualHash = SignatureVerificationUtil.calculateSHA256(
                    new ByteArrayInputStream(imageData)
            );

            boolean isValid = SignatureVerificationUtil.verifySignatureHash(invalidHash, actualHash);

            // Then - 검증 실패 (불필요한 처리 방지)
            assertThat(isValid).isFalse();
        }

        /**
         * 테스트용 서명 이미지 데이터 생성
         */
        private byte[] createTestSignatureImage() {
            // 실제로는 PNG/JPEG 등의 이미지 데이터
            // 테스트에서는 간단한 바이트 배열 사용
            return "Test Signature Image - Employee John Doe - 2024-01-01".getBytes(StandardCharsets.UTF_8);
        }
    }
}