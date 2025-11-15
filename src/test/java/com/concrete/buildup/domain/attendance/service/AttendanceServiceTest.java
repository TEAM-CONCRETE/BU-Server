package com.concrete.buildup.domain.attendance.service;

import com.concrete.buildup.domain.attendance.dto.AttendanceVerificationRequestDto;
import com.concrete.buildup.domain.attendance.dto.AttendanceVerificationResponseDto;
import com.concrete.buildup.domain.attendance.dto.FaceSimilarityResponseDto;
import com.concrete.buildup.domain.attendance.entity.Attendance;
import com.concrete.buildup.domain.attendance.enums.AttendanceType;
import com.concrete.buildup.domain.attendance.exception.DuplicateAttendanceException;
import com.concrete.buildup.domain.attendance.exception.FaceImageNotRegisteredException;
import com.concrete.buildup.domain.attendance.repository.AttendanceRepository;
import com.concrete.buildup.domain.auth.entity.Employee;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.EmployeeRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.contract.entity.Contract;
import com.concrete.buildup.domain.contract.enums.EmpType;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * AttendanceService 단위 테스트
 *
 * 핵심 비즈니스 로직 테스트:
 * 1. 얼굴 인식 기반 출퇴근 검증 및 기록
 * 2. 출퇴근 유형 자동 판단
 * 3. 중복 체크인/체크아웃 방지
 * 4. 얼굴 미등록 사원 처리
 */
@ExtendWith(MockitoExtension.class)
class AttendanceServiceTest {

    @InjectMocks
    private AttendanceService attendanceService;

    @Mock
    private AttendanceRepository attendanceRepository;

    @Mock
    private EmployeeRepository employeeRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private FaceSimilarityClient faceSimilarityClient;

    @Mock
    private S3Service s3Service;

    @Mock
    private com.concrete.buildup.domain.contract.repository.ContractRepository contractRepository;

    private User mockManagerUser;
    private Manager mockManager;
    private Site mockSite;
    private User mockEmployeeUser;
    private Employee mockEmployee;
    private Contract mockContract;
    private String bucketName = "test-bucket";

    @BeforeEach
    void setUp() {
        // SecurityContext 모킹
        SecurityContext securityContext = mock(SecurityContext.class);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
            "manager123",
            null,
            Collections.singletonList(new SimpleGrantedAuthority("ROLE_MANAGER"))
        );
        when(securityContext.getAuthentication()).thenReturn(authentication);
        SecurityContextHolder.setContext(securityContext);

        // 관리자 User 생성
        mockManagerUser = User.builder()
            .userId("manager123")
            .password("password")
            .phone("010-9999-9999")
            .build();
        ReflectionTestUtils.setField(mockManagerUser, "id", 100L);

        // Manager 생성
        mockManager = Manager.builder()
            .user(mockManagerUser)
            .managerName("관리자")
            .build();
        ReflectionTestUtils.setField(mockManager, "id", 10L);

        // Site 생성
        mockSite = Site.builder()
            .siteName("테스트 현장")
            .siteAddress("서울시 강남구")
            .manager(mockManager)
            .build();
        ReflectionTestUtils.setField(mockSite, "id", 1L);

        // 근로자 User 생성
        mockEmployeeUser = User.builder()
            .userId("employee123")
            .password("password")
            .phone("01012345678")
            .build();
        ReflectionTestUtils.setField(mockEmployeeUser, "id", 200L);

        // 근로자 Employee 생성 (얼굴 이미지 등록됨)
        mockEmployee = Employee.builder()
            .user(mockEmployeeUser)
            .empName("테스트 근로자")
            .profileImageUrl("profile/200/face.jpg")
            .build();
        ReflectionTestUtils.setField(mockEmployee, "id", 20L);
        ReflectionTestUtils.setField(mockEmployee, "empType", "DAILY"); // empType 설정

