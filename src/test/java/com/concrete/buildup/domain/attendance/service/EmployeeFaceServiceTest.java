package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

/**
 * EmployeeFaceService 단위 테스트
 *
 * 핵심 기능만 테스트:
 * 1. S3 URL 구성 및 저장
 * 2. User/Employee 존재 여부 검증
 */
@ExtendWith(MockitoExtension.class)
class EmployeeFaceServiceTest {

    private EmployeeFaceService employeeFaceService;

    @Mock
    private UserRepository userRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private S3Service s3Service;

    @BeforeEach
    void setUp() {
        employeeFaceService = new EmployeeFaceService(
            userRepository,
            employeeRepository,
            java.util.Optional.of(s3Service)
        );
    }

    @Test
    @DisplayName("얼굴 이미지 등록 성공 - S3 URL 구성 및 저장")
    void registerEmployeeFaceImage_Success() {
        // Given
        String userId = "test123";
        String uploadId = "profile/1/face.jpg";
        String bucketName = "test-bucket";

        ReflectionTestUtils.setField(employeeFaceService, "bucketName", bucketName);

        User mockUser = User.builder()
                .userId(userId)
                .password("password")
                .phone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        Employee mockEmployee = Employee.builder()
                .user(mockUser)
                .empName("테스트")
                .build();
        ReflectionTestUtils.setField(mockEmployee, "id", 1L);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(mockUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(mockEmployee));
        when(s3Service.doesObjectExist(uploadId)).thenReturn(true);
        when(employeeRepository.save(any(Employee.class))).thenReturn(mockEmployee);

        // When
        String result = employeeFaceService.registerEmployeeFaceImage(userId, uploadId);

        // Then
        String expectedUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, uploadId);
        assertThat(result).isEqualTo(expectedUrl);

        verify(userRepository, times(1)).findByUserId(userId);
        verify(employeeRepository, times(1)).findByUserId(1L);
        verify(s3Service, times(1)).doesObjectExist(uploadId);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("얼굴 이미지 등록 실패 - User 없음")
    void registerEmployeeFaceImage_UserNotFound() {
        // Given
        String userId = "nonexistent";
        String uploadId = "profile/999/face.jpg";

        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeFaceService.registerEmployeeFaceImage(userId, uploadId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(userRepository, times(1)).findByUserId(userId);
        verify(employeeRepository, never()).findByUserId(any());
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("얼굴 이미지 등록 실패 - Employee 없음")
    void registerEmployeeFaceImage_EmployeeNotFound() {
        // Given
        String userId = "test123";
        String uploadId = "profile/999/face.jpg";

        User mockUser = User.builder()
                .userId(userId)
                .password("password")
                .phone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(mockUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> employeeFaceService.registerEmployeeFaceImage(userId, uploadId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");

        verify(userRepository, times(1)).findByUserId(userId);
        verify(employeeRepository, times(1)).findByUserId(1L);
        verify(employeeRepository, never()).save(any());
    }

    @Test
    @DisplayName("기존 얼굴 이미지가 있을 때 덮어쓰기")
    void registerEmployeeFaceImage_ReplaceExisting() {
        // Given
        String userId = "test123";
        String uploadId = "profile/1/face.jpg";
        String bucketName = "test-bucket";
        String oldImageUrl = "https://test-bucket.s3.amazonaws.com/profile/1/old-face.jpg";

        ReflectionTestUtils.setField(employeeFaceService, "bucketName", bucketName);

        User mockUser = User.builder()
                .userId(userId)
                .password("password")
                .phone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        Employee mockEmployee = Employee.builder()
                .user(mockUser)
                .empName("테스트")
                .profileImageUrl(oldImageUrl)
                .build();
        ReflectionTestUtils.setField(mockEmployee, "id", 1L);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(mockUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(mockEmployee));
        when(s3Service.doesObjectExist(uploadId)).thenReturn(true);
        when(employeeRepository.save(any(Employee.class))).thenReturn(mockEmployee);

        // When
        String result = employeeFaceService.registerEmployeeFaceImage(userId, uploadId);

        // Then
        String expectedUrl = String.format("https://%s.s3.amazonaws.com/%s", bucketName, uploadId);
        assertThat(result).isEqualTo(expectedUrl);
        assertThat(result).isNotEqualTo(oldImageUrl);

        verify(s3Service, times(1)).doesObjectExist(uploadId);
        verify(employeeRepository, times(1)).save(any(Employee.class));
    }

    @Test
    @DisplayName("얼굴 이미지 등록 실패 - S3에 파일이 존재하지 않음")
    void registerEmployeeFaceImage_S3ObjectNotFound() {
        // Given
        String userId = "test123";
        String uploadId = "profile/1/face.jpg";

        User mockUser = User.builder()
                .userId(userId)
                .password("password")
                .phone("010-1234-5678")
                .build();
        ReflectionTestUtils.setField(mockUser, "id", 1L);

        Employee mockEmployee = Employee.builder()
                .user(mockUser)
                .empName("테스트")
                .build();
        ReflectionTestUtils.setField(mockEmployee, "id", 1L);

        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(mockUser));
        when(employeeRepository.findByUserId(1L)).thenReturn(Optional.of(mockEmployee));
        when(s3Service.doesObjectExist(uploadId)).thenReturn(false);

        // When & Then
        assertThatThrownBy(() -> employeeFaceService.registerEmployeeFaceImage(userId, uploadId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("업로드된 이미지 파일이 S3에 존재하지 않습니다");

        verify(userRepository, times(1)).findByUserId(userId);
        verify(employeeRepository, times(1)).findByUserId(1L);
        verify(s3Service, times(1)).doesObjectExist(uploadId);
        verify(employeeRepository, never()).save(any());
    }
}
