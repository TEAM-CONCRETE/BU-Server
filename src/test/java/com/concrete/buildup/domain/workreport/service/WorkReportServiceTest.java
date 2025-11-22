package com.concrete.buildup.domain.workreport.service;

import com.concrete.buildup.domain.auth.entity.Corporation;
import com.concrete.buildup.domain.auth.entity.Manager;
import com.concrete.buildup.domain.auth.entity.User;
import com.concrete.buildup.domain.auth.repository.ManagerRepository;
import com.concrete.buildup.domain.auth.repository.UserRepository;
import com.concrete.buildup.domain.site.entity.Site;
import com.concrete.buildup.domain.site.repository.SiteRepository;
import com.concrete.buildup.domain.upload.service.S3Service;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportRequest;
import com.concrete.buildup.domain.workreport.dto.CreateWorkReportResponse;
import com.concrete.buildup.domain.workreport.dto.MaterialInputDto;
import com.concrete.buildup.domain.workreport.dto.WorkSectionDto;
import com.concrete.buildup.domain.workreport.entity.WorkReport;
import com.concrete.buildup.domain.workreport.repository.WorkReportMaterialRepository;
import com.concrete.buildup.domain.workreport.repository.WorkReportRepository;
import com.concrete.buildup.global.exception.BusinessException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("WorkReportService 단위 테스트")
class WorkReportServiceTest {

    @Mock
    private WorkReportRepository workReportRepository;

    @Mock
    private WorkReportMaterialRepository workReportMaterialRepository;

    @Mock
    private SiteRepository siteRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private ManagerRepository managerRepository;

    @Mock
    private WorkReportPdfService workReportPdfService;

    @Mock
    private S3Service s3Service;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkReportService workReportService;

    private Site testSite;
    private User testUser;
    private Manager testManager;
    private Corporation testCorporation;
    private CreateWorkReportRequest testRequest;

    @BeforeEach
    void setUp() {
        // Corporation 설정
        testCorporation = Corporation.builder()
                .corpName("테스트 기업")
                .build();
        ReflectionTestUtils.setField(testCorporation, "id", 1L);

        // Manager 설정 (Site보다 먼저 생성해야 함)
        testUser = User.builder()
                .userId("manager01")
                .build();
        ReflectionTestUtils.setField(testUser, "id", 1L);

        testManager = Manager.builder()
                .user(testUser)
                .managerName("김관리")
                .build();
        ReflectionTestUtils.setField(testManager, "id", 1L);

        // Site 설정 (manager 할당 포함)
        testSite = Site.builder()
                .siteName("테스트 현장")
                .siteAddress("서울시 강남구")
                .corporation(testCorporation)
                .manager(testManager)  // Manager 권한 검증을 위해 추가
                .build();
        ReflectionTestUtils.setField(testSite, "id", 1L);

        // Request 설정 - 여러 공정 포함
        List<WorkSectionDto> workSections = new ArrayList<>();
        workSections.add(WorkSectionDto.builder()
                .sectionName("철근공사")
                .employeeNum(9)
                .context("1층 바닥 철근 배근 작업 완료")
                .build());
        workSections.add(WorkSectionDto.builder()
                .sectionName("거푸집공사")
                .employeeNum(6)
                .context("2층 기둥 거푸집 설치")
                .build());

        List<MaterialInputDto> materials = new ArrayList<>();
        materials.add(MaterialInputDto.builder()
                .materialName("철근")
                .materialStandard("D13")
                .materialUnit("ton")
                .build());

        testRequest = CreateWorkReportRequest.builder()
                .workSections(workSections)
                .materials(materials)
                .build();

        // ObjectMapper 실제 인스턴스 사용
        objectMapper = new ObjectMapper();
        workReportService = new WorkReportService(
                workReportRepository,
                workReportMaterialRepository,
                siteRepository,
                userRepository,
                managerRepository,
                workReportPdfService,
                Optional.of(s3Service),
                objectMapper
        );
    }

    @Test
    @DisplayName("작업일보 생성 성공 - 여러 공정 정보가 JSON으로 저장됨")
    void createWorkReport_Success_WithMultipleSections() throws Exception {
        // given
        Long siteId = 1L;
        String userId = "manager01";

        when(siteRepository.findById(siteId)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(managerRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testManager));
        when(workReportRepository.findBySiteIdAndIsDeletedFalse(siteId)).thenReturn(new ArrayList<>());

        WorkReport savedWorkReport = WorkReport.builder()
                .site(testSite)
                .manager(testManager)
                .corporation(testCorporation)
                .workSections(objectMapper.writeValueAsString(testRequest.getWorkSections()))
                .build();
        ReflectionTestUtils.setField(savedWorkReport, "id", 1L);

        when(workReportRepository.save(any(WorkReport.class))).thenReturn(savedWorkReport);
        when(workReportPdfService.generateWorkReportPdf(any(), any(), anyString(), any()))
                .thenReturn(new byte[]{1, 2, 3});
        when(s3Service.getPdfUrl(anyString())).thenReturn("https://s3.amazonaws.com/test.pdf");

        // when
        CreateWorkReportResponse response = workReportService.createWorkReport(siteId, testRequest, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWorkReportId()).isEqualTo(1L);
        assertThat(response.getPdfUrl()).contains("s3.amazonaws.com");

        // WorkReport 저장 시 workSections이 JSON 문자열로 변환되었는지 확인
        ArgumentCaptor<WorkReport> workReportCaptor = ArgumentCaptor.forClass(WorkReport.class);
        verify(workReportRepository, times(2)).save(workReportCaptor.capture());

        WorkReport capturedWorkReport = workReportCaptor.getAllValues().get(0);
        assertThat(capturedWorkReport.getWorkSections()).isNotNull();

        // JSON 문자열을 다시 파싱해서 내용 확인
        List<WorkSectionDto> parsedSections = objectMapper.readValue(
                capturedWorkReport.getWorkSections(),
                new TypeReference<List<WorkSectionDto>>() {}
        );
        assertThat(parsedSections).hasSize(2);
        assertThat(parsedSections.get(0).getSectionName()).isEqualTo("철근공사");
        assertThat(parsedSections.get(0).getEmployeeNum()).isEqualTo(9);
        assertThat(parsedSections.get(1).getSectionName()).isEqualTo("거푸집공사");
        assertThat(parsedSections.get(1).getEmployeeNum()).isEqualTo(6);
    }