        // Mock Contract (minimal setup for attendance record creation)
        mockContract = mock(Contract.class);
        ReflectionTestUtils.setField(mockContract, "id", 1L);
    }

    @Test
    @DisplayName("출근 기록 성공 - 얼굴 인식 성공")
    void verifyAndRecordAttendance_CheckIn_Success() {
        // Given
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
            .phoneNumber("010-1234-5678")
            .uploadId("attendance/1/20/1234567890.jpg")
            .build();

        // Mock 설정
        when(userRepository.findByUserId("manager123")).thenReturn(Optional.of(mockManagerUser));
        when(siteRepository.findByManagerUserId(100L)).thenReturn(Optional.of(mockSite));
        when(siteRepository.findById(1L)).thenReturn(Optional.of(mockSite)); // createAttendanceRecord에서 필요
        when(employeeRepository.findByPhoneWithUser("01012345678")).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findByEmployeeIdAndSearchDate(eq(20L), any(LocalDate.class)))
            .thenReturn(Collections.emptyList()); // 오늘 기록 없음 → CHECK_IN

        // Contract 조회 모킹
        when(mockContract.getId()).thenReturn(1L);
        when(mockContract.getEmployeeId()).thenReturn(20L); // Must match mockEmployee.id
        when(contractRepository.findActiveContractsByManagerIdAndEmpTypeAndDate(eq(10L), eq(EmpType.DAILY), any(LocalDate.class)))
            .thenReturn(List.of(mockContract));
        when(contractRepository.findByIdWithDetails(1L)).thenReturn(Optional.of(mockContract));

        // S3Service 모킹 - Presigned URL 생성
        when(s3Service.generatePresignedGetUrl("profile/200/face.jpg"))
            .thenReturn("https://test-bucket.s3.amazonaws.com/profile/200/face.jpg");
        when(s3Service.generatePresignedGetUrl(startsWith("attendance/1/20/")))
            .thenReturn("https://test-bucket.s3.amazonaws.com/attendance/1/20/1234567890.jpg");

        FaceSimilarityResponseDto faceApiResponse = new FaceSimilarityResponseDto();
        faceApiResponse.setVerified(true);
        faceApiResponse.setSimilarity(0.95);
        when(faceSimilarityClient.compareFaces(anyString(), anyString())).thenReturn(faceApiResponse);

        Attendance savedAttendance = Attendance.builder()
            .employeeId(20L)
            .siteId(1L)
            .checkInTime(LocalDateTime.now())
            .build();
        ReflectionTestUtils.setField(savedAttendance, "id", 1L);
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(savedAttendance);

        // When
        AttendanceVerificationResponseDto response = attendanceService.verifyAndRecordAttendance(request);

        // Then
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getVerified()).isTrue();
        assertThat(response.getAttendanceType()).isEqualTo(AttendanceType.CHECK_IN);
        assertThat(response.getEmployeeName()).isEqualTo("테스트 근로자");
        assertThat(response.getSimilarityScore()).isEqualTo(0.95);

        verify(attendanceRepository, times(1)).save(any(Attendance.class));
        verify(faceSimilarityClient, times(1)).compareFaces(anyString(), anyString());
    }

    @Test
    @DisplayName("퇴근 기록 성공 - 출근 기록 존재")
    void verifyAndRecordAttendance_CheckOut_Success() {
        // Given
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
            .phoneNumber("010-1234-5678")
            .uploadId("attendance/1/20/1234567890.jpg")
            .build();

        // 오늘 출근 기록 존재
        Attendance existingCheckIn = Attendance.builder()
            .employeeId(20L)
            .siteId(1L)
            .checkInTime(LocalDateTime.now().minusHours(8))
            .build();

        when(userRepository.findByUserId("manager123")).thenReturn(Optional.of(mockManagerUser));
        when(siteRepository.findByManagerUserId(100L)).thenReturn(Optional.of(mockSite));
        when(employeeRepository.findByPhoneWithUser("01012345678")).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findByEmployeeIdAndSearchDate(eq(20L), any(LocalDate.class)))
            .thenReturn(List.of(existingCheckIn)); // 출근 기록 있음 → CHECK_OUT

        // S3Service 모킹
        when(s3Service.generatePresignedGetUrl("profile/200/face.jpg"))
            .thenReturn("https://test-bucket.s3.amazonaws.com/profile/200/face.jpg");
        when(s3Service.generatePresignedGetUrl(startsWith("attendance/1/20/")))
            .thenReturn("https://test-bucket.s3.amazonaws.com/attendance/1/20/1234567890.jpg");

        FaceSimilarityResponseDto faceApiResponse = new FaceSimilarityResponseDto();
        faceApiResponse.setVerified(true);
        faceApiResponse.setSimilarity(0.92);
        when(faceSimilarityClient.compareFaces(anyString(), anyString())).thenReturn(faceApiResponse);

        Attendance updatedAttendance = Attendance.builder()
            .employeeId(20L)
            .siteId(1L)
            .checkInTime(existingCheckIn.getCheckInTime())
            .checkOutTime(LocalDateTime.now())
            .build();
        when(attendanceRepository.save(any(Attendance.class))).thenReturn(updatedAttendance);

        // When
        AttendanceVerificationResponseDto response = attendanceService.verifyAndRecordAttendance(request);

        // Then
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getVerified()).isTrue();
        assertThat(response.getAttendanceType()).isEqualTo(AttendanceType.CHECK_OUT);
        assertThat(response.getSimilarityScore()).isEqualTo(0.92);

        verify(attendanceRepository, times(1)).save(any(Attendance.class));
    }

    @Test
    @DisplayName("얼굴 인식 실패 - 유사도 낮음")
    void verifyAndRecordAttendance_FaceVerificationFailed() {
        // Given
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
            .phoneNumber("010-1234-5678")
            .uploadId("attendance/1/20/1234567890.jpg")
            .build();

        when(userRepository.findByUserId("manager123")).thenReturn(Optional.of(mockManagerUser));
        when(siteRepository.findByManagerUserId(100L)).thenReturn(Optional.of(mockSite));
        when(employeeRepository.findByPhoneWithUser("01012345678")).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findByEmployeeIdAndSearchDate(eq(20L), any(LocalDate.class)))
            .thenReturn(Collections.emptyList());

        // S3Service 모킹
        when(s3Service.generatePresignedGetUrl("profile/200/face.jpg"))
            .thenReturn("https://test-bucket.s3.amazonaws.com/profile/200/face.jpg");
        when(s3Service.generatePresignedGetUrl(startsWith("attendance/1/20/")))
            .thenReturn("https://test-bucket.s3.amazonaws.com/attendance/1/20/1234567890.jpg");

        // 얼굴 인식 실패 (유사도 낮음)
        FaceSimilarityResponseDto faceApiResponse = new FaceSimilarityResponseDto();
        faceApiResponse.setVerified(false);
        faceApiResponse.setSimilarity(0.45);
        when(faceSimilarityClient.compareFaces(anyString(), anyString())).thenReturn(faceApiResponse);

        // When
        AttendanceVerificationResponseDto response = attendanceService.verifyAndRecordAttendance(request);

        // Then
        assertThat(response.getSuccess()).isTrue();
        assertThat(response.getVerified()).isFalse();
        assertThat(response.getRecordId()).isNull();
        assertThat(response.getSimilarityScore()).isEqualTo(0.45);
        assertThat(response.getMessage()).contains("얼굴 인식에 실패했습니다");

        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("얼굴 미등록 사원 - 예외 발생")
    void verifyAndRecordAttendance_FaceImageNotRegistered() {
        // Given
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
            .phoneNumber("010-1234-5678")
            .uploadId("attendance/1/20/1234567890.jpg")
            .build();

        // 얼굴 이미지 미등록 근로자
        Employee noFaceEmployee = Employee.builder()
            .user(mockEmployeeUser)
            .empName("얼굴 미등록")
            .profileImageUrl(null)
            .build();
        ReflectionTestUtils.setField(noFaceEmployee, "id", 20L);

        when(userRepository.findByUserId("manager123")).thenReturn(Optional.of(mockManagerUser));
        when(siteRepository.findByManagerUserId(100L)).thenReturn(Optional.of(mockSite));
        when(employeeRepository.findByPhoneWithUser("01012345678")).thenReturn(Optional.of(noFaceEmployee));

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAndRecordAttendance(request))
            .isInstanceOf(FaceImageNotRegisteredException.class);

        verify(faceSimilarityClient, never()).compareFaces(anyString(), anyString());
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("중복 퇴근 시도 - 출근 기록에 이미 퇴근 시각 존재")
    void verifyAndRecordAttendance_DuplicateCheckOut_WithExistingCheckOut() {
        // Given
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
            .phoneNumber("010-1234-5678")
            .uploadId("attendance/1/20/1234567890.jpg")
            .build();

        // 이미 출근+퇴근 완료된 기록 존재
        Attendance completedAttendance = Attendance.builder()
            .employeeId(20L)
            .siteId(1L)
            .checkInTime(LocalDateTime.now().minusHours(8))
            .checkOutTime(LocalDateTime.now().minusHours(1))
            .build();

        when(userRepository.findByUserId("manager123")).thenReturn(Optional.of(mockManagerUser));
        when(siteRepository.findByManagerUserId(100L)).thenReturn(Optional.of(mockSite));
        when(employeeRepository.findByPhoneWithUser("01012345678")).thenReturn(Optional.of(mockEmployee));
        when(attendanceRepository.findByEmployeeIdAndSearchDate(eq(20L), any(LocalDate.class)))
            .thenReturn(List.of(completedAttendance)); // 퇴근 기록 있음

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAndRecordAttendance(request))
            .isInstanceOf(DuplicateAttendanceException.class);

        verify(faceSimilarityClient, never()).compareFaces(anyString(), anyString());
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }

    @Test
    @DisplayName("등록되지 않은 전화번호 - 예외 발생")
    void verifyAndRecordAttendance_EmployeeNotFound() {
        // Given
        AttendanceVerificationRequestDto request = AttendanceVerificationRequestDto.builder()
            .phoneNumber("010-9999-9999")
            .uploadId("attendance/1/999/1234567890.jpg")
            .build();

        when(userRepository.findByUserId("manager123")).thenReturn(Optional.of(mockManagerUser));
        when(siteRepository.findByManagerUserId(100L)).thenReturn(Optional.of(mockSite));
        when(employeeRepository.findByPhoneWithUser("01099999999")).thenReturn(Optional.empty());

        // When & Then
        assertThatThrownBy(() -> attendanceService.verifyAndRecordAttendance(request))
            .isInstanceOf(BusinessException.class)
            .hasMessageContaining("등록된 근로자를 찾을 수 없습니다");

        verify(faceSimilarityClient, never()).compareFaces(anyString(), anyString());
        verify(attendanceRepository, never()).save(any(Attendance.class));
    }
}
