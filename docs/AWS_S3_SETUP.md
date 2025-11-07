# AWS S3 설정 가이드

Build-Up Platform에서 파일 업로드 기능(Presigned URL)을 사용하기 위한 AWS S3 설정 가이드입니다.

## 목차
1. [AWS S3 버킷 생성](#1-aws-s3-버킷-생성)
2. [IAM 사용자 생성 및 권한 설정](#2-iam-사용자-생성-및-권한-설정)
3. [환경 변수 설정](#3-환경-변수-설정)
4. [S3 통합 테스트 실행](#4-s3-통합-테스트-실행)
5. [문제 해결](#5-문제-해결)

---

## 1. AWS S3 버킷 생성

### 1.1 AWS Console 로그인
https://console.aws.amazon.com/ 에 접속하여 로그인합니다.

### 1.2 S3 버킷 생성
1. AWS Console에서 **S3** 서비스 검색
2. **버킷 만들기** 클릭
3. 버킷 설정:
   - **버킷 이름**: `build-up-contracts` (또는 원하는 이름)
   - **리전**: `아시아 태평양(서울) ap-northeast-2` 선택
   - **퍼블릭 액세스 차단**: 모두 체크 (기본값)
   - **버킷 버전 관리**: 비활성화 (선택사항)
   - **암호화**: 서버 측 암호화 활성화 권장
4. **버킷 만들기** 클릭

### 1.3 CORS 설정 (필수)
클라이언트에서 Presigned URL로 직접 업로드하려면 CORS 설정이 필요합니다.

1. 생성한 버킷 선택
2. **권한** 탭 > **CORS(Cross-Origin Resource Sharing)** 섹션
3. **편집** 클릭 후 아래 내용 입력:

```json
[
    {
        "AllowedHeaders": [
            "*"
        ],
        "AllowedMethods": [
            "GET",
            "PUT",
            "POST",
            "DELETE",
            "HEAD"
        ],
        "AllowedOrigins": [
            "http://localhost:3000",
            "http://localhost:5173",
            "https://yourdomain.com"
        ],
        "ExposeHeaders": [
            "ETag"
        ],
        "MaxAgeSeconds": 3600
    }
]
```

4. **변경 사항 저장**

---

## 2. IAM 사용자 생성 및 권한 설정

### 2.1 IAM 사용자 생성
1. AWS Console에서 **IAM** 서비스 검색
2. **사용자** > **사용자 생성** 클릭
3. 사용자 이름: `buildup-s3-user` (또는 원하는 이름)
4. **다음** 클릭

### 2.2 권한 설정
**옵션 1: 기존 정책 직접 연결 (간단)**
1. **권한 옵션**: "직접 정책 연결"
2. 정책 검색: `AmazonS3FullAccess` 선택
   - ⚠️ **주의**: 프로덕션 환경에서는 최소 권한 원칙 적용 권장

**옵션 2: 커스텀 정책 생성 (권장)**
1. **권한 옵션**: "직접 정책 연결"
2. **정책 생성** 클릭
3. JSON 탭에서 아래 내용 입력:

```json
{
    "Version": "2012-10-17",
    "Statement": [
        {
            "Sid": "S3BucketAccess",
            "Effect": "Allow",
            "Action": [
                "s3:PutObject",
                "s3:GetObject",
                "s3:DeleteObject",
                "s3:ListBucket"
            ],
            "Resource": [
                "arn:aws:s3:::build-up-contracts",
                "arn:aws:s3:::build-up-contracts/*"
            ]
        }
    ]
}
```

4. **다음** > 정책 이름: `BuildUpS3Policy` > **정책 생성**
5. 사용자 생성 화면으로 돌아가서 방금 만든 정책 선택

### 2.3 액세스 키 생성
1. 생성한 사용자 클릭
2. **보안 자격 증명** 탭
3. **액세스 키 만들기** 클릭
4. 사용 사례: **로컬 코드**
5. **액세스 키 만들기** 클릭
6. ⚠️ **중요**: **액세스 키 ID**와 **비밀 액세스 키**를 안전하게 복사
   - 비밀 액세스 키는 이 화면을 벗어나면 다시 볼 수 없습니다!

---

## 3. 환경 변수 설정

### 3.1 .env 파일 생성
프로젝트 루트 디렉토리에서:

```bash
# .env.example을 .env로 복사
cp .env.example .env
```

### 3.2 AWS 자격 증명 입력
`.env` 파일을 열어서 다음 값들을 입력:

```bash
# AWS S3 설정
AWS_S3_ENABLED=true

# IAM 사용자의 액세스 키 (2.3에서 복사한 값)
AWS_ACCESS_KEY_ID=AKIA...your_key_here
AWS_SECRET_ACCESS_KEY=your_secret_key_here

# S3 버킷 이름 (1.2에서 생성한 버킷)
AWS_S3_BUCKET=build-up-contracts

# AWS 리전
AWS_REGION=ap-northeast-2
```

### 3.3 환경 변수 로드

**방법 1: start-dev.sh 수정 (권장)**
`start-dev.sh` 파일에 환경 변수 로드 추가 (이미 되어 있음)

**방법 2: 직접 export**
```bash
export AWS_S3_ENABLED=true
export AWS_ACCESS_KEY_ID=AKIA...
export AWS_SECRET_ACCESS_KEY=...
export AWS_S3_BUCKET=build-up-contracts
export AWS_REGION=ap-northeast-2

./gradlew bootRun
```

**방법 3: IntelliJ 환경 변수 설정**
1. Run > Edit Configurations
2. Environment variables 섹션에 추가

---

## 4. S3 통합 테스트 실행

### 4.1 테스트 환경 변수 설정
```bash
# .env 파일에 추가
AWS_INTEGRATION_TEST=true
AWS_S3_ENABLED=true
AWS_S3_BUCKET=build-up-contracts-test  # 테스트용 버킷 사용 권장
```

### 4.2 테스트 실행
```bash
# 환경 변수와 함께 테스트 실행
AWS_INTEGRATION_TEST=true \
AWS_S3_ENABLED=true \
AWS_ACCESS_KEY_ID=AKIA... \
AWS_SECRET_ACCESS_KEY=... \
AWS_S3_BUCKET=build-up-contracts-test \
./gradlew test --tests S3ServiceIntegrationTest
```

### 4.3 테스트 결과 확인
성공 시:
```
S3Service 실제 AWS 통합 테스트 > 실제 Presigned URL 발급 및 업로드 테스트 PASSED
S3Service 실제 AWS 통합 테스트 > 실제 이미지 다운로드 테스트 PASSED
S3Service 실제 AWS 통합 테스트 > PDF 파일 Presigned URL 발급 테스트 PASSED
```

---

## 5. 문제 해결

### 5.1 Access Denied 에러
```
AccessDenied: Access Denied
```

**원인**: IAM 권한 부족
**해결**:
1. IAM 정책에 필요한 권한이 있는지 확인
2. 버킷 정책 확인
3. 버킷 이름이 올바른지 확인

### 5.2 No credentials 에러
```
Unable to load credentials from any of the providers in the chain
```

**원인**: AWS 자격 증명이 설정되지 않음
**해결**:
1. `.env` 파일에 `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`가 올바르게 설정되었는지 확인
2. 환경 변수가 제대로 로드되었는지 확인: `echo $AWS_ACCESS_KEY_ID`

### 5.3 S3Client Bean을 찾을 수 없는 에러
```
No qualifying bean of type 'S3Client'
```

**원인**: S3 설정이 비활성화됨
**해결**:
```bash
# .env 파일에서 확인
AWS_S3_ENABLED=true  # false가 아닌지 확인
```

### 5.4 CORS 에러 (브라우저)
```
Access to XMLHttpRequest has been blocked by CORS policy
```

**원인**: S3 버킷의 CORS 설정 미흡
**해결**: [1.3 CORS 설정](#13-cors-설정-필수) 참고

### 5.5 비용 관련
- **Presigned URL 발급**: 무료 (API 호출만)
- **파일 업로드/다운로드**: PUT/GET 요청당 $0.000005 (매우 저렴)
- **저장 용량**: 첫 50TB까지 GB당 $0.025/월
- **테스트 실행 시**: 1KB 파일 몇 개 업로드하므로 거의 무료

---

## 참고 자료

- [AWS S3 공식 문서](https://docs.aws.amazon.com/s3/)
- [AWS SDK for Java 2.x](https://docs.aws.amazon.com/sdk-for-java/latest/developer-guide/home.html)
- [Spring Cloud AWS](https://docs.awspring.io/spring-cloud-aws/docs/3.0.0/reference/html/index.html)