    @Test
    @DisplayName("작업일보 생성 실패 - 현장이 존재하지 않음")
    void createWorkReport_Fail_SiteNotFound() {
        // given
        Long siteId = 999L;
        String userId = "manager01";

        when(siteRepository.findById(siteId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workReportService.createWorkReport(siteId, testRequest, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("현장을 찾을 수 없습니다");
    }

    @Test
    @DisplayName("작업일보 생성 실패 - 사용자가 존재하지 않음")
    void createWorkReport_Fail_UserNotFound() {
        // given
        Long siteId = 1L;
        String userId = "invalid_user";

        when(siteRepository.findById(siteId)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> workReportService.createWorkReport(siteId, testRequest, userId))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("사용자를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("작업일보 생성 성공 - 순번 증가 확인 (같은 날 여러 개)")
    void createWorkReport_Success_SequenceIncrement() throws Exception {
        // given
        Long siteId = 1L;
        String userId = "manager01";

        // 이미 오늘 작성된 작업일보가 2개 있다고 가정
        List<WorkReport> existingReports = new ArrayList<>();
        WorkReport report1 = WorkReport.builder()
                .site(testSite)
                .build();
        ReflectionTestUtils.setField(report1, "id", 1L);
        ReflectionTestUtils.setField(report1, "createdAt", LocalDateTime.now());

        WorkReport report2 = WorkReport.builder()
                .site(testSite)
                .build();
        ReflectionTestUtils.setField(report2, "id", 2L);
        ReflectionTestUtils.setField(report2, "createdAt", LocalDateTime.now());

        existingReports.add(report1);
        existingReports.add(report2);

        when(siteRepository.findById(siteId)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(managerRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testManager));
        when(workReportRepository.findBySiteIdAndIsDeletedFalse(siteId)).thenReturn(existingReports);

        WorkReport savedWorkReport = WorkReport.builder()
                .site(testSite)
                .manager(testManager)
                .corporation(testCorporation)
                .workSections(objectMapper.writeValueAsString(testRequest.getWorkSections()))
                .build();
        ReflectionTestUtils.setField(savedWorkReport, "id", 3L);

        when(workReportRepository.save(any(WorkReport.class))).thenReturn(savedWorkReport);
        when(workReportPdfService.generateWorkReportPdf(any(), any(), anyString(), any()))
                .thenReturn(new byte[]{1, 2, 3});
        when(s3Service.getPdfUrl(anyString())).thenReturn("https://s3.amazonaws.com/test.pdf");

        // when
        CreateWorkReportResponse response = workReportService.createWorkReport(siteId, testRequest, userId);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getWorkReportId()).isEqualTo(3L);

        // S3 업로드가 순번 3으로 호출되었는지 확인
        ArgumentCaptor<String> s3KeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(s3Service).uploadPdf(s3KeyCaptor.capture(), any(byte[].class));

        String s3Key = s3KeyCaptor.getValue();
        assertThat(s3Key).contains("-3.pdf"); // 순번이 3이어야 함
    }

    @Test
    @DisplayName("자재 정보 저장 확인")
    void createWorkReport_Success_WithMaterials() throws Exception {
        // given
        Long siteId = 1L;
        String userId = "manager01";

        when(siteRepository.findById(siteId)).thenReturn(Optional.of(testSite));
        when(userRepository.findByUserId(userId)).thenReturn(Optional.of(testUser));
        when(managerRepository.findByUserId(testUser.getId())).thenReturn(Optional.of(testManager));
        when(workReportRepository.findBySiteIdAndIsDeletedFalse(siteId)).thenReturn(new ArrayList<>());

        WorkReport savedWorkReport = WorkReport.builder()
                .site(testSite)
                .manager(testManager)
                .corporation(testCorporation)
                .workSections(objectMapper.writeValueAsString(testRequest.getWorkSections()))
                .materials(new ArrayList<>())
                .build();
        ReflectionTestUtils.setField(savedWorkReport, "id", 1L);

        when(workReportRepository.save(any(WorkReport.class))).thenReturn(savedWorkReport);
        when(workReportPdfService.generateWorkReportPdf(any(), any(), anyString(), any()))
                .thenReturn(new byte[]{1, 2, 3});
        when(s3Service.getPdfUrl(anyString())).thenReturn("https://s3.amazonaws.com/test.pdf");

        // when
        CreateWorkReportResponse response = workReportService.createWorkReport(siteId, testRequest, userId);

        // then
        assertThat(response).isNotNull();

        // 자재가 추가되었는지 확인
        ArgumentCaptor<WorkReport> workReportCaptor = ArgumentCaptor.forClass(WorkReport.class);
        verify(workReportRepository, times(2)).save(workReportCaptor.capture());

        // 첫 번째 저장 시점에 자재가 추가되어 있어야 함
        WorkReport capturedWorkReport = workReportCaptor.getAllValues().get(0);
        assertThat(capturedWorkReport.getMaterials()).isNotNull();
    }
}
